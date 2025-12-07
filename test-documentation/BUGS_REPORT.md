# Bug Report - EDU360 Management System

## Tổng Quan
**Số lượng bugs phát hiện:** 2  
**Severity:** Medium  
**Status:** Cần fix  
**Phát hiện qua:** Unit Testing  

---

## 🐛 Bug #1: NullPointerException in AttendanceService

### Thông Tin
- **Service:** AttendanceService
- **Method:** `getTodaySessionsForTeacher()`
- **File:** `AttendanceService.java`
- **Line:** 69
- **Test case phát hiện:** `test10_sessionMissingRoom`

### Mô Tả
Khi `ClassSession` không có `Room` (room = null), code gọi trực tiếp `s.getRoom().getName()` gây ra NullPointerException.

### Code Hiện Tại (Có Bug)
```java
return sessions.stream().map(s -> {
    boolean marked = attendanceRepository.findBySession_Id(s.getId()).stream()
            .anyMatch(a -> a.getStatus() != AttendanceStatus.UNMARKED);
    return AttendanceSessionSummaryResponse.builder()
            .sessionId(s.getId())
            .classId(s.getClazz().getId())
            .className(s.getClazz().getName())
            .subjectName(s.getClazz().getSubject().getName())
            .roomName(s.getRoom().getName())  // ❌ BUG: NullPointerException nếu room null
            .timeStart(s.getTimeSlot().getStartTime().toString())
            .timeEnd(s.getTimeSlot().getEndTime().toString())
            .marked(marked)
            .build();
}).toList();
```

### Stack Trace
```
java.lang.NullPointerException: Cannot invoke "fpt.capstone.edu360managementsystem.entity.Room.getName()" 
because the return value of "fpt.capstone.edu360managementsystem.entity.ClassSession.getRoom()" is null
    at fpt.capstone.edu360managementsystem.service.AttendanceService.lambda$getTodaySessionsForTeacher$3(AttendanceService.java:69)
```

### Test Case
```java
@Test void test10_sessionMissingRoom() {
    // Arrange: Cố tình set room = null
    session.setRoom(null);
    when(teacherRepository.findAll()).thenReturn(Arrays.asList(teacher));
    when(classSessionRepository.findTodaySessionsForTeacher(1L, LocalDate.now()))
        .thenReturn(Arrays.asList(session));
    when(attendanceRepository.findBySession_Id(1L)).thenReturn(Collections.emptyList());
    
    // Act
    List<AttendanceSessionSummaryResponse> result = attendanceService.getTodaySessionsForTeacher(1L);
    
    // Assert: Mong đợi roomName = "N/A"
    assertThat(result.get(0).getRoomName()).isEqualTo("N/A");
}
```

### Code Đúng (Đã Fix)
```java
return sessions.stream().map(s -> {
    boolean marked = attendanceRepository.findBySession_Id(s.getId()).stream()
            .anyMatch(a -> a.getStatus() != AttendanceStatus.UNMARKED);
    return AttendanceSessionSummaryResponse.builder()
            .sessionId(s.getId())
            .classId(s.getClazz().getId())
            .className(s.getClazz().getName())
            .subjectName(s.getClazz().getSubject().getName())
            .roomName(s.getRoom() != null ? s.getRoom().getName() : "N/A")  // ✅ FIXED
            .timeStart(s.getTimeSlot().getStartTime().toString())
            .timeEnd(s.getTimeSlot().getEndTime().toString())
            .marked(marked)
            .build();
}).toList();
```

### Impact
- **User Impact:** Khi tạo session mà chưa assign room, app sẽ crash
- **Frequency:** Trung bình - có thể xảy ra khi admin tạo session draft
- **Workaround:** Luôn assign room trước khi save session

### Priority
🔴 **MEDIUM** - Cần fix trước khi release

---

## 🐛 Bug #2: NullPointerException in EnrollmentService

### Thông Tin
- **Service:** EnrollmentService
- **Method:** `enrollOne()`
- **File:** `EnrollmentService.java`
- **Line:** 73
- **Test case phát hiện:** `test11_enrollOne_semesterNull_skipConflictCheck`

### Mô Tả
Khi `Clazz` không có `Semester` (semester = null), code gọi trực tiếp `clazz.getSemester().getId()` để check schedule conflicts, gây ra NullPointerException.

### Code Hiện Tại (Có Bug)
```java
// Schedule conflicts in same semester
var schedules = classScheduleRepository.findByClazz_Id(classId);
var dows = schedules.stream().map(ClassSchedule::getDayOfWeek).collect(Collectors.toSet());
var slotIds = schedules.stream().map(s -> s.getTimeSlot().getId()).collect(Collectors.toSet());

var conflicts = classEnrollmentRepository.findScheduleConflicts(
        student.getId(), 
        clazz.getSemester().getId(),  // ❌ BUG: NullPointerException nếu semester null
        dows, 
        slotIds
);
if (!conflicts.isEmpty()) {
    throw new RuntimeException("Schedule conflict with other enrolled classes");
}
```

### Stack Trace
```
java.lang.NullPointerException: Cannot invoke "fpt.capstone.edu360managementsystem.entity.Semester.getId()" 
because the return value of "fpt.capstone.edu360managementsystem.entity.Clazz.getSemester()" is null
    at fpt.capstone.edu360managementsystem.service.EnrollmentService.enrollOne(EnrollmentService.java:73)
```

### Test Case
```java
@Test void test11_enrollOne_semesterNull_skipConflictCheck() {
    // Arrange: Cố tình set semester = null
    clazz.setSemester(null);
    when(clazzRepository.findById(1L)).thenReturn(Optional.of(clazz));
    when(studentRepository.findById(1L)).thenReturn(Optional.of(student));
    when(classEnrollmentRepository.countByClazz_Id(1L)).thenReturn(0);
    when(classEnrollmentRepository.existsByClazzAndStudent(clazz, student)).thenReturn(false);
    when(classEnrollmentRepository.save(any())).thenReturn(null);
    
    EnrollStudentRequest req = new EnrollStudentRequest();
    req.setStudentId(1L);
    
    // Act
    enrollmentService.enrollOne(1L, req, 1L, false);
    
    // Assert: Mong đợi bỏ qua conflict check khi semester null
    verify(classEnrollmentRepository, never()).findScheduleConflicts(anyLong(), anyLong(), anySet(), anySet());
    verify(classScheduleRepository, never()).findByClazz_Id(anyLong());
}
```

### Code Đúng (Đã Fix)
```java
// Schedule conflicts in same semester - chỉ check khi có semester
if (clazz.getSemester() != null) {  // ✅ ADDED NULL CHECK
    var schedules = classScheduleRepository.findByClazz_Id(classId);
    var dows = schedules.stream().map(ClassSchedule::getDayOfWeek).collect(Collectors.toSet());
    var slotIds = schedules.stream().map(s -> s.getTimeSlot().getId()).collect(Collectors.toSet());

    var conflicts = classEnrollmentRepository.findScheduleConflicts(
            student.getId(), 
            clazz.getSemester().getId(),
            dows, 
            slotIds
    );
    if (!conflicts.isEmpty()) {
        throw new RuntimeException("Schedule conflict with other enrolled classes");
    }
}
```

### Impact
- **User Impact:** Không thể enroll student vào class nếu class chưa có semester
- **Frequency:** Thấp - chỉ xảy ra khi admin tạo class draft chưa assign semester
- **Workaround:** Luôn assign semester trước khi enroll students

### Priority
🔴 **MEDIUM** - Cần fix trước khi release

---

## 📊 Phân Tích

### Root Cause
Cả 2 bugs đều do **thiếu null-safety checks** khi xử lý related entities.

### Nguyên Nhân
- Code giả định rằng entities luôn có đầy đủ relationships
- Không xử lý trường hợp draft/incomplete data
- Thiếu defensive programming

### Best Practice Để Tránh
```java
// ❌ BAD
entity.getRelation().getField()

// ✅ GOOD - Option 1: Ternary operator
entity.getRelation() != null ? entity.getRelation().getField() : defaultValue

// ✅ GOOD - Option 2: Optional
Optional.ofNullable(entity.getRelation())
    .map(Relation::getField)
    .orElse(defaultValue)

// ✅ GOOD - Option 3: If check
if (entity.getRelation() != null) {
    // Use relation
}
```

---

## ✅ Verification Plan

### Sau Khi Fix
1. **Chạy lại test cases:**
   ```bash
   mvn test -Dtest=AttendanceServiceTest#test10_sessionMissingRoom
   mvn test -Dtest=EnrollmentServiceTest#test11_enrollOne_semesterNull_skipConflictCheck
   ```

2. **Verify toàn bộ test suite:**
   ```bash
   mvn test
   ```

3. **Expected result:**
   ```
   Tests run: 535, Failures: 0, Errors: 0, Skipped: 0
   ```

---

## 📝 Lessons Learned

### 1. **Value of Unit Testing**
Unit tests đã phát hiện bugs trước khi code được deploy, tiết kiệm thời gian debug và tránh production issues.

### 2. **Edge Cases Matter**
Cần test các edge cases như null values, empty lists, để đảm bảo code robust.

### 3. **Defensive Programming**
Luôn assume rằng external data có thể null/invalid và xử lý accordingly.

### 4. **Clear Test Names**
Tên test rõ ràng (`test10_sessionMissingRoom`) giúp dễ hiểu scenario và debug nhanh.

---

**Report Created:** December 4, 2025  
**Status:** Open  
**Assigned To:** Development Team  
**Next Review:** After fixes implemented
