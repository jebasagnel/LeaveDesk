package com.example

import com.example.data.LeaveDeskRepository
import com.example.model.LeaveType
import com.example.model.UserRole
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class ExampleUnitTest {

  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testWorkingDaysCalculationExcludesWeekends() {
    val repo = LeaveDeskRepository()
    val monday = LocalDate.of(2026, 11, 2)
    val sunday = monday.plusDays(6)

    val workingDays = repo.calculateWorkingDays("g_eng", monday, sunday, isHalfDay = false)
    assertTrue("Should be 4 or 5 working days depending on holidays", workingDays in 4f..5f)
  }

  @Test
  fun testLongLeaveRequiresCeoApproval() {
    val repo = LeaveDeskRepository()
    val emp = LeaveDeskRepository.DEMO_USERS.first { it.role == UserRole.EMPLOYEE }
    val monday = LocalDate.now().plusDays(1)
    val friday = monday.plusDays(4) // 4 working days

    val validation = repo.validateLeaveApplication(
      user = emp,
      leaveType = LeaveType.EL,
      startDate = monday,
      endDate = friday,
      isHalfDay = false,
      hasProof = true
    )

    assertTrue("Leaves >= 3 days must route to CEO", validation.requiresCeoApproval)
  }

  @Test
  fun testSickLeaveIsNeverBlocked() {
    assertTrue(LeaveType.SL.isUrgentNeverBlocked)
  }

  @Test
  fun testHalfDayWorkingDaysIsHalf() {
    val repo = LeaveDeskRepository()
    val today = LocalDate.now()
    val days = repo.calculateWorkingDays("g_eng", today, today, isHalfDay = true)
    assertEquals(0.5f, days, 0.001f)
  }

  @Test
  fun testCeoToggleDateLock() {
    val repo = LeaveDeskRepository()
    val ceo = LeaveDeskRepository.DEMO_USERS.first { it.role == UserRole.CEO }
    val testDate = LocalDate.now().plusDays(20)

    val locked = repo.ceoToggleDateLock(ceo, testDate)
    assertTrue("Date should now be locked", locked)

    val unlocked = repo.ceoToggleDateLock(ceo, testDate)
    assertFalse("Date should now be unlocked", unlocked)
  }

  @Test
  fun testAvailableChatThreadsContainDirectoryMetadata() {
    val repo = LeaveDeskRepository()
    val ceo = LeaveDeskRepository.DEMO_USERS.first { it.role == UserRole.CEO }
    val threads = repo.getAvailableChatThreads(ceo)

    assertTrue("CEO should have multiple threads for employees and TLs", threads.size >= 5)
    val empThread = threads.find { it.role == UserRole.EMPLOYEE }
    assertNotNull("Should have employee thread", empThread)
    assertNotNull("Should have employeeId in directory metadata", empThread?.employeeId)
    assertNotNull("Should have designation in directory metadata", empThread?.designation)
    assertNotNull("Should have groupId in directory metadata", empThread?.groupId)
  }

  @Test
  fun testEmployeeCannotInitiallyMessageCeoWithoutQuery() {
    val repo = LeaveDeskRepository()
    // Maya Lin (u_emp6) has no queries from CEO, so direct CEO line is strictly locked
    val canChat = repo.canEmployeeChatWithCeo("u_emp6")
    assertFalse("Employee without active query cannot initiate contact with CEO", canChat)

    // Alex Chen (u_emp1) has an active CEO query on LR-89102, so communication is unlocked
    val canAlexChat = repo.canEmployeeChatWithCeo("u_emp1")
    assertTrue("Employee with CEO query can reply and share proofs in chat", canAlexChat)
  }
}
