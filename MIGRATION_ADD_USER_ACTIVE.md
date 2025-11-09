# Migration: Add active column to users table

## SQL Script

```sql
-- Thêm cột active vào bảng users
ALTER TABLE users ADD COLUMN active TINYINT(1) NOT NULL DEFAULT 1;

-- Đặt tất cả user hiện tại thành active
UPDATE users SET active = 1 WHERE active IS NULL;

-- Tạo index cho cột active (tùy chọn, tối ưu query filter)
CREATE INDEX idx_users_active ON users(active);
```

## Rollback (nếu cần)

```sql
-- Xóa index
DROP INDEX idx_users_active ON users;

-- Xóa cột
ALTER TABLE users DROP COLUMN active;
```

## Verification

Sau khi chạy migration, kiểm tra:

```sql
-- Xem cấu trúc bảng
DESCRIBE users;

-- Kiểm tra dữ liệu
SELECT id, username, email, active FROM users LIMIT 10;
```

## Notes

- Cột `active` kiểu `TINYINT(1)` tương đương `BOOLEAN` trong MySQL
- Default value `1` (true) đảm bảo user mới được tạo sẽ active
- Backend code đã cập nhật:
  - Entity `User.java`: thêm field `active`
  - DTO `UserResponse.java`: thêm field `active`
  - Mapper `UserMapper.java`: map field `active`
  - Service `UserService.java`: thêm method `updateUserStatus()`
  - Controller `UserController.java`: thêm endpoint `PATCH /api/users/{id}/status`
