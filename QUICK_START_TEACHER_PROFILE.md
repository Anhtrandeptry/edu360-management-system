# 🚀 Quick Start - Teacher Profile Multi-Table

## Bước 1: Chạy Migration SQL

1. Mở **MySQL Workbench**
2. Connect đến database `edu360_db`
3. Mở file `MIGRATION_TEACHER_PROFILE_TO_MULTI_TABLE.sql`
4. Chạy toàn bộ script (Ctrl + Shift + Enter)

**Kết quả mong đợi:**
```
✅ Created table: teacher_certificates
✅ Created table: teacher_experience  
✅ Created table: teacher_education
✅ Dropped column: certificates (JSON)
✅ Dropped column: experience (JSON)
✅ Dropped column: education (JSON)
✅ Added column: years_of_experience (if not exists)
✅ Added column: rating (if not exists)
✅ Added column: achievements (if not exists)
```

---

## Bước 2: Start Backend

```powershell
cd e:\Semester9\360FullStack_ver2\edu360-management-system
.\mvnw.cmd spring-boot:run
```

Kiểm tra console không có lỗi:
- ✅ Hibernate mapping thành công cho 3 entities mới
- ✅ Server chạy tại `http://localhost:8080`

---

## Bước 3: Test APIs với Postman

### A. Login as Admin

```
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
```

**Lưu cookie `JSESSIONID` từ response!**

---

### B. Thêm Certificate cho Teacher

```
POST http://localhost:8080/api/admin/teachers/1/certificates
Cookie: JSESSIONID=<your-session-id>
Content-Type: application/json

{
  "title": "AWS Solutions Architect Professional",
  "organization": "Amazon Web Services",
  "year": 2023,
  "description": "Professional level cloud architecture certification"
}
```

**Response:**
```json
{
  "id": 1,
  "title": "AWS Solutions Architect Professional",
  "organization": "Amazon Web Services",
  "year": 2023,
  "description": "Professional level cloud architecture certification"
}
```

---

### C. Thêm Experience

```
POST http://localhost:8080/api/admin/teachers/1/experiences
Cookie: JSESSIONID=<your-session-id>
Content-Type: application/json

{
  "position": "Senior Java Developer",
  "company": "FPT Software",
  "startYear": 2018,
  "endYear": 2022,
  "description": "Led development team for enterprise projects"
}
```

---

### D. Thêm Education

```
POST http://localhost:8080/api/admin/teachers/1/educations
Cookie: JSESSIONID=<your-session-id>
Content-Type: application/json

{
  "degree": "Master of Computer Science",
  "school": "FPT University",
  "year": 2018,
  "description": "Specialized in Software Engineering, GPA: 3.8/4.0"
}
```

---

### E. Lấy tất cả data của Teacher

Login as Teacher first:
```
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "teacher1",
  "password": "teacher123"
}
```

Get profile:
```
GET http://localhost:8080/api/teachers/profile
Cookie: JSESSIONID=<teacher-session-id>
```

**Response sẽ bao gồm:**
```json
{
  "id": 1,
  "userId": 3,
  "username": "teacher1",
  "fullName": "Nguyễn Văn A",
  "email": "teacher1@edu360.com",
  ...
  "yearsOfExperience": 5,
  "rating": 4.5,
  "achievements": null,
  "certificates": [
    {
      "id": 1,
      "title": "AWS Solutions Architect Professional",
      "organization": "Amazon Web Services",
      "year": 2023,
      "description": "Professional level cloud architecture certification"
    }
  ],
  "experiences": [
    {
      "id": 1,
      "position": "Senior Java Developer",
      "company": "FPT Software",
      "startYear": 2018,
      "endYear": 2022,
      "description": "Led development team for enterprise projects"
    }
  ],
  "educations": [
    {
      "id": 1,
      "degree": "Master of Computer Science",
      "school": "FPT University",
      "year": 2018,
      "description": "Specialized in Software Engineering, GPA: 3.8/4.0"
    }
  ]
}
```

---

## 📋 Postman Collection (Import vào Postman)

```json
{
  "info": {
    "name": "Teacher Profile Multi-Table APIs",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Admin - Add Certificate",
      "request": {
        "method": "POST",
        "url": "http://localhost:8080/api/admin/teachers/1/certificates",
        "body": {
          "mode": "raw",
          "raw": "{\n  \"title\": \"AWS Solutions Architect\",\n  \"organization\": \"Amazon\",\n  \"year\": 2023,\n  \"description\": \"Professional certification\"\n}",
          "options": { "raw": { "language": "json" } }
        }
      }
    },
    {
      "name": "Admin - Get Certificates",
      "request": {
        "method": "GET",
        "url": "http://localhost:8080/api/admin/teachers/1/certificates"
      }
    },
    {
      "name": "Admin - Add Experience",
      "request": {
        "method": "POST",
        "url": "http://localhost:8080/api/admin/teachers/1/experiences",
        "body": {
          "mode": "raw",
          "raw": "{\n  \"position\": \"Senior Dev\",\n  \"company\": \"FPT\",\n  \"startYear\": 2018,\n  \"endYear\": 2022,\n  \"description\": \"Led team\"\n}",
          "options": { "raw": { "language": "json" } }
        }
      }
    },
    {
      "name": "Teacher - Get My Profile",
      "request": {
        "method": "GET",
        "url": "http://localhost:8080/api/teachers/profile"
      }
    }
  ]
}
```

---

## ✅ Verification Checklist

- [ ] Migration SQL chạy thành công
- [ ] 3 bảng mới đã được tạo
- [ ] Backend start không lỗi
- [ ] POST certificate thành công
- [ ] POST experience thành công
- [ ] POST education thành công
- [ ] GET teacher profile trả về đầy đủ data
- [ ] PUT update certificate thành công
- [ ] DELETE certificate thành công

---

## 🔍 Troubleshooting

### Lỗi: "Table already exists"
→ Bỏ qua, migration script có `IF NOT EXISTS`

### Lỗi: "Foreign key constraint fails"
→ Đảm bảo `teacherId` tồn tại trong bảng `teachers`

### Lỗi 403 Forbidden
→ Kiểm tra cookie `JSESSIONID` và role `ADMIN`

### Backend không start
→ Kiểm tra database connection trong `application.properties`

---

## 🎯 Next: Tạo UI Admin

Sau khi test APIs thành công, tạo UI Admin để quản lý:
1. Form thêm/sửa Certificate
2. Form thêm/sửa Experience
3. Form thêm/sửa Education
4. Table hiển thị danh sách với actions (Edit/Delete)

Tham khảo API endpoints ở file `TEACHER_PROFILE_MULTI_TABLE_GUIDE.md`
