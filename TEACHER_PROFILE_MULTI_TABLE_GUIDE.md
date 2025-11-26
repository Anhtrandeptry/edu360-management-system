# Teacher Profile - Multi-Table Implementation Guide

## 🎯 Tổng quan

Đã chuyển từ **JSON trong 1 bảng** sang **nhiều bảng riêng biệt** để quản lý teacher profile.

## 📊 Cấu trúc Database

### Bảng chính: `teachers`
```sql
- id (PK)
- user_id
- subject_id
- specialization
- degree
- note
- workplace
- avatar_url
- linkedin_url
- facebook_url
- bio
- years_of_experience  -- NEW
- rating               -- NEW
- achievements         -- NEW
```

### Bảng phụ (mới tạo):

#### 1. `teacher_certificates` - Chứng chỉ
```sql
- id (PK)
- teacher_id (FK -> teachers.id)
- title
- organization
- year
- description
```

#### 2. `teacher_experience` - Kinh nghiệm làm việc
```sql
- id (PK)
- teacher_id (FK -> teachers.id)
- position
- company
- start_year
- end_year
- description
```

#### 3. `teacher_education` - Học vấn
```sql
- id (PK)
- teacher_id (FK -> teachers.id)
- degree
- school
- year
- description
```

---

## 🚀 Các bước thực hiện

### 1. Chạy Migration SQL
```bash
# Chạy file này trong MySQL Workbench
MIGRATION_TEACHER_PROFILE_TO_MULTI_TABLE.sql
```

Script sẽ:
- ✅ Tạo 3 bảng mới: `teacher_certificates`, `teacher_experience`, `teacher_education`
- ✅ Xóa các cột JSON: `certificates`, `experience`, `education`
- ✅ Thêm các cột mới nếu chưa có: `years_of_experience`, `rating`, `achievements`

### 2. Start Backend
```bash
cd edu360-management-system
./mvnw spring-boot:run
```

Hibernate sẽ tự động nhận diện entities mới.

---

## 📡 API Endpoints cho Admin

### Certificates (Chứng chỉ)

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/api/admin/teachers/{teacherId}/certificates` | Lấy tất cả certificates |
| POST | `/api/admin/teachers/{teacherId}/certificates` | Thêm certificate mới |
| PUT | `/api/admin/teachers/{teacherId}/certificates/{certId}` | Cập nhật certificate |
| DELETE | `/api/admin/teachers/{teacherId}/certificates/{certId}` | Xóa certificate |

**Request body (POST/PUT):**
```json
{
  "title": "AWS Certified Solutions Architect",
  "organization": "Amazon",
  "year": 2023,
  "description": "Professional level certification"
}
```

### Experiences (Kinh nghiệm)

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/api/admin/teachers/{teacherId}/experiences` | Lấy tất cả experiences |
| POST | `/api/admin/teachers/{teacherId}/experiences` | Thêm experience mới |
| PUT | `/api/admin/teachers/{teacherId}/experiences/{expId}` | Cập nhật experience |
| DELETE | `/api/admin/teachers/{teacherId}/experiences/{expId}` | Xóa experience |

**Request body (POST/PUT):**
```json
{
  "position": "Senior Java Developer",
  "company": "FPT Software",
  "startYear": 2018,
  "endYear": 2022,
  "description": "Led a team of 5 developers"
}
```

### Educations (Học vấn)

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/api/admin/teachers/{teacherId}/educations` | Lấy tất cả educations |
| POST | `/api/admin/teachers/{teacherId}/educations` | Thêm education mới |
| PUT | `/api/admin/teachers/{teacherId}/educations/{eduId}` | Cập nhật education |
| DELETE | `/api/admin/teachers/{teacherId}/educations/{eduId}` | Xóa education |

**Request body (POST/PUT):**
```json
{
  "degree": "Master of Computer Science",
  "school": "FPT University",
  "year": 2018,
  "description": "GPA: 3.8/4.0"
}
```

---

## 🎨 Ví dụ sử dụng với Fetch API (Frontend)

### 1. Lấy danh sách certificates
```javascript
const response = await fetch(`/api/admin/teachers/${teacherId}/certificates`, {
  credentials: 'include'
});
const certificates = await response.json();
```

### 2. Thêm certificate mới
```javascript
const response = await fetch(`/api/admin/teachers/${teacherId}/certificates`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  credentials: 'include',
  body: JSON.stringify({
    title: "Oracle Java Certification",
    organization: "Oracle",
    year: 2022,
    description: "Java SE 11 Developer"
  })
});
const newCert = await response.json();
```

### 3. Cập nhật certificate
```javascript
const response = await fetch(`/api/admin/teachers/${teacherId}/certificates/${certId}`, {
  method: 'PUT',
  headers: { 'Content-Type': 'application/json' },
  credentials: 'include',
  body: JSON.stringify({
    title: "Oracle Java Certification - Updated",
    organization: "Oracle",
    year: 2023,
    description: "Updated description"
  })
});
```

### 4. Xóa certificate
```javascript
await fetch(`/api/admin/teachers/${teacherId}/certificates/${certId}`, {
  method: 'DELETE',
  credentials: 'include'
});
```

---

## 📝 Files đã tạo/sửa

### Backend - Entities
- ✅ `TeacherCertificate.java` - Entity cho certificates
- ✅ `TeacherExperience.java` - Entity cho experiences  
- ✅ `TeacherEducation.java` - Entity cho educations
- ✅ `Teacher.java` - Đã cập nhật với relationships

### Backend - Repositories
- ✅ `TeacherCertificateRepository.java`
- ✅ `TeacherExperienceRepository.java`
- ✅ `TeacherEducationRepository.java`

### Backend - DTOs
- ✅ `TeacherCertificateRequest.java`
- ✅ `TeacherExperienceRequest.java`
- ✅ `TeacherEducationRequest.java`
- ✅ `TeacherProfileUpdateRequest.java` - Đã cập nhật
- ✅ `TeacherProfileResponse.java` - Đã cập nhật

### Backend - Services
- ✅ `TeacherService.java` - Đã refactor để dùng bảng riêng

### Backend - Controllers
- ✅ `AdminTeacherProfileController.java` - Controller mới cho admin

### Database
- ✅ `MIGRATION_TEACHER_PROFILE_TO_MULTI_TABLE.sql` - Migration script

---

## 🔧 Testing với Postman

### 1. Login as Admin
```
POST /api/auth/login
Body: { "username": "admin", "password": "admin123" }
```

### 2. Thêm Certificate cho Teacher (teacherId = 1)
```
POST /api/admin/teachers/1/certificates
Headers: Cookie: JSESSIONID=...
Body:
{
  "title": "AWS Solutions Architect",
  "organization": "Amazon Web Services",
  "year": 2023,
  "description": "Professional certification"
}
```

### 3. Lấy Teacher Profile (với certificates, experiences, educations)
```
GET /api/teachers/profile
Headers: Cookie: JSESSIONID=... (teacher login)
```

---

## ✅ Checklist

- [x] Tạo 3 entities mới
- [x] Tạo 3 repositories
- [x] Tạo DTOs
- [x] Update Teacher entity với @OneToMany
- [x] Refactor TeacherService
- [x] Tạo AdminTeacherProfileController
- [x] Tạo migration SQL
- [ ] Test APIs với Postman
- [ ] Tạo UI Admin để quản lý
- [ ] Tạo UI Guest để xem

---

## 🎯 Next Steps

1. **Chạy migration SQL** trong MySQL Workbench
2. **Start backend** và kiểm tra không có lỗi
3. **Test APIs** với Postman
4. **Tạo UI Admin** để CRUD certificates/experiences/educations
5. **Tạo UI Guest** để hiển thị teacher profile

## ⚠️ Lưu ý

- Tất cả endpoints admin yêu cầu role `ADMIN`
- DELETE operations có cascade, xóa teacher sẽ xóa hết certificates/experiences/educations
- Foreign key constraint đảm bảo data integrity
- Index được tạo trên `teacher_id` để query nhanh hơn
