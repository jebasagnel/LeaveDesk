// Service 1: Identity & Authentication Service (Port 8081)
const express = require('express');
const cors = require('cors');
const db = require('../../db');

const app = express();
const corsOptions = {
  origin: '*',
  methods: ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS'],
  allowedHeaders: ['Content-Type', 'Authorization', 'ngrok-skip-browser-warning', 'Accept', 'X-Requested-With'],
  credentials: true
};
app.use(cors(corsOptions));
app.options('*', cors(corsOptions));
app.use(express.json());

// POST /api/v1/auth/login
app.post('/api/v1/auth/login', async (req, res) => {
  try {
    console.log('[Auth Service] POST /auth/login - Body:', { identifier: req.body?.identifier, organization: req.body?.organization });
    const { identifier, password, organization } = req.body;
    if (!identifier || !password) {
      return res.status(400).json({ error: "Identifier and password are required." });
    }

    if (organization) {
      const cleanOrg = organization.trim().toLowerCase();
      const validOrgs = ["leavedesk.corp", "leavedesk", "corp01", "corp", "executive", "engineering", "operations"];
      const matchesOrg = validOrgs.some(o => cleanOrg.includes(o) || o.includes(cleanOrg));
      if (!matchesOrg) {
        return res.status(401).json({ error: "Invalid organization domain or tenant code." });
      }
    }

    const user = await db.findUserByIdentifier(identifier);
    if (!user || user.password !== password) {
      return res.status(401).json({ error: "Invalid credentials. Please verify your Employee ID or email and password." });
    }

    // Create JWT token payload simulator
    const token = `jwt_live_${user.id}_${Date.now()}`;
    const refreshToken = `ref_live_${user.id}_${Date.now()}`;

    // Automatically record login event in MySQL audit_logs
    try {
      await db.addAudit(
        user.name,
        user.role,
        "USER_LOGIN",
        user.employeeId,
        "User authenticated via credentials",
        `Organization: ${organization || "Default"} • Email: ${user.email}`
      );
    } catch (auditErr) {
      console.error("Audit login write error:", auditErr.message);
    }

    const { password: _, ...safeUser } = user;
    res.json({
      token,
      refreshToken,
      user: safeUser
    });
  } catch (err) {
    console.error("Auth login error:", err);
    res.status(500).json({ error: "Internal Authentication Error" });
  }
});

// GET /api/v1/auth/users
app.get('/api/v1/auth/users', async (req, res) => {
  try {
    const users = await db.getAllUsers();
    res.json(users);
  } catch (err) {
    res.status(500).json({ error: "Failed to fetch users" });
  }
});

// GET /api/v1/auth/teams/:groupId/members
app.get('/api/v1/auth/teams/:groupId/members', async (req, res) => {
  try {
    const { groupId } = req.params;
    const members = await db.getTeamMembers(groupId);
    res.json(members);
  } catch (err) {
    res.status(500).json({ error: "Failed to fetch team members" });
  }
});

const PORT = process.env.PORT || 8081;
if (require.main === module) {
  app.listen(PORT, () => {
    console.log(`[Service 1: Auth] Running on http://localhost:${PORT}`);
  });
}

module.exports = app;
