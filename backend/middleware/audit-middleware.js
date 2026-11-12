/**
 * LeaveDesk Asynchronous Automated Audit Logging Middleware
 * Intercepts every mutating HTTP action (POST, PUT, PATCH, DELETE)
 * and records structured business events into MySQL 'audit_logs' and 'activity_logs'.
 */

const dbHelper = require('../db-helper');
const mysqlDb = require('../mysql-db');

const ACTION_MAP = {
  '/api/v1/auth/login': 'USER_LOGIN',
  '/api/v1/leaves/apply': 'APPLY_LEAVE',
  '/api/v1/leaves/decision': 'DECISION_LEAVE',
  '/api/v1/leaves/emergency-cancel': 'EMERGENCY_CANCEL',
  '/api/v1/leaves/query': 'QUERY_LEAVE',
  '/api/v1/calendar/date-lock': 'DATE_LOCK_TOGGLE',
  '/api/v1/chat/messages': 'CHAT_MESSAGE',
  '/api/v1/users/profile': 'PROFILE_UPDATE'
};

function deduceAction(req) {
  const path = req.originalUrl.split('?')[0];
  if (ACTION_MAP[path]) return ACTION_MAP[path];
  if (path.includes('/decision')) return 'DECISION_LEAVE';
  if (path.includes('/query')) return 'QUERY_LEAVE';
  if (path.includes('/cancel')) return 'CANCEL_LEAVE';
  if (path.includes('/profile')) return 'PROFILE_UPDATE';
  return `${req.method}_${req.path.replace(/\//g, '_').substring(1).toUpperCase()}`;
}

function deduceActor(req) {
  const body = req.body || {};
  return {
    name: body.actorName || body.userName || body.senderName || body.approverName || (body.identifier ? `User (${body.identifier})` : 'App User'),
    role: body.actorRole || body.userRole || body.senderRole || body.approverRole || body.role || 'EMPLOYEE',
    userId: body.actorUserId || body.userId || body.senderId || body.approverUserId || null
  };
}

function deduceTarget(req, action) {
  const body = req.body || {};
  const params = req.params || {};
  if (params.id || params.leaveId) return `Request ${params.leaveId || params.id}`;
  if (body.id || body.requestId) return `Request ${body.id || body.requestId}`;
  if (body.identifier) return `Auth ${body.identifier}`;
  if (body.date) return `Date ${body.date}`;
  if (body.target) return body.target;
  return req.originalUrl;
}

module.exports = function auditMiddleware(req, res, next) {
  const mutatingMethods = ['POST', 'PUT', 'PATCH', 'DELETE'];
  if (!mutatingMethods.includes(req.method)) {
    return next();
  }

  const startTime = Date.now();
  const requestId = "req_" + Math.random().toString(36).substring(2, 10);

  // Hook into response completion
  res.on('finish', () => {
    // Only log once response is sent
    const durationMs = Date.now() - startTime;
    const isSuccess = res.statusCode < 400;
    const action = deduceAction(req);
    const actor = deduceActor(req);
    const target = deduceTarget(req, action);

    // 1. Business Audit Log (High-level compliance event)
    const reason = req.body?.reason || req.body?.comment || (isSuccess ? 'HTTP 200/201 Success' : `Failed (${res.statusCode})`);
    const details = {
      method: req.method,
      endpoint: req.originalUrl,
      statusCode: res.statusCode,
      durationMs,
      ip: req.ip || req.connection?.remoteAddress,
      workingDays: req.body?.workingDays,
      leaveType: req.body?.leaveType
    };

    // Execute asynchronously (non-blocking)
    setImmediate(async () => {
      try {
        if (mysqlDb.isAvailable()) {
          // Write to audit_logs
          await dbHelper.writeAudit({
            actorName: actor.name,
            actorRole: actor.role,
            action,
            target,
            reason,
            details: JSON.stringify(details)
          });

          // Write to activity_logs
          await mysqlDb.addActivityLog({
            id: "act_" + Math.random().toString(36).substring(2, 12),
            requestId,
            service: 'unified-gateway',
            actorUserId: actor.userId,
            actorRole: actor.role,
            action,
            resourceType: req.path.split('/')[3] || 'resource',
            outcome: isSuccess ? 'SUCCESS' : 'FAILURE',
            httpMethod: req.method,
            route: req.originalUrl,
            statusCode: res.statusCode,
            durationMs,
            details
          });
        }
      } catch (err) {
        console.error(`⚠️ [Audit Middleware Error]:`, err.message);
      }
    });
  });

  next();
};
