-- Migration: Create News table
-- Purpose: Store news/articles with support for draft/published status, views tracking, and tags

CREATE TABLE IF NOT EXISTS news (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    excerpt TEXT,
    content TEXT NOT NULL,
    image_url VARCHAR(255),
    author VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    views INT NOT NULL DEFAULT 0,
    tags VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    published_at DATETIME,
    
    INDEX idx_status (status),
    INDEX idx_published_at (published_at),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insert sample news data
INSERT INTO news (title, excerpt, content, author, status, views, tags, published_at) VALUES
('Khai giảng khóa học lập trình Python 2024', 
 'Chào mừng các bạn đến với khóa học Python cơ bản đến nâng cao. Khóa học bắt đầu từ 15/12/2024.', 
 '<p>Trung tâm Edu360 hân hạnh thông báo khai giảng khóa học <strong>Lập trình Python</strong> dành cho người mới bắt đầu.</p><h3>Nội dung khóa học:</h3><ul><li>Python cơ bản</li><li>Lập trình hướng đối tượng</li><li>Làm việc với database</li><li>Xây dựng web với Django</li></ul><h3>Thời gian học:</h3><p>Thứ 2, 4, 6 - 19:00 đến 21:00</p><p>Khai giảng: 15/12/2024</p>',
 'Admin',
 'PUBLISHED',
 125,
 'Python,Lập trình,Khóa học',
 '2024-11-15 10:00:00'),

('Thông báo nghỉ lễ 30/4 - 1/5', 
 'Trung tâm thông báo lịch nghỉ lễ Giải phóng miền Nam và Quốc tế Lao động 2024.', 
 '<p>Kính gửi quý phụ huynh và học viên,</p><p>Trung tâm Edu360 xin thông báo lịch nghỉ lễ như sau:</p><ul><li><strong>Nghỉ từ:</strong> 30/4/2024 (Thứ Ba)</li><li><strong>Đến:</strong> 1/5/2024 (Thứ Tư)</li><li><strong>Học lại:</strong> 2/5/2024 (Thứ Năm)</li></ul><p>Mọi thắc mắc xin liên hệ hotline: <strong>1900.xxx.xxx</strong></p>',
 'Admin',
 'PUBLISHED',
 89,
 'Thông báo,Lịch nghỉ',
 '2024-11-10 14:30:00'),

('Học bổng 50% cho học viên xuất sắc', 
 'Chương trình trao học bổng cho học viên có thành tích học tập xuất sắc trong học kỳ vừa qua.', 
 '<h2>Chương trình học bổng Edu360</h2><p>Nhằm khuyến khích tinh thần học tập, Edu360 triển khai chương trình học bổng 50% học phí cho học kỳ tiếp theo.</p><h3>Điều kiện nhận học bổng:</h3><ol><li>Điểm trung bình >= 8.5/10</li><li>Không vi phạm nội quy</li><li>Tham gia đầy đủ các buổi học (>= 90%)</li></ol><h3>Thời gian đăng ký:</h3><p>Từ ngày 20/11/2024 đến 30/11/2024</p>',
 'Admin',
 'PUBLISHED',
 234,
 'Học bổng,Ưu đãi,Thông báo',
 '2024-11-18 09:00:00'),

('Khai trương cơ sở 2 tại Quận 7', 
 'Edu360 mở rộng quy mô với cơ sở mới tại Quận 7, TP.HCM. Ưu đãi đặc biệt cho học viên đăng ký sớm.', 
 '<p>Sau 5 năm hoạt động, Edu360 tự hào thông báo khai trương cơ sở 2 tại <strong>Quận 7, TP.HCM</strong>.</p><h3>Thông tin cơ sở mới:</h3><ul><li><strong>Địa chỉ:</strong> 123 Nguyễn Văn Linh, Quận 7</li><li><strong>Diện tích:</strong> 500m² với 10 phòng học hiện đại</li><li><strong>Trang thiết bị:</strong> Máy chiếu 4K, điều hòa, bàn ghế ergonomic</li></ul><h3>Ưu đãi khai trương:</h3><p>🎁 Giảm 30% học phí cho 100 học viên đăng ký sớm nhất<br/>🎁 Tặng combo học liệu trị giá 500.000đ<br/>🎁 Miễn phí tháng đầu tiên</p><p><strong>Hạn đăng ký:</strong> 30/12/2024</p>',
 'Admin',
 'PUBLISHED',
 567,
 'Khai trương,Cơ sở mới,Ưu đãi',
 '2024-11-20 08:00:00'),

('Hướng dẫn đăng ký học online qua Google Meet', 
 'Chi tiết các bước đăng ký và tham gia lớp học online tại Edu360.', 
 '<h2>Hướng dẫn học online</h2><p>Edu360 cung cấp hình thức học online qua Google Meet với chất lượng tương đương lớp offline.</p><h3>Bước 1: Đăng ký tài khoản</h3><p>Truy cập website edu360.vn và đăng ký tài khoản học viên.</p><h3>Bước 2: Chọn lớp học</h3><p>Duyệt danh sách lớp học và chọn lớp phù hợp với lịch của bạn.</p><h3>Bước 3: Thanh toán</h3><p>Thanh toán học phí qua chuyển khoản hoặc ví điện tử.</p><h3>Bước 4: Nhận link Google Meet</h3><p>Sau khi thanh toán, bạn sẽ nhận link tham gia lớp qua email.</p><h3>Lưu ý:</h3><ul><li>Đảm bảo kết nối internet ổn định</li><li>Chuẩn bị camera và micro</li><li>Vào lớp trước 5 phút</li></ul>',
 'Support Team',
 'PUBLISHED',
 445,
 'Hướng dẫn,Online,Google Meet',
 '2024-11-22 16:00:00');
