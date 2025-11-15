# 🚀 HƯỚNG DẪN SỬA LỖI NHANH

## ❌ Lỗi hiện tại

```
Column 'semester_id' cannot be null
```

## ✅ Giải pháp (chọn 1 trong 3 cách)

---

### **Cách 1: MySQL Workbench (KHUYÊN DÙNG - DỄ NHẤT)**

1. Mở **MySQL Workbench**
2. Kết nối với database (user: `root`, password: `123456`)
3. Click **File** → **Open SQL Script**
4. Chọn file: `QUICK_FIX_SEMESTER.sql`
5. Click biểu tượng **⚡ Execute** (hoặc Ctrl+Shift+Enter)
6. Kiểm tra output tab, phải thấy message: "Migration completed!"

---

### **Cách 2: phpMyAdmin (nếu dùng XAMPP)**

1. Mở http://localhost/phpmyadmin
2. Chọn database `edu360_system` bên trái
3. Click tab **SQL** ở trên
4. Copy toàn bộ nội dung từ file `QUICK_FIX_SEMESTER.sql`
5. Paste vào ô SQL và click **Go**
6. Phải thấy message: "Migration completed!"

---

### **Cách 3: Command Line (nếu biết đường dẫn MySQL)**

Thay đổi đường dẫn MySQL bin theo máy bạn:

```cmd
cd d:\New360EDU-Ver3\edu360-management-system

REM Nếu MySQL 8.0 (mặc định)
"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p123456 edu360_system < QUICK_FIX_SEMESTER.sql

REM Hoặc nếu dùng XAMPP
"C:\xampp\mysql\bin\mysql.exe" -u root -p123456 edu360_system < QUICK_FIX_SEMESTER.sql
```

---

## 📝 Sau khi chạy SQL thành công

### **Bước 1: Kiểm tra trong MySQL**

Chạy lệnh này để xác nhận:

```sql
USE edu360_system;
DESCRIBE classes;
```

Phải thấy dòng:

```
semester_id | bigint | YES | MUL | NULL
```

### **Bước 2: Restart Spring Boot**

- Trong IntelliJ/Eclipse: Stop application (⏹️) rồi Run lại (▶️)
- Hoặc trong terminal:

```cmd
cd d:\New360EDU-Ver3\edu360-management-system
mvnw spring-boot:run
```

### **Bước 3: Test tạo lớp offline**

1. Mở form tạo lớp offline
2. Điền thông tin:
   - ✅ **Môn học**: Chọn môn bất kỳ
   - ✅ **Mô tả**: Nhập mô tả
   - ✅ **Sĩ số**: Nhập số (phải ≤ sức chứa phòng)
   - ✅ **Giáo viên**: Chọn giáo viên
   - ✅ **Tên lớp**: Tự động sinh
   - ✅ **Phòng học**: Chọn phòng
   - ✅ **Số buổi**: Nhập số buổi (vd: 15)
   - ✅ **Ngày bắt đầu**: Chọn ngày (phải >= hôm nay)
   - ✅ **Chọn lịch**: Pick các slot trong tuần
   - ✅ **Ngày kết thúc**: Tự động tính
3. Click **Tạo lớp**
4. Phải thấy toast thành công ✅

---

## ❗ Nếu vẫn lỗi

**Kiểm tra log Spring Boot:**

```
Error creating bean... Clazz.semester
```

→ Restart chưa đúng, phải stop hoàn toàn rồi run lại

**Vẫn báo "cannot be null":**

```sql
-- Kiểm tra constraint còn sót
SELECT * FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA = 'edu360_system'
  AND TABLE_NAME = 'classes'
  AND CONSTRAINT_TYPE = 'UNIQUE';
```

Nếu có constraint chứa semester_id → Drop nó:

```sql
ALTER TABLE classes DROP INDEX <constraint_name>;
```

---

## 📌 Tóm tắt

1. **Chạy QUICK_FIX_SEMESTER.sql** (bằng cách 1, 2 hoặc 3)
2. **Restart Spring Boot**
3. **Test tạo lớp offline**

File SQL đã sẵn sàng tại:

```
d:\New360EDU-Ver3\edu360-management-system\QUICK_FIX_SEMESTER.sql
```
