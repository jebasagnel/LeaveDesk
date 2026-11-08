// Service 2: Leave Workflow & Business Engine (Port 8082)
const express = require('express');
const cors = require('cors');
const db = require('../../db');

const app = express();
app.use(cors());
app.use(express.json());

// Helper: Calculate working days (skipping weekends & holidays)
async function calculateWorkingDays(startDateStr, endDateStr, isHalfDay) {
  if (isHalfDay) return 0.5;
  const start = new Date(startDateStr);
  const end = new Date(endDateStr);
  if (start > end) return 0;

  let count = 0;
  let curr = new Date(start);
  const holidays = await db.getHolidays();
  const holidayDates = new Set(holidays.map(h => h.date));

  while (curr <= end) {
    const day = curr.getDay(); // 0 is Sunday, 6 is Saturday
    const dateIso = curr.toISOString().split('T')[0];
    if (day !== 0 && day !== 6 && !holidayDates.has(dateIso)) {
      count++;
    }
    curr.setDate(curr.getDate() + 1);
  }
  return count;
}

// GET /api/v1/leaves/balances/:userId
app.get('/api/v1/leaves/balances/:userId', async (req, res) => {
  try {
    const quotas = await db.getQuotas(req.params.userId);
    res.json(quotas);
  } catch (err) {
    res.status(500).json({ error: "Failed to fetch quotas" });
  }
});

// GET /api/v1/leaves/requests
app.get('/api/v1/leaves/requests', async (req, res) => {
  try {
    const { userId, role, groupId } = req.query;
    const list = await db.getRequests({ userId, role, groupId });
    res.json(list);
  } catch (err) {
    res.status(500).json({ error: "Failed to fetch requests" });
  }
});

// POST /api/v1/leaves/apply
app.post('/api/v1/leaves/apply', async (req, res) => {
  try {
    const {
      userId,
      leaveType,
      startDate,
      endDate,
      isHalfDay,
      reason,
      backupUserId,
      backupNote,
      proofFileName
    } = req.body;

    const user = await db.getUser(userId);
    if (!user) return res.status(404).json({ error: "User not found" });

    const workingDays = await calculateWorkingDays(startDate, endDate, isHalfDay);
    if (workingDays <= 0) {
      return res.status(400).json({ error: "Selected dates fall on weekends or holidays." });
    }

    // Check quota
    const quotas = await db.getQuotas(userId);
    const quota = quotas.find(q => q.leaveType === leaveType);
    if (quota && !quota.isNoCap && (quota.total - quota.used) < workingDays) {
      return res.status(400).json({
        error: `Insufficient ${leaveType} balance. Remaining: ${quota.total - quota.used}, required: ${workingDays}. Consider LOP.`
      });
    }

    // Check team presence threshold
    const group = await db.getGroup(user.groupId);
    if (group && leaveType !== "SL") {
      const allRequests = await db.getRequests({ groupId: user.groupId });
      const activeRequests = allRequests.filter(
        r => r.status === "APPROVED" && !(r.endDate < startDate || r.startDate > endDate)
      );
      const presentCount = (group.memberIds ? group.memberIds.length : 4) - (activeRequests.length + 1);
      if (presentCount < group.minPresentThreshold) {
        return res.status(409).json({
          error: `Group minimum presence violated. Requires ${group.minPresentThreshold} members active; would leave ${presentCount}. Please send an emergency request.`,
          isThresholdViolation: true
        });
      }
    }

    const backupUser = backupUserId ? await db.getUser(backupUserId) : null;
    const newRequest = {
      id: "LR-" + Math.floor(1000 + Math.random() * 9000),
      userId,
      userName: user.name,
      userRole: user.role,
      groupId: user.groupId,
      groupName: user.groupName,
      leaveType,
      startDate,
      endDate,
      workingDays,
      isHalfDay: !!isHalfDay,
      reason,
      backupUserId: backupUserId || null,
      backupUserName: backupUser ? backupUser.name : null,
      backupNote: backupNote || null,
      proofFileName: proofFileName || null,
      status: "PENDING",
      createdAt: new Date().toISOString()
    };

    await db.createRequest(newRequest);
    await db.addAudit(user.name, user.role, "APPLY_LEAVE", newRequest.id, reason, `${workingDays} days ${leaveType}`);

    res.status(201).json(newRequest);
  } catch (err) {
    console.error("Apply leave error:", err);
    res.status(500).json({ error: "Failed to submit leave request" });
  }
});

// POST /api/v1/leaves/:id/decision (Approve / Reject)
app.post('/api/v1/leaves/:id/decision', async (req, res) => {
  try {
    const { id } = req.params;
    const { approverUserId, decision, comment } = req.body;

    const approver = await db.getUser(approverUserId);
    const requests = await db.getRequests();
    const request = requests.find(r => r.id === id);
    if (!request) return res.status(404).json({ error: "Request not found" });

    if (decision === "REJECTED") {
      const updatePayload = {
        status: "REJECTED",
        ...(approver.role === "CEO"
          ? { ceoDecision: "REJECTED", ceoComment: comment }
          : { tlDecision: "REJECTED", tlComment: comment })
      };
      await db.updateDecision(id, updatePayload);
      await db.addAudit(approver.name, approver.role, "REJECT_LEAVE", request.id, comment || "Rejected", "");

      // Post decision to direct chat thread
      const threadId = approver.role === "CEO" ? `thread_emp_${request.userId}_ceo` : `thread_emp_${request.userId}_tl`;
      try {
        await db.addChatMessage({
          id: `m_${Date.now()}`,
          threadId,
          senderId: approver.id,
          senderName: approver.name,
          senderRole: approver.role,
          text: `OFFICIAL DECISION: Request ${request.id} has been REJECTED.\nReason: ${comment || 'Rejected by ' + approver.role}`,
          isOfficialDecision: true,
          relatedRequestId: request.id,
          timestamp: new Date().toISOString()
        });
      } catch (e) { /* ignore chat sync if offline */ }

      return res.json({ ...request, ...updatePayload });
    }

    if (decision === "QUERY") {
      const updatePayload = {
        status: "QUERY",
        queryQuestion: comment,
        ...(approver.role === "CEO"
          ? { ceoDecision: "QUERY", ceoComment: comment }
          : { tlDecision: "QUERY", tlComment: comment })
      };
      await db.updateDecision(id, updatePayload);
      await db.addAudit(approver.name, approver.role, "QUERY_LEAVE", request.id, comment || "Query raised", "");

      // Post official query to direct chat thread
      const threadId = approver.role === "CEO" ? `thread_emp_${request.userId}_ceo` : `thread_emp_${request.userId}_tl`;
      try {
        await db.addChatMessage({
          id: `m_${Date.now()}`,
          threadId,
          senderId: approver.id,
          senderName: approver.name,
          senderRole: approver.role,
          text: `OFFICIAL APPROVAL QUERY on ${request.id}:\n${comment}`,
          isOfficialQuery: true,
          relatedRequestId: request.id,
          timestamp: new Date().toISOString()
        });
      } catch (e) { /* ignore chat sync if offline */ }

      return res.json({ ...request, ...updatePayload });
    }

    // Approval Flow Rules:
    // 1. TL's personal leave -> directly requires CEO
    // 2. Short leave (1-2 days) -> TL only
    // 3. Long leave (3+ days, ML) -> TL then CEO
    let finalStatus = request.status;
    const updatePayload = {};

    if (approver.role === "TEAM_LEAD") {
      updatePayload.tlDecision = "APPROVED";
      updatePayload.tlComment = comment;

      if (request.workingDays <= 2 && request.leaveType !== "ML") {
        finalStatus = "APPROVED";
        updatePayload.status = "APPROVED";
        await db.deductQuota(request.userId, request.leaveType, request.workingDays);
      } else {
        finalStatus = "PENDING"; // awaiting CEO
        updatePayload.status = "PENDING";
      }
    } else if (approver.role === "CEO") {
      updatePayload.ceoDecision = "APPROVED";
      updatePayload.ceoComment = comment;
      finalStatus = "APPROVED";
      updatePayload.status = "APPROVED";
      await db.deductQuota(request.userId, request.leaveType, request.workingDays);
    }

    await db.updateDecision(id, updatePayload);
    await db.addAudit(approver.name, approver.role, "APPROVE_LEAVE", request.id, comment || "Approved", `Final Status: ${finalStatus}`);
    res.json({ ...request, ...updatePayload });
  } catch (err) {
    console.error("Decision error:", err);
    res.status(500).json({ error: "Failed to process leave decision" });
  }
});

// POST /api/v1/leaves/:id/emergency-cancel
app.post('/api/v1/leaves/:id/emergency-cancel', async (req, res) => {
  try {
    const { id } = req.params;
    const { actorUserId, reason } = req.body;

    const actor = await db.getUser(actorUserId);
    const requests = await db.getRequests();
    const request = requests.find(r => r.id === id);
    if (!request) return res.status(404).json({ error: "Request not found" });

    if (request.status === "APPROVED") {
      await db.restoreQuota(request.userId, request.leaveType, request.workingDays);
    }

    const updatePayload = {
      status: "CANCELLED",
      cancellationReason: reason
    };
    await db.updateDecision(id, updatePayload);

    await db.addAudit(actor.name, actor.role, "EMERGENCY_CANCEL", request.id, reason, `Restored ${request.workingDays} days`);
    res.json({ ...request, ...updatePayload });
  } catch (err) {
    res.status(500).json({ error: "Failed to emergency cancel leave" });
  }
});

// GET /api/v1/audit/logs
app.get('/api/v1/audit/logs', async (req, res) => {
  try {
    const logs = await db.getAuditLogs();
    res.json(logs);
  } catch (err) {
    res.status(500).json({ error: "Failed to fetch audit logs" });
  }
});

const PORT = process.env.PORT || 8082;
if (require.main === module) {
  app.listen(PORT, () => {
    console.log(`[Service 2: Workflow] Running on http://localhost:${PORT}`);
  });
}

module.exports = app;
