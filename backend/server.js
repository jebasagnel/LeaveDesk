/**
 * LeaveDesk Unified Local Gateway & Server
 * 
 * Runs all 3 microservices on their dedicated ports simultaneously:
 * - Service 1: Identity & Auth (:8081)
 * - Service 2: Leave Workflow Engine (:8082)
 * - Service 3: Real-Time Calendar & WebSockets (:8083)
 * - Reverse Proxy Gateway (:8084 or configured GATEWAY_PORT)
 * - Database: MySQL with auto-initialization & in-memory fallback
 */

require('dotenv').config();
const express = require('express');
const cors = require('cors');
const mysqlDb = require('./mysql-db');

const authApp = require('./services/auth-service/index');
const workflowApp = require('./services/workflow-service/index');
const { app: calendarApp, server: calendarServer } = require('./services/calendar-service/index');

const AUTH_PORT = parseInt(process.env.AUTH_PORT || '8081', 10);
const WORKFLOW_PORT = parseInt(process.env.WORKFLOW_PORT || '8082', 10);
const CALENDAR_PORT = parseInt(process.env.CALENDAR_PORT || '8083', 10);
let GATEWAY_PORT = parseInt(process.env.GATEWAY_PORT || '8084', 10);

// Initialize MySQL asynchronously
mysqlDb.initMySQL().then(isOnline => {
  if (isOnline) {
    console.log(`🐬 [Database] MySQL cluster ONLINE (Host: ${mysqlDb.config.host}, Database: ${mysqlDb.config.database})`);
  } else {
    console.log(`💾 [Database] In-Memory Fallback store ACTIVE (All endpoints 100% operational)`);
  }
});

// Start Microservice 1
authApp.listen(AUTH_PORT, () => {
  console.log(`✅ [Service 1: Identity & Auth] listening at http://localhost:${AUTH_PORT}`);
}).on('error', (err) => {
  console.log(`ℹ️ [Service 1: Auth] Note on port ${AUTH_PORT}: ${err.message}`);
});

// Start Microservice 2
workflowApp.listen(WORKFLOW_PORT, () => {
  console.log(`✅ [Service 2: Leave Workflow Engine] listening at http://localhost:${WORKFLOW_PORT}`);
}).on('error', (err) => {
  console.log(`ℹ️ [Service 2: Workflow] Note on port ${WORKFLOW_PORT}: ${err.message}`);
});

// Start Microservice 3
calendarServer.listen(CALENDAR_PORT, () => {
  console.log(`✅ [Service 3: Real-Time Calendar & WSS] listening at http://localhost:${CALENDAR_PORT}`);
}).on('error', (err) => {
  console.log(`ℹ️ [Service 3: Calendar] Note on port ${CALENDAR_PORT}: ${err.message}`);
});

// Start API Gateway
const gateway = express();

const corsOptions = {
  origin: '*',
  methods: ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS'],
  allowedHeaders: ['Content-Type', 'Authorization', 'ngrok-skip-browser-warning', 'Accept', 'X-Requested-With'],
  credentials: true
};
gateway.use(cors(corsOptions));
gateway.options('*', cors(corsOptions));
gateway.use(express.json());

// Automated Asynchronous Audit Logging on All State Mutations
const auditMiddleware = require('./middleware/audit-middleware');
gateway.use(auditMiddleware);

// Dedicated endpoint to insert directly into audit_logs from app/clients
gateway.post(['/api/v1/audit/log', '/audit/log'], async (req, res) => {
  try {
    console.log('[Gateway] POST /api/v1/audit/log - Body:', req.body);
    const { actorName, actorRole, action, target, reason, details } = req.body;
    const db = require('./db');
    const entry = await db.addAudit(
      actorName || 'System',
      actorRole || 'EMPLOYEE',
      action || 'ACTION',
      target || 'UNKNOWN',
      reason || '',
      details || ''
    );
    res.status(201).json(entry);
  } catch (err) {
    console.error("Audit log error in SQL execution:", err.sqlMessage || err.message || err);
    res.status(500).json({ error: "Failed to record audit log: " + (err.sqlMessage || err.message) });
  }
});

// Dedicated endpoint to fetch real MySQL activity logs
gateway.get('/api/v1/audit/activity-logs', async (req, res) => {
  try {
    const logs = await mysqlDb.getActivityLogs();
    res.json(logs);
  } catch (err) {
    res.status(500).json({ error: "Failed to fetch activity logs" });
  }
});

// Forwarding rules - mount apps directly so /api/v1/... routes resolve seamlessly
gateway.use(authApp);
gateway.use(workflowApp);
gateway.use(calendarApp);
gateway.use('/api/v1/auth', authApp);
gateway.use('/api/v1/leaves', workflowApp);
gateway.use('/api/v1/audit', workflowApp);
gateway.use('/api/v1/calendar', calendarApp);
gateway.use('/api/v1/chat', calendarApp);

gateway.get('/health', (req, res) => {
  res.json({
    status: "HEALTHY",
    database: mysqlDb.isAvailable() ? "MYSQL_ONLINE" : "IN_MEMORY_ACTIVE",
    databaseHost: mysqlDb.config.host,
    databaseName: mysqlDb.config.database,
    services: {
      auth: `http://localhost:${AUTH_PORT}`,
      workflow: `http://localhost:${WORKFLOW_PORT}`,
      calendar: `http://localhost:${CALENDAR_PORT}`
    },
    version: "1.0.0"
  });
});

function startGateway(port) {
  const server = gateway.listen(port, () => {
    console.log(`\n==================================================`);
    console.log(`🚀 LeaveDesk Unified Gateway ready on http://localhost:${port}`);
    console.log(`==================================================`);
    console.log(`• Test Health:     curl http://localhost:${port}/health`);
    console.log(`• Login Endpoint:  curl -X POST http://localhost:${port}/api/v1/auth/login -H "Content-Type: application/json" -d '{"identifier":"EMP101","password":"password123"}'`);
    console.log(`• Leave Balances:  curl http://localhost:${port}/api/v1/leaves/balances/u_emp1`);
    console.log(`• Calendar Lock:   curl http://localhost:${port}/api/v1/calendar/holidays`);
    console.log(`==================================================\n`);
  });

  server.on('error', (err) => {
    if (err.code === 'EADDRINUSE') {
      console.log(`Port ${port} in use, trying next port ${port + 1}...`);
      startGateway(port + 1);
    } else {
      console.error("Gateway error:", err);
    }
  });
}

startGateway(GATEWAY_PORT);
