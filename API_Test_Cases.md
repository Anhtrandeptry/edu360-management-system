# EDU360 API Test Cases

## Test Case Documentation

| No | Module Name | Method Name | Endpoint | HTTP Method | Description | Pre-Condition | Test Data | Expected Result |
|----|-------------|-------------|----------|-------------|-------------|---------------|-----------|-----------------|
| 1 | Attendance | upsertAttendanceByClassAndDate | `/api/attendance/class/{classId}/date/{date}` | PUT | Cập nhật điểm danh theo lớp và ngày | - User đăng nhập với role TEACHER<br>- Class tồn tại<br>- Date hợp lệ | classId: 1<br>date: "2024-12-12"<br>timeSlotId: 1<br>items: [{studentId: 1, status: "PRESENT"}] | Status 200<br>Điểm danh được cập nhật thành công |
| 2 | Enrollment | enrollBulk | `/api/enrollments/bulk` | POST | Đăng ký hàng loạt sinh viên vào lớp | - User đăng nhập với role ADMIN<br>- Class tồn tại và còn chỗ<br>- Students tồn tại | classId: 1<br>studentIds: [1,2,3] | Status 200<br>Danh sách enrollment được tạo |
| 3 | Enrollment | enrollAfterPayment | `/api/enrollments/after-payment` | POST | Đăng ký sau khi thanh toán | - Payment đã được xác nhận<br>- Class còn chỗ<br>- Student chưa đăng ký | paymentId: 1<br>classId: 1<br>studentId: 1 | Status 200<br>Enrollment được tạo |
| 4 | Enrollment | selfEnroll | `/api/enrollments/self-enroll` | POST | Sinh viên tự đăng ký lớp | - User đăng nhập với role STUDENT<br>- Class PUBLIC và còn chỗ<br>- Không xung đột lịch | classId: 1 | Status 200<br>Enrollment thành công |
| 5 | Payment | confirmPayment | `/api/payments/confirm` | POST | Xác nhận thanh toán | - Payment tồn tại<br>- Payment chưa được confirm | paymentId: 1<br>status: "COMPLETED" | Status 200<br>Payment được confirm |
| 6 | Payment | handleCassoWebhook | `/api/payments/webhook/casso` | POST | Xử lý webhook từ Casso | - Webhook signature hợp lệ | Casso webhook payload | Status 200<br>Payment được cập nhật |
| 7 | Payment | handleVietQrCallback | `/api/payments/callback/vietqr` | GET | Xử lý callback từ VietQR | - Payment tồn tại | paymentId: 1<br>status: "success" | Status 200<br>Redirect về trang success |
| 8 | Class | updateClass | `/api/classes/{id}` | PUT | Cập nhật thông tin lớp | - User có quyền ADMIN/TEACHER<br>- Class tồn tại | id: 1<br>name: "Math 102"<br>status: "PUBLIC" | Status 200<br>Class được cập nhật |
| 9 | Class | listClasses | `/api/classes` | GET | Lấy danh sách lớp học | - User đăng nhập | teacherId: 1 (optional)<br>timeSlotId: 1 (optional) | Status 200<br>Danh sách classes |
| 10 | Class | createClass | `/api/classes` | POST | Tạo lớp học mới | - User có quyền ADMIN<br>- Subject, Teacher, Room tồn tại<br>- Không xung đột lịch | name: "Math 101"<br>subjectId: 1<br>teacherId: 1<br>roomId: 1<br>startDate: "2024-09-01"<br>endDate: "2024-12-31"<br>totalSessions: 30<br>maxStudents: 25<br>schedule: [{dayOfWeek: 1, timeSlotId: 1}] | Status 201<br>Class được tạo thành công |
| 11 | Class | getClass | `/api/classes/{id}` | GET | Lấy thông tin chi tiết lớp | - Class tồn tại | id: 1 | Status 200<br>Thông tin class đầy đủ |
| 12 | User | updateUserStatus | `/api/users/{id}/status` | PUT | Cập nhật trạng thái user | - User có quyền ADMIN<br>- Target user tồn tại | id: 1<br>active: true | Status 200<br>User status được cập nhật |
| 13 | User | getAll | `/api/users` | GET | Lấy danh sách tất cả users | - User có quyền ADMIN | page: 0<br>size: 10 | Status 200<br>Danh sách users phân trang |
| 14 | User | getById | `/api/users/{id}` | GET | Lấy thông tin user theo ID | - User đăng nhập | id: 1 | Status 200<br>Thông tin user |
| 15 | Schedule | getScheduleByDate | `/api/my-schedule/date/{date}` | GET | Lấy lịch học theo ngày | - User đăng nhập với role STUDENT | date: "2024-12-12" | Status 200<br>Lịch học trong ngày |
| 16 | Semester | getAll | `/api/semesters` | GET | Lấy danh sách học kỳ | - User đăng nhập | statusFilter: "OPEN" (optional) | Status 200<br>Danh sách semesters |
| 17 | Schedule | getScheduleByWeek | `/api/my-schedule/week` | GET | Lấy lịch học theo tuần | - User đăng nhập với role STUDENT | startDate: "2024-12-09" | Status 200<br>Lịch học cả tuần |
| 18 | Schedule | getCurrentWeekStart | `/api/my-schedule/current-week` | GET | Lấy ngày bắt đầu tuần hiện tại | - User đăng nhập | None | Status 200<br>Ngày đầu tuần (Monday) |
| 19 | Teacher | getTeachers | `/api/teachers` | GET | Lấy danh sách giáo viên | - User đăng nhập | subjectId: 1 (optional) | Status 200<br>Danh sách teachers |
| 20 | Room | getAllRooms | `/api/rooms` | GET | Lấy danh sách phòng học | - User đăng nhập | status: "AVAILABLE" (optional) | Status 200<br>Danh sách rooms |
| 21 | Room | getRoomById | `/api/rooms/{id}` | GET | Lấy thông tin phòng theo ID | - Room tồn tại | id: 1 | Status 200<br>Thông tin room |
| 22 | User | getByUserId | `/api/students/profile` hoặc `/api/teachers/profile` | GET | Lấy thông tin profile theo userId | - User đăng nhập | userId từ token | Status 200<br>Thông tin profile |
| 23 | Auth | registerStudentWithParent | `/api/auth/register-student` | POST | Đăng ký tài khoản sinh viên và phụ huynh | - Email chưa tồn tại<br>- Phone chưa tồn tại | username: "student1"<br>email: "student1@edu.vn"<br>password: "Pass@123"<br>fullName: "Nguyen Van A"<br>phone: "0123456789"<br>parentName: "Nguyen Van B"<br>parentPhone: "0987654321" | Status 201<br>Tài khoản được tạo |
| 24 | Auth | registerTeacher | `/api/auth/register-teacher` | POST | Đăng ký tài khoản giáo viên | - Email chưa tồn tại<br>- User có quyền ADMIN | username: "teacher1"<br>email: "teacher1@edu.vn"<br>password: "Pass@123"<br>fullName: "Tran Thi C"<br>phone: "0111222333"<br>subjectId: 1 | Status 201<br>Tài khoản teacher được tạo |
| 25 | Auth | forgotPassword | `/api/auth/forgot-password` | POST | Yêu cầu reset mật khẩu | - Email tồn tại trong hệ thống | email: "user@edu.vn" | Status 200<br>Email reset được gửi |
| 26 | Auth | authenticateUser | `/api/auth/login` | POST | Đăng nhập vào hệ thống | - User tồn tại và active | username: "admin"<br>password: "admin123" | Status 200<br>JWT token được trả về |
| 27 | Auth | logoutUser | `/api/auth/logout` | POST | Đăng xuất khỏi hệ thống | - User đã đăng nhập | Authorization: Bearer {token} | Status 200<br>Logout thành công |

## Test Scenarios by Priority

### Priority 1: Critical Path (Must Test)
1. **Authentication Flow**
   - Login → Get Profile → Logout
2. **Class Creation Flow**
   - Create Class → List Classes → Get Class Details
3. **Enrollment Flow**
   - Self Enroll → Check Enrollment → View Schedule
4. **Attendance Flow**
   - Get Today Sessions → Mark Attendance → Save

### Priority 2: Important Features
1. **Payment Flow**
   - Create Payment → Webhook Callback → Enroll After Payment
2. **User Management**
   - Register Student → Update Status → Get User List
3. **Schedule Management**
   - Get Schedule by Date → Get Weekly Schedule

### Priority 3: Additional Features
1. Room Management
2. Teacher Management
3. Semester Management
4. Password Reset

## Test Data Setup

### Test Users
```json
{
  "admin": {
    "username": "admin",
    "password": "admin123",
    "role": "ADMIN"
  },
  "teacher": {
    "username": "teacher1",
    "password": "Teacher@123",
    "role": "TEACHER"
  },
  "student": {
    "username": "student1",
    "password": "Student@123",
    "role": "STUDENT"
  }
}
```

### Test Classes
```json
{
  "class1": {
    "id": 1,
    "name": "Math 101",
    "status": "PUBLIC",
    "maxStudents": 25
  }
}
```

## Negative Test Cases

| No | Test Case | Input | Expected Result |
|----|-----------|-------|-----------------|
| 1 | Login with invalid credentials | username: "wrong", password: "wrong" | Status 401 Unauthorized |
| 2 | Create class without authentication | No token | Status 401 Unauthorized |
| 3 | Enroll to full class | classId với maxStudents đã đủ | Status 400 Bad Request - "Class is full" |
| 4 | Create class with schedule conflict | Teacher đã có lớp cùng giờ | Status 400 Bad Request - "Teacher conflict" |
| 5 | Self enroll with schedule conflict | Student đã có lớp cùng giờ | Status 400 Bad Request - "Schedule conflict" |
| 6 | Update class without permission | Student token | Status 403 Forbidden |
| 7 | Get non-existent class | id: 99999 | Status 404 Not Found |
| 8 | Register with duplicate email | Email đã tồn tại | Status 400 Bad Request - "Email already exists" |

## Environment Variables

```properties
BASE_URL=http://localhost:8080
DB_URL=jdbc:mysql://localhost:3306/edu360_db
JWT_SECRET=your-secret-key
JWT_EXPIRATION=86400000
```

## Notes
- Tất cả các API (trừ login, register, forgot-password) cần JWT token trong header
- Format: `Authorization: Bearer {token}`
- Token có thời hạn 24h (86400000ms)
- Sử dụng Content-Type: application/json cho tất cả request body
