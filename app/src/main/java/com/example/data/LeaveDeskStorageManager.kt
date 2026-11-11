package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class LeaveDeskStorageManager(context: Context) {

  private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  companion object {
    private const val PREFS_NAME = "leavedesk_persistent_store"
    private const val KEY_LOGGED_IN_USER_ID = "logged_in_user_id"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_LEAVE_REQUESTS = "leave_requests_json"
    private const val KEY_USER_BALANCES = "user_balances_json"
    private const val KEY_CHAT_MESSAGES = "chat_messages_json"
    private const val KEY_HOLIDAYS = "holidays_json"
    private const val KEY_AUDIT_LOGS = "audit_logs_json"
    private const val KEY_BACKEND_URL = "backend_url"
    const val DEFAULT_BACKEND_URL = "https://juice-seldom-recapture.ngrok-free.dev"
  }

  // MARK: - Backend URL
  fun saveBackendUrl(url: String) {
    prefs.edit().putString(KEY_BACKEND_URL, url.trim()).apply()
  }

  fun loadBackendUrl(): String {
    return prefs.getString(KEY_BACKEND_URL, DEFAULT_BACKEND_URL) ?: DEFAULT_BACKEND_URL
  }

  // MARK: - Auth Session
  fun saveAuthSession(userId: String?, isLoggedIn: Boolean) {
    prefs.edit()
      .putString(KEY_LOGGED_IN_USER_ID, userId)
      .putBoolean(KEY_IS_LOGGED_IN, isLoggedIn)
      .apply()
  }

  fun loadLoggedInUserId(): String? = prefs.getString(KEY_LOGGED_IN_USER_ID, null)
  fun loadIsLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

  // MARK: - Leave Requests
  fun saveLeaveRequests(requests: List<LeaveRequest>) {
    try {
      val array = JSONArray()
      requests.forEach { req ->
        val obj = JSONObject().apply {
          put("id", req.id)
          put("userId", req.userId)
          put("userName", req.userName)
          put("userRole", req.userRole.name)
          put("groupId", req.groupId)
          put("groupName", req.groupName)
          put("leaveType", req.leaveType.name)
          put("startDate", req.startDate.toString())
          put("endDate", req.endDate.toString())
          put("isHalfDay", req.isHalfDay)
          put("halfDaySession", req.halfDaySession ?: "")
          put("workingDays", req.workingDays.toDouble())
          put("reason", req.reason)
          put("backupUserId", req.backupUserId ?: "")
          put("backupUserName", req.backupUserName ?: "")
          put("backupTaskNote", req.backupTaskNote ?: "")
          put("status", req.status.name)
          put("cancelRequested", req.cancelRequested)
          put("cancelReason", req.cancelReason ?: "")
          put("queryQuestion", req.queryQuestion ?: "")
          put("querySuggestedDates", req.querySuggestedDates ?: "")
          put("requiresCeoApproval", req.requiresCeoApproval)
          req.proof?.let { proof ->
            val pObj = JSONObject().apply {
              put("fileName", proof.fileName)
              put("fileType", proof.fileType)
              put("fileSizeStr", proof.fileSizeStr)
              put("uriString", proof.uriString ?: "")
            }
            put("uploadedProof", pObj)
          }
        }
        array.put(obj)
      }
      prefs.edit().putString(KEY_LEAVE_REQUESTS, array.toString()).apply()
    } catch (_: Exception) {}
  }

  fun loadLeaveRequests(): List<LeaveRequest>? {
    val jsonStr = prefs.getString(KEY_LEAVE_REQUESTS, null) ?: return null
    return try {
      val array = JSONArray(jsonStr)
      val list = mutableListOf<LeaveRequest>()
      for (i in 0 until array.length()) {
        val obj = array.getJSONObject(i)
        val proofObj = obj.optJSONObject("uploadedProof")
        val proof = proofObj?.let {
          LeaveProof(
            fileName = it.optString("fileName", "doc.pdf"),
            fileType = it.optString("fileType", "PDF"),
            fileSizeStr = it.optString("fileSizeStr", "500 KB"),
            uriString = it.optString("uriString").takeIf { s -> s.isNotBlank() }
          )
        }

        list.add(
          LeaveRequest(
            id = obj.getString("id"),
            userId = obj.getString("userId"),
            userName = obj.getString("userName"),
            userRole = UserRole.valueOf(obj.optString("userRole", "EMPLOYEE")),
            groupId = obj.getString("groupId"),
            groupName = obj.getString("groupName"),
            leaveType = LeaveType.valueOf(obj.optString("leaveType", "CL")),
            startDate = LocalDate.parse(obj.getString("startDate")),
            endDate = LocalDate.parse(obj.getString("endDate")),
            isHalfDay = obj.optBoolean("isHalfDay", false),
            halfDaySession = obj.optString("halfDaySession").takeIf { it.isNotBlank() },
            workingDays = obj.optDouble("workingDays", 1.0).toFloat(),
            reason = obj.optString("reason", ""),
            backupUserId = obj.optString("backupUserId").takeIf { it.isNotBlank() },
            backupUserName = obj.optString("backupUserName").takeIf { it.isNotBlank() },
            backupTaskNote = obj.optString("backupTaskNote").takeIf { it.isNotBlank() },
            proof = proof,
            status = LeaveStatus.valueOf(obj.optString("status", "PENDING")),
            submittedAt = LocalDateTime.now(),
            cancelRequested = obj.optBoolean("cancelRequested", false),
            cancelReason = obj.optString("cancelReason").takeIf { it.isNotBlank() },
            queryQuestion = obj.optString("queryQuestion").takeIf { it.isNotBlank() },
            querySuggestedDates = obj.optString("querySuggestedDates").takeIf { it.isNotBlank() },
            requiresCeoApproval = obj.optBoolean("requiresCeoApproval", false)
          )
        )
      }
      list
    } catch (_: Exception) {
      null
    }
  }

  // MARK: - User Balances
  fun saveUserBalances(balances: Map<String, List<LeaveQuota>>) {
    try {
      val rootObj = JSONObject()
      balances.forEach { (userId, quotas) ->
        val qArray = JSONArray()
        quotas.forEach { q ->
          val qObj = JSONObject().apply {
            put("leaveType", q.leaveType.name)
            put("total", q.total.toDouble())
            put("used", q.used.toDouble())
          }
          qArray.put(qObj)
        }
        rootObj.put(userId, qArray)
      }
      prefs.edit().putString(KEY_USER_BALANCES, rootObj.toString()).apply()
    } catch (_: Exception) {}
  }

  fun loadUserBalances(): Map<String, List<LeaveQuota>>? {
    val jsonStr = prefs.getString(KEY_USER_BALANCES, null) ?: return null
    return try {
      val rootObj = JSONObject(jsonStr)
      val result = mutableMapOf<String, List<LeaveQuota>>()
      val keys = rootObj.keys()
      while (keys.hasNext()) {
        val userId = keys.next()
        val qArray = rootObj.getJSONArray(userId)
        val qList = mutableListOf<LeaveQuota>()
        for (i in 0 until qArray.length()) {
          val qObj = qArray.getJSONObject(i)
          qList.add(
            LeaveQuota(
              leaveType = LeaveType.valueOf(qObj.getString("leaveType")),
              total = qObj.getDouble("total").toFloat(),
              used = qObj.getDouble("used").toFloat()
            )
          )
        }
        result[userId] = qList
      }
      result
    } catch (_: Exception) {
      null
    }
  }

  // MARK: - Chat Messages
  fun saveChatMessages(messages: List<ChatMessage>) {
    try {
      val array = JSONArray()
      messages.forEach { msg ->
        val obj = JSONObject().apply {
          put("id", msg.id)
          put("threadId", msg.threadId)
          put("senderId", msg.senderId)
          put("senderName", msg.senderName)
          put("senderRole", msg.senderRole.name)
          put("text", msg.text)
          put("timestamp", msg.timestamp.toString())
          put("isRead", msg.isRead)
          put("relatedRequestId", msg.relatedRequestId ?: "")
          put("isOfficialQuery", msg.isOfficialQuery)
          put("isOfficialDecision", msg.isOfficialDecision)
          msg.attachment?.let { att ->
            val aObj = JSONObject().apply {
              put("fileName", att.fileName)
              put("fileType", att.fileType)
              put("fileSizeStr", att.fileSizeStr)
              put("uriString", att.uriString ?: "")
            }
            put("attachment", aObj)
          }
        }
        array.put(obj)
      }
      prefs.edit().putString(KEY_CHAT_MESSAGES, array.toString()).apply()
    } catch (_: Exception) {}
  }

  fun loadChatMessages(): List<ChatMessage>? {
    val jsonStr = prefs.getString(KEY_CHAT_MESSAGES, null) ?: return null
    return try {
      val array = JSONArray(jsonStr)
      val list = mutableListOf<ChatMessage>()
      for (i in 0 until array.length()) {
        val obj = array.getJSONObject(i)
        val attObj = obj.optJSONObject("attachment")
        val attachment = attObj?.let {
          LeaveProof(
            fileName = it.optString("fileName", "doc.pdf"),
            fileType = it.optString("fileType", "PDF"),
            fileSizeStr = it.optString("fileSizeStr", "500 KB"),
            uriString = it.optString("uriString").takeIf { s -> s.isNotBlank() }
          )
        }

        list.add(
          ChatMessage(
            id = obj.getString("id"),
            threadId = obj.getString("threadId"),
            senderId = obj.getString("senderId"),
            senderName = obj.getString("senderName"),
            senderRole = UserRole.valueOf(obj.optString("senderRole", "EMPLOYEE")),
            text = obj.getString("text"),
            timestamp = LocalDateTime.parse(obj.getString("timestamp")),
            isRead = obj.optBoolean("isRead", false),
            relatedRequestId = obj.optString("relatedRequestId").takeIf { it.isNotBlank() },
            attachment = attachment,
            isOfficialQuery = obj.optBoolean("isOfficialQuery", false),
            isOfficialDecision = obj.optBoolean("isOfficialDecision", false)
          )
        )
      }
      list
    } catch (_: Exception) {
      null
    }
  }

  // MARK: - Holidays
  fun saveCompanyHolidays(holidays: List<CompanyHoliday>) {
    try {
      val array = JSONArray()
      holidays.forEach { h ->
        val obj = JSONObject().apply {
          put("id", h.id)
          put("date", h.date.toString())
          put("title", h.title)
          put("type", h.type.name)
          put("remark", h.remark)
          put("colorHex", h.colorHex)
          put("isLockedByCeo", h.isLockedByCeo)
        }
        array.put(obj)
      }
      prefs.edit().putString(KEY_HOLIDAYS, array.toString()).apply()
    } catch (_: Exception) {}
  }

  fun loadCompanyHolidays(): List<CompanyHoliday>? {
    val jsonStr = prefs.getString(KEY_HOLIDAYS, null) ?: return null
    return try {
      val array = JSONArray(jsonStr)
      val list = mutableListOf<CompanyHoliday>()
      for (i in 0 until array.length()) {
        val obj = array.getJSONObject(i)
        list.add(
          CompanyHoliday(
            id = obj.getString("id"),
            date = LocalDate.parse(obj.getString("date")),
            title = obj.optString("title", "Holiday"),
            type = HolidayType.valueOf(obj.optString("type", "PUBLIC_HOLIDAY")),
            remark = obj.optString("remark", ""),
            colorHex = obj.optLong("colorHex", HolidayType.PUBLIC_HOLIDAY.colorHex),
            isLockedByCeo = obj.optBoolean("isLockedByCeo", false)
          )
        )
      }
      list
    } catch (_: Exception) {
      null
    }
  }

  // MARK: - Audit Logs
  fun saveAuditLogs(logs: List<AuditLogEntry>) {
    try {
      val array = JSONArray()
      logs.forEach { log ->
        val obj = JSONObject().apply {
          put("id", log.id)
          put("timestamp", log.timestamp.toString())
          put("actorName", log.actorName)
          put("actorRole", log.actorRole.name)
          put("action", log.action)
          put("target", log.target)
          put("reason", log.reason)
          put("details", log.details)
        }
        array.put(obj)
      }
      prefs.edit().putString(KEY_AUDIT_LOGS, array.toString()).apply()
    } catch (_: Exception) {}
  }

  fun loadAuditLogs(): List<AuditLogEntry>? {
    val jsonStr = prefs.getString(KEY_AUDIT_LOGS, null) ?: return null
    return try {
      val array = JSONArray(jsonStr)
      val list = mutableListOf<AuditLogEntry>()
      for (i in 0 until array.length()) {
        val obj = array.getJSONObject(i)
        list.add(
          AuditLogEntry(
            id = obj.getString("id"),
            timestamp = LocalDateTime.parse(obj.getString("timestamp")),
            actorName = obj.getString("actorName"),
            actorRole = UserRole.valueOf(obj.optString("actorRole", "EMPLOYEE")),
            action = obj.getString("action"),
            target = obj.getString("target"),
            reason = obj.optString("reason", ""),
            details = obj.optString("details", "")
          )
        )
      }
      list
    } catch (_: Exception) {
      null
    }
  }
}
