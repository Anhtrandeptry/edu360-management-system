# Hướng Dẫn: Lưu Nội Dung Buổi Học

## Tổng Quan

Tính năng này cho phép giáo viên ghi nội dung chi tiết cho mỗi buổi học, bao gồm:

- Chương học được dạy
- Bài học được dạy
- Nội dung mô tả buổi học

## Database Schema

### Bảng: `class_sessions`

Đã thêm cột mới:

```sql
lesson_content TEXT NULL
```

Bảng này lưu thông tin về mỗi buổi học:

- `id` - ID của buổi học
- `class_id` - Lớp học
- `date` - Ngày học
- `timeslot_id` - Khung giờ
- `room_id` - Phòng học
- `status` - Trạng thái (PLANNED, ONGOING, COMPLETED, CANCELLED)
- **`lesson_content`** - Nội dung buổi học (TEXT, nullable)

### Bảng Liên Kết

1. **`session_chapters`** - Liên kết buổi học với chương học

   - `id` (PK)
   - `session_id` (FK → class_sessions)
   - `chapter_id` (FK → course_chapters)

2. **`session_lessons`** - Liên kết buổi học với bài học
   - `id` (PK)
   - `session_id` (FK → class_sessions)
   - `lesson_id` (FK → course_lessons)

## Cách Chạy Migration

### Bước 1: Chạy file batch

```bash
cd edu360-management-system
RUN_MIGRATION_LESSON_CONTENT.bat
```

### Bước 2: Nhập thông tin

```
Enter MySQL username [root]: root
Enter MySQL password: your_password
```

### Bước 3: Kiểm tra

```sql
USE edu360_system;

-- Xem cấu trúc bảng
DESC class_sessions;

-- Kiểm tra dữ liệu
SELECT id, date, lesson_content
FROM class_sessions
WHERE lesson_content IS NOT NULL;
```

## Backend API

### Endpoint: Lưu Nội Dung Buổi Học

**POST** `/api/sessions/by-class-date`

**Params:**

- `classId` (Long) - ID của lớp học
- `date` (String) - Ngày học (format: YYYY-MM-DD)

**Request Body:**

```json
{
  "chapterIds": [1, 2],
  "lessonIds": [3, 4, 5],
  "content": "Nội dung buổi học hôm nay..."
}
```

**Response:**

```json
"Updated session content"
```

### Endpoint: Lấy Nội Dung Buổi Học

**GET** `/api/sessions/{sessionId}/content`

**Response:**

```json
{
  "sessionId": 123,
  "classId": 45,
  "className": "Java Basic",
  "subjectName": "Java",
  "courseTitle": "Java Programming",
  "content": "Nội dung buổi học...",
  "chapters": [
    {
      "id": 1,
      "title": "Chương 1",
      "lessons": [...]
    }
  ]
}
```

## Frontend Implementation

### Service: `session.service.js`

```javascript
import sessionService from "../../services/class/session.service";

// Save lesson content
await sessionService.saveSessionContent({
  classId: 123,
  date: "2024-01-15",
  chapterIds: [1],
  lessonIds: [3],
  content: "Nội dung buổi học...",
});
```

### Component: `ClassDetail.jsx`

**State Management:**

```javascript
const [courseData, setCourseData] = useState(null);
const [selectedChapterId, setSelectedChapterId] = useState("");
const [selectedLessonId, setSelectedLessonId] = useState("");
const [lessonContent, setLessonContent] = useState("");
const [savingContent, setSavingContent] = useState(false);
```

**UI Flow:**

1. Hiển thị thông tin course
2. Select chương học (dropdown)
3. Select bài học (filtered theo chương)
4. Textarea nhập nội dung
5. Button "Lưu Nội Dung"

## Validation Rules

### Frontend

- ✅ Chọn chương học (required)
- ✅ Chọn bài học (required)
- ✅ Nhập nội dung (required, min 1 char after trim)

### Backend

- ✅ Session phải tồn tại cho classId + date
- ✅ User phải là giáo viên của lớp đó
- ✅ Chapter và Lesson phải thuộc course của lớp
- ✅ Content được trim trước khi lưu

## Testing Checklist

### 1. Database Migration

- [ ] Chạy migration script thành công
- [ ] Column `lesson_content` xuất hiện trong bảng
- [ ] Column có type TEXT và nullable

### 2. Backend

- [ ] Restart Spring Boot server
- [ ] POST /api/sessions/by-class-date response 200
- [ ] Data được lưu vào database
- [ ] GET /api/sessions/{id}/content trả về content

### 3. Frontend

- [ ] UI hiển thị đầy đủ (course info, dropdowns, textarea)
- [ ] Dropdown chương học load đúng
- [ ] Dropdown bài học filter theo chương
- [ ] Validation hiển thị khi thiếu data
- [ ] Toast success khi lưu thành công
- [ ] Console không có error

### 4. Integration Test

```bash
# Step 1: Mở ClassDetail page
# Step 2: Chọn chương học
# Step 3: Chọn bài học
# Step 4: Nhập nội dung: "Test content for integration"
# Step 5: Click "Lưu Nội Dung"
# Step 6: Check toast success
# Step 7: Query database:
SELECT * FROM class_sessions
WHERE lesson_content LIKE '%Test content%';
```

## Troubleshooting

### Lỗi: "Session not found for class X on YYYY-MM-DD"

**Nguyên nhân:** Chưa có session record cho ngày đó

**Giải pháp:** Session được tạo khi:

- Admin tạo lịch cho lớp
- Giáo viên điểm danh lần đầu

Cần điểm danh trước khi lưu nội dung.

### Lỗi: "Cannot find symbol: method getLessonContent()"

**Nguyên nhân:** Chưa rebuild backend sau khi thêm field

**Giải pháp:**

```bash
cd edu360-management-system
mvnw clean compile
```

### Lỗi: Network error 404

**Nguyên nhân:** Backend chưa restart

**Giải pháp:**

```bash
mvnw spring-boot:run
```

### Content lưu rỗng trong DB

**Nguyên nhân:** Frontend không trim() hoặc gửi string rỗng

**Giải pháp:** Backend đã có check:

```java
if (req.getContent() != null && !req.getContent().trim().isEmpty()) {
    session.setLessonContent(req.getContent().trim());
}
```

## Data Flow

```
Frontend (ClassDetail.jsx)
    ↓ Select chapter, lesson, write content
    ↓ Click "Lưu Nội Dung"
    ↓
sessionService.saveSessionContent()
    ↓ POST /api/sessions/by-class-date
    ↓ {classId, date, chapterIds, lessonIds, content}
    ↓
SessionContentController.upsertSessionContentByClassDate()
    ↓ Extract params and body
    ↓
SessionContentService.upsertSessionContentByClassDate()
    ↓ Find session by classId + date
    ↓ Call upsertSessionContent()
    ↓
SessionContentService.upsertSessionContent()
    ↓ Validate teacher ownership
    ↓ Delete old session_chapters links
    ↓ Delete old session_lessons links
    ↓ Create new session_chapters links
    ↓ Create new session_lessons links
    ↓ Save lesson_content to session
    ↓
classSessionRepository.save(session)
    ↓ UPDATE class_sessions SET lesson_content = ?
    ↓
Database (class_sessions table)
    ✅ Data persisted
```

## Notes

- Content field là TEXT type → có thể lưu nội dung dài (64KB)
- Content nullable → không bắt buộc phải nhập
- Một session có thể link nhiều chapter và lesson (many-to-many)
- Content được ghi đè mỗi lần save (không append)
- Chỉ giáo viên của lớp mới được phép lưu content

## Related Files

**Backend:**

- `ClassSession.java` - Entity with lessonContent field
- `SessionContentService.java` - Business logic
- `SessionContentController.java` - REST API
- `SessionContentUpsertRequest.java` - DTO
- `SessionContentResponse.java` - DTO

**Frontend:**

- `ClassDetail.jsx` - UI component
- `session.service.js` - API service

**Database:**

- `MIGRATION_ADD_LESSON_CONTENT.sql` - Migration script
- `RUN_MIGRATION_LESSON_CONTENT.bat` - Migration runner
