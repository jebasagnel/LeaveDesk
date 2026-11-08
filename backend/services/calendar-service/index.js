// Service 3: Real-Time Calendar & Notification Hub (Port 8083)
const express = require('express');
const cors = require('cors');
const http = require('http');
const WebSocket = require('ws');
const db = require('../../db');

const app = express();
app.use(cors());
app.use(express.json());

const server = http.createServer(app);
const wss = new WebSocket.Server({ server });

// WebSocket broadcasting to all connected clients
function broadcast(event, payload) {
  const data = JSON.stringify({ event, payload, timestamp: new Date().toISOString() });
  wss.clients.forEach(client => {
    if (client.readyState === WebSocket.OPEN) {
      client.send(data);
    }
  });
}

wss.on('connection', (ws) => {
  console.log('[Service 3: WebSocket] Client connected.');
  ws.send(JSON.stringify({ event: 'CONNECTED', message: 'Real-time sync online' }));
});

// GET /api/v1/calendar/holidays
app.get('/api/v1/calendar/holidays', async (req, res) => {
  try {
    const holidays = await db.getHolidays();
    res.json(holidays);
  } catch (err) {
    res.status(500).json({ error: "Failed to fetch holidays" });
  }
});

// POST /api/v1/calendar/date-lock (CEO 1-tap lock)
app.post('/api/v1/calendar/date-lock', async (req, res) => {
  try {
    const { date, reason, isLocked } = req.body;
    const holiday = await db.setHolidayLock(date, reason, isLocked);
    broadcast('CALENDAR_LOCK_CHANGED', holiday);
    res.json(holiday);
  } catch (err) {
    res.status(500).json({ error: "Failed to update date lock" });
  }
});

// GET /api/v1/chat/messages
app.get('/api/v1/chat/messages', async (req, res) => {
  try {
    const messages = await db.getChatMessages(req.query.threadId || 'general');
    res.json(messages);
  } catch (err) {
    res.status(500).json({ error: "Failed to fetch chat messages" });
  }
});

// POST /api/v1/chat/messages
app.post('/api/v1/chat/messages', async (req, res) => {
  try {
    const { senderId, message, threadId, relatedRequestId } = req.body;
    const user = await db.getUser(senderId);
    if (!user) return res.status(404).json({ error: "Sender not found" });

    const msg = {
      id: "MSG-" + Math.floor(1000 + Math.random() * 9000),
      senderId,
      senderName: user.name,
      senderRole: user.role,
      message,
      threadId: threadId || "general",
      relatedRequestId: relatedRequestId || null,
      timestamp: new Date().toISOString()
    };

    await db.addChatMessage(msg);
    broadcast('NEW_CHAT_MESSAGE', msg);
    res.status(201).json(msg);
  } catch (err) {
    res.status(500).json({ error: "Failed to send chat message" });
  }
});

const PORT = process.env.PORT || 8083;
if (require.main === module) {
  server.listen(PORT, () => {
    console.log(`[Service 3: Calendar & Real-Time] Running on http://localhost:${PORT}`);
  });
}

module.exports = { app, server };
