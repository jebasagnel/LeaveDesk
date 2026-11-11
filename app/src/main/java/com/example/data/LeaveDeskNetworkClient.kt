package com.example.data

import com.example.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class LeaveDeskNetworkClient {

  private val client = OkHttpClient.Builder()
    .addInterceptor { chain ->
      val original = chain.request()
      val request = original.newBuilder()
        .header("ngrok-skip-browser-warning", "true")
        .header("Accept", "application/json")
        .build()
      chain.proceed(request)
    }
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .writeTimeout(15, TimeUnit.SECONDS)
    .build()

  private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

  private fun cleanBaseUrl(url: String): String {
    val trimmed = url.trim()
    return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
  }

  suspend fun checkHealth(baseUrl: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
    try {
      val url = cleanBaseUrl(baseUrl) + "health"
      val request = Request.Builder().url(url).get().build()
      client.newCall(request).execute().use { response ->
        if (response.isSuccessful) {
          val body = response.body?.string() ?: ""
          val json = JSONObject(body)
          val db = json.optString("database", "ONLINE")
          Pair(true, "Gateway Online • Database: $db")
        } else {
          Pair(false, "Server returned HTTP ${response.code}")
        }
      }
    } catch (e: Exception) {
      Pair(false, "Offline / Unreachable (${e.localizedMessage ?: "Connection Refused"})")
    }
  }

  suspend fun login(baseUrl: String, identifier: String, pass: String): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
    try {
      val url = cleanBaseUrl(baseUrl) + "api/v1/auth/login"
      val json = JSONObject().apply {
        put("identifier", identifier)
        put("password", pass)
      }
      val request = Request.Builder()
        .url(url)
        .post(json.toString().toRequestBody(jsonMediaType))
        .build()

      client.newCall(request).execute().use { response ->
        if (response.isSuccessful) {
          val body = response.body?.string() ?: ""
          val resObj = JSONObject(body)
          val token = resObj.optString("token")
          Pair(true, token)
        } else {
          Pair(false, null)
        }
      }
    } catch (_: Exception) {
      Pair(false, null)
    }
  }

  suspend fun fetchLeaveRequests(baseUrl: String): List<LeaveRequest>? = withContext(Dispatchers.IO) {
    try {
      val url = cleanBaseUrl(baseUrl) + "api/v1/leaves/requests"
      val request = Request.Builder().url(url).get().build()
      client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) return@withContext null
        val body = response.body?.string() ?: return@withContext null
        val array = JSONArray(body)
        val list = mutableListOf<LeaveRequest>()
        for (i in 0 until array.length()) {
          val obj = array.getJSONObject(i)
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
              status = LeaveStatus.valueOf(obj.optString("status", "PENDING")),
              submittedAt = LocalDateTime.now(),
              queryQuestion = obj.optString("queryQuestion").takeIf { it.isNotBlank() },
              querySuggestedDates = obj.optString("querySuggestedDates").takeIf { it.isNotBlank() }
            )
          )
        }
        list
      }
    } catch (_: Exception) {
      null
    }
  }

  suspend fun submitLeave(baseUrl: String, req: LeaveRequest): Boolean = withContext(Dispatchers.IO) {
    try {
      val url = cleanBaseUrl(baseUrl) + "api/v1/leaves/apply"
      val json = JSONObject().apply {
        put("id", req.id)
        put("userId", req.userId)
        put("leaveType", req.leaveType.name)
        put("startDate", req.startDate.toString())
        put("endDate", req.endDate.toString())
        put("isHalfDay", req.isHalfDay)
        put("reason", req.reason)
        put("backupUserId", req.backupUserId ?: "")
        put("proofFileName", req.proof?.fileName ?: "")
      }
      val request = Request.Builder()
        .url(url)
        .post(json.toString().toRequestBody(jsonMediaType))
        .build()

      client.newCall(request).execute().use { it.isSuccessful }
    } catch (_: Exception) {
      false
    }
  }

  suspend fun submitDecision(
    baseUrl: String,
    reqId: String,
    role: String,
    decision: String,
    remarks: String?,
    userId: String? = null,
    leaveType: String? = null,
    workingDays: Float? = null
  ): Boolean = withContext(Dispatchers.IO) {
    try {
      val url = cleanBaseUrl(baseUrl) + "api/v1/leaves/$reqId/decision"
      val json = JSONObject().apply {
        put("role", role)
        put("approverRole", role)
        put("approverUserId", if (role == "CEO") "u_ceo" else if (role == "TEAM_LEAD") "u_tl1" else "u_emp1")
        put("decision", decision)
        put("comment", remarks ?: "")
        userId?.let { put("userId", it) }
        leaveType?.let { put("leaveType", it) }
        workingDays?.let { put("workingDays", it) }
      }
      val request = Request.Builder()
        .url(url)
        .post(json.toString().toRequestBody(jsonMediaType))
        .build()

      client.newCall(request).execute().use { it.isSuccessful }
    } catch (_: Exception) {
      false
    }
  }

  suspend fun submitQuery(
    baseUrl: String,
    reqId: String,
    role: String,
    question: String,
    suggestedDates: String?
  ): Boolean = withContext(Dispatchers.IO) {
    try {
      val url = cleanBaseUrl(baseUrl) + "api/v1/leaves/$reqId/query"
      val json = JSONObject().apply {
        put("role", role)
        put("question", question)
        put("suggestedDates", suggestedDates ?: "")
      }
      val request = Request.Builder()
        .url(url)
        .post(json.toString().toRequestBody(jsonMediaType))
        .build()

      client.newCall(request).execute().use { it.isSuccessful }
    } catch (_: Exception) {
      false
    }
  }

  suspend fun fetchChatMessages(baseUrl: String, threadId: String): List<ChatMessage>? = withContext(Dispatchers.IO) {
    try {
      val url = cleanBaseUrl(baseUrl) + "api/v1/chat/messages?threadId=$threadId"
      val request = Request.Builder().url(url).get().build()
      client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) return@withContext null
        val body = response.body?.string() ?: return@withContext null
        val array = JSONArray(body)
        val list = mutableListOf<ChatMessage>()
        for (i in 0 until array.length()) {
          val obj = array.getJSONObject(i)
          list.add(
            ChatMessage(
              id = obj.getString("id"),
              threadId = obj.optString("threadId", threadId),
              senderId = obj.getString("senderId"),
              senderName = obj.optString("senderName", "User"),
              senderRole = UserRole.valueOf(obj.optString("senderRole", "EMPLOYEE")),
              text = obj.getString("message"),
              timestamp = LocalDateTime.now(),
              relatedRequestId = obj.optString("relatedRequestId").takeIf { it.isNotBlank() }
            )
          )
        }
        list
      }
    } catch (_: Exception) {
      null
    }
  }

  suspend fun sendChatMessage(baseUrl: String, msg: ChatMessage): Boolean = withContext(Dispatchers.IO) {
    try {
      val url = cleanBaseUrl(baseUrl) + "api/v1/chat/messages"
      val json = JSONObject().apply {
        put("senderId", msg.senderId)
        put("message", msg.text)
        put("threadId", msg.threadId)
        put("relatedRequestId", msg.relatedRequestId ?: "")
      }
      val request = Request.Builder()
        .url(url)
        .post(json.toString().toRequestBody(jsonMediaType))
        .build()

      client.newCall(request).execute().use { it.isSuccessful }
    } catch (_: Exception) {
      false
    }
  }

  suspend fun fetchHolidays(baseUrl: String): List<CompanyHoliday>? = withContext(Dispatchers.IO) {
    try {
      val url = cleanBaseUrl(baseUrl) + "api/v1/calendar/holidays"
      val request = Request.Builder().url(url).get().build()
      client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) return@withContext null
        val body = response.body?.string() ?: return@withContext null
        val array = JSONArray(body)
        val list = mutableListOf<CompanyHoliday>()
        for (i in 0 until array.length()) {
          val obj = array.getJSONObject(i)
          list.add(
            CompanyHoliday(
              id = "HOL-${i + 1}",
              date = LocalDate.parse(obj.getString("date")),
              title = obj.getString("title"),
              type = HolidayType.PUBLIC_HOLIDAY,
              remark = obj.optString("lockReason", ""),
              isLockedByCeo = obj.optBoolean("isLockedByCeo", false)
            )
          )
        }
        list
      }
    } catch (_: Exception) {
      null
    }
  }

  suspend fun toggleLock(baseUrl: String, date: String, isLocked: Boolean): Boolean {
    return setHolidayLock(baseUrl, date, "Locked from LeaveDesk app", isLocked)
  }

  suspend fun setHolidayLock(baseUrl: String, date: String, reason: String?, isLocked: Boolean): Boolean = withContext(Dispatchers.IO) {
    try {
      val url = cleanBaseUrl(baseUrl) + "api/v1/calendar/date-lock"
      val json = JSONObject().apply {
        put("date", date)
        put("reason", reason ?: "")
        put("isLocked", isLocked)
      }
      val request = Request.Builder()
        .url(url)
        .post(json.toString().toRequestBody(jsonMediaType))
        .build()

      client.newCall(request).execute().use { it.isSuccessful }
    } catch (_: Exception) {
      false
    }
  }

  suspend fun logAudit(
    baseUrl: String,
    actorName: String,
    actorRole: String,
    action: String,
    target: String,
    reason: String?,
    details: String?
  ): Boolean = withContext(Dispatchers.IO) {
    try {
      val url = cleanBaseUrl(baseUrl) + "api/v1/audit/log"
      val json = JSONObject().apply {
        put("actorName", actorName)
        put("actorRole", actorRole)
        put("action", action)
        put("target", target)
        put("reason", reason ?: "")
        put("details", details ?: "")
      }
      val request = Request.Builder()
        .url(url)
        .post(json.toString().toRequestBody(jsonMediaType))
        .build()

      client.newCall(request).execute().use { it.isSuccessful }
    } catch (_: Exception) {
      false
    }
  }
}

