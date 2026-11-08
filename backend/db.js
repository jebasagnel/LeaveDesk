// Unified Database Layer with Auto-Detecting MySQL & In-Memory Fallback
// When MySQL is running, all reads & writes execute against real MySQL tables.
// When MySQL is offline or initializing, safely falls back to in-memory state.

const mysqlDb = require('./mysql-db');

const initialUsers = [
  {
    id: "u_ceo",
    employeeId: "CEO001",
    name: "Robert Sterling",
    email: "robert.sterling@leavedesk.corp",
    password: "password123",
    role: "CEO",
    groupId: "g_exec",
    groupName: "Executive Office",
    designation: "Chief Executive Officer",
    avatarInitials: "RS"
  },
  {
    id: "u_tl1",
    employeeId: "TL201",
    name: "Marcus Vance",
    email: "marcus.v@leavedesk.corp",
    password: "password123",
    role: "TEAM_LEAD",
    groupId: "g_eng",
    groupName: "Engineering & Product",
    designation: "Engineering Lead",
    avatarInitials: "MV"
  },
  {
    id: "u_tl2",
    employeeId: "TL202",
    name: "Elena Rostova",
    email: "elena.r@leavedesk.corp",
    password: "password123",
    role: "TEAM_LEAD",
    groupId: "g_ops",
    groupName: "Design & Operations",
    designation: "Operations Lead",
    avatarInitials: "ER"
  },
  {
    id: "u_emp1",
    employeeId: "EMP101",
    name: "Alex Chen",
    email: "alex.chen@leavedesk.corp",
    password: "password123",
    role: "EMPLOYEE",
    groupId: "g_eng",
    groupName: "Engineering & Product",
    designation: "Senior Android Engineer",
    avatarInitials: "AC"
  },
  {
    id: "u_emp2",
    employeeId: "EMP102",
    name: "Sarah Jenkins",
    email: "sarah.j@leavedesk.corp",
    password: "password123",
    role: "EMPLOYEE",
    groupId: "g_eng",
    groupName: "Engineering & Product",
    designation: "Backend Architect",
    avatarInitials: "SJ"
  },
  {
    id: "u_emp3",
    employeeId: "EMP103",
    name: "David Kim",
    email: "david.kim@leavedesk.corp",
    password: "password123",
    role: "EMPLOYEE",
    groupId: "g_eng",
    groupName: "Engineering & Product",
    designation: "QA Specialist",
    avatarInitials: "DK"
  },
  {
    id: "u_emp4",
    employeeId: "EMP104",
    name: "Priya Patel",
    email: "priya.p@leavedesk.corp",
    password: "password123",
    role: "EMPLOYEE",
    groupId: "g_eng",
    groupName: "Engineering & Product",
    designation: "Fullstack Developer",
    avatarInitials: "PP"
  }
];

const initialGroups = [
  {
    id: "g_eng",
    name: "Engineering & Product",
    tlUserId: "u_tl1",
    tlName: "Marcus Vance",
    minPresentThreshold: 3,
    memberIds: ["u_tl1", "u_emp1", "u_emp2", "u_emp3", "u_emp4"]
  },
  {
    id: "g_ops",
    name: "Design & Operations",
    tlUserId: "u_tl2",
    tlName: "Elena Rostova",
    minPresentThreshold: 2,
    memberIds: ["u_tl2"]
  }
];

const initialQuotas = {
  u_emp1: [
    { leaveType: "CL", total: 12, used: 2 },
    { leaveType: "SL", total: 10, used: 1 },
    { leaveType: "EL", total: 15, used: 4 },
    { leaveType: "ML", total: 90, used: 0 },
    { leaveType: "BL", total: 5, used: 0 },
    { leaveType: "LOP", total: 0, used: 0, isNoCap: true },
    { leaveType: "HD", total: 8, used: 1 },
    { leaveType: "WFH", total: 0, used: 3, isNoCap: true },
    { leaveType: "SHORT", total: 12, used: 2 }
  ],
  u_tl1: [
    { leaveType: "CL", total: 12, used: 3 },
    { leaveType: "SL", total: 10, used: 0 },
    { leaveType: "EL", total: 15, used: 5 },
    { leaveType: "ML", total: 90, used: 0 },
    { leaveType: "BL", total: 5, used: 0 },
    { leaveType: "LOP", total: 0, used: 0, isNoCap: true },
    { leaveType: "HD", total: 8, used: 0 },
    { leaveType: "WFH", total: 0, used: 2, isNoCap: true },
    { leaveType: "SHORT", total: 12, used: 1 }
  ],
  u_ceo: [
    { leaveType: "CL", total: 15, used: 0 },
    { leaveType: "SL", total: 15, used: 0 },
    { leaveType: "EL", total: 20, used: 2 },
    { leaveType: "ML", total: 90, used: 0 },
    { leaveType: "BL", total: 5, used: 0 },
    { leaveType: "LOP", total: 0, used: 0, isNoCap: true },
    { leaveType: "HD", total: 10, used: 0 },
    { leaveType: "WFH", total: 0, used: 0, isNoCap: true },
    { leaveType: "SHORT", total: 20, used: 0 }
  ]
};

const initialHolidays = [
  { id: 1, date: "2026-11-26", title: "Thanksgiving Day", isLockedByCeo: true, lockReason: "Company-Wide Thanksgiving Holiday" },
  { id: 2, date: "2026-11-27", title: "Day After Thanksgiving", isLockedByCeo: true, lockReason: "Extended Holiday" },
  { id: 3, date: "2026-12-25", title: "Christmas Day", isLockedByCeo: true, lockReason: "Christmas Holiday" },
  { id: 4, date: "2027-01-01", title: "New Year's Day", isLockedByCeo: true, lockReason: "New Year Celebration" }
];

const initialLeaveRequests = [
  {
    id: "LR-7892",
    userId: "u_emp1",
    userName: "Alex Chen",
    userRole: "EMPLOYEE",
    groupId: "g_eng",
    groupName: "Engineering & Product",
    leaveType: "CL",
    startDate: "2026-11-16",
    endDate: "2026-11-17",
    workingDays: 2,
    isHalfDay: false,
    reason: "Personal family commitment and travel out of town.",
    backupUserId: "u_emp2",
    backupUserName: "Sarah Jenkins",
    status: "PENDING",
    createdAt: new Date().toISOString()
  }
];

const initialAuditLogs = [
  {
    id: "AUD-1001",
    timestamp: new Date().toISOString(),
    actorName: "Robert Sterling",
    actorRole: "CEO",
    action: "SYSTEM_INIT",
    target: "System Boot",
    reason: "Production Microservices cluster initialized",
    details: "All 3 services online and connected to persistent store."
  }
];

class UnifiedDatabase {
  constructor() {
    this.mysql = mysqlDb;
    this.users = [...initialUsers];
    this.groups = [...initialGroups];
    this.quotas = JSON.parse(JSON.stringify(initialQuotas));
    this.holidays = [...initialHolidays];
    this.requests = [...initialLeaveRequests];
    this.auditLogs = [...initialAuditLogs];
    this.chatMessages = [];
  }

  isMySQLActive() {
    return this.mysql.isAvailable();
  }

  async findUserByIdentifier(identifier) {
    if (this.isMySQLActive()) {
      const u = await this.mysql.findUserByIdentifier(identifier);
      if (u) return u;
    }
    const idClean = identifier.trim().toLowerCase();
    return this.users.find(
      u => u.employeeId.toLowerCase() === idClean || u.email.toLowerCase() === idClean
    ) || null;
  }

  async getUser(id) {
    if (this.isMySQLActive()) {
      const u = await this.mysql.getUser(id);
      if (u) return u;
    }
    return this.users.find(u => u.id === id) || null;
  }

  async getAllUsers() {
    if (this.isMySQLActive()) {
      const list = await this.mysql.getAllUsers();
      if (list && list.length > 0) return list;
    }
    return this.users.map(({ password, ...u }) => u);
  }

  async getTeamMembers(groupId) {
    if (this.isMySQLActive()) {
      const list = await this.mysql.getTeamMembers(groupId);
      if (list && list.length > 0) return list;
    }
    return this.users.filter(u => u.groupId === groupId).map(({ password, ...u }) => u);
  }

  async getGroup(id) {
    if (this.isMySQLActive()) {
      const g = await this.mysql.getGroup(id);
      if (g) return g;
    }
    return this.groups.find(g => g.id === id) || null;
  }

  async getQuotas(userId) {
    if (this.isMySQLActive()) {
      const q = await this.mysql.getQuotas(userId);
      if (q && q.length > 0) return q;
    }
    return this.quotas[userId] || this.quotas["u_emp1"] || [];
  }

  async deductQuota(userId, leaveType, days) {
    if (this.isMySQLActive()) {
      await this.mysql.deductQuota(userId, leaveType, days);
    }
    // Also keep in-memory sync
    const list = this.quotas[userId] || this.quotas["u_emp1"];
    if (list) {
      const item = list.find(q => q.leaveType === leaveType);
      if (item && !item.isNoCap) item.used += days;
    }
  }

  async restoreQuota(userId, leaveType, days) {
    if (this.isMySQLActive()) {
      await this.mysql.restoreQuota(userId, leaveType, days);
    }
    const list = this.quotas[userId] || this.quotas["u_emp1"];
    if (list) {
      const item = list.find(q => q.leaveType === leaveType);
      if (item && !item.isNoCap) item.used = Math.max(0, item.used - days);
    }
  }

  async getRequests(filter = {}) {
    if (this.isMySQLActive()) {
      const list = await this.mysql.getLeaveRequests(filter);
      if (list) return list;
    }
    let list = this.requests;
    if (filter.userId) {
      list = list.filter(r => r.userId === filter.userId);
    } else if (filter.role === "TEAM_LEAD" && filter.groupId) {
      list = list.filter(r => r.groupId === filter.groupId);
    }
    return list;
  }

  async createRequest(reqData) {
    if (this.isMySQLActive()) {
      await this.mysql.createLeaveRequest(reqData);
    }
    this.requests.unshift(reqData);
    return reqData;
  }

  async updateDecision(id, updateData) {
    if (this.isMySQLActive()) {
      await this.mysql.updateLeaveDecision(id, updateData);
    }
    const r = this.requests.find(item => item.id === id);
    if (r) {
      Object.assign(r, updateData);
    }
    return r;
  }

  async getHolidays() {
    if (this.isMySQLActive()) {
      const h = await this.mysql.getHolidays();
      if (h && h.length > 0) return h;
    }
    return this.holidays;
  }

  async setHolidayLock(date, reason, isLocked) {
    if (this.isMySQLActive()) {
      await this.mysql.setHolidayLock(date, reason, isLocked);
    }
    let holiday = this.holidays.find(h => h.date === date);
    if (holiday) {
      holiday.isLockedByCeo = isLocked;
      holiday.lockReason = isLocked ? (reason || "Locked by CEO") : null;
    } else {
      holiday = {
        id: this.holidays.length + 1,
        date,
        title: reason || "Executive Lock",
        isLockedByCeo: isLocked,
        lockReason: reason || "Locked by CEO"
      };
      this.holidays.push(holiday);
    }
    return holiday;
  }

  async getChatMessages(threadId = 'general') {
    if (this.isMySQLActive()) {
      const m = await this.mysql.getChatMessages(threadId);
      if (m && m.length > 0) return m;
    }
    return this.chatMessages;
  }

  async addChatMessage(msg) {
    if (this.isMySQLActive()) {
      await this.mysql.addChatMessage(msg);
    }
    this.chatMessages.push(msg);
    return msg;
  }

  async getAuditLogs() {
    if (this.isMySQLActive()) {
      const a = await this.mysql.getAuditLogs();
      if (a && a.length > 0) return a;
    }
    return this.auditLogs;
  }

  async addAudit(actorName, actorRole, action, target, reason, details) {
    const entry = {
      id: "AUD-" + Math.random().toString(36).substring(2, 8).toUpperCase(),
      timestamp: new Date().toISOString(),
      actorName,
      actorRole,
      action,
      target,
      reason,
      details: details || ""
    };
    if (this.isMySQLActive()) {
      await this.mysql.addAudit(actorName, actorRole, action, target, reason, details);
    }
    this.auditLogs.unshift(entry);
    return entry;
  }
}

module.exports = new UnifiedDatabase();
