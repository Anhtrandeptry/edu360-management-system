# Hướng dẫn cấu hình Thanh toán tự động (PayOS/Casso)

## 📋 Tổng quan

Hệ thống hỗ trợ 2 cách nhận webhook thanh toán:
1. **PayOS** (khuyên dùng) - Webhook từ my.payos.vn
2. **Casso** - Webhook từ my.casso.vn

Khi học sinh chuyển tiền, hệ thống sẽ tự động:
1. ✅ Xác nhận thanh toán (Payment PAID)
2. ✅ Enroll học sinh vào lớp học
3. ✅ Gửi thông báo cho học sinh

---

## 🚀 Cách 1: PayOS (Khuyên dùng)

### Bước 1: Lấy thông tin từ PayOS

Bạn đã có thông tin sau từ https://my.payos.vn:
- **Client ID**: `1f34c1ae-a32e-43c5-a81f-b6d7aed32799`
- **API Key**: `10c0a596-02bc-4513-99a2-fee3192b9435`
- **Checksum Key**: `31d9f9c5ff7cdd0d58ff4e8710d7d04b0ccd1390df`

### Bước 2: Cấu hình biến môi trường

Thêm vào file `.env` hoặc biến môi trường:

```properties
PAYOS_CLIENT_ID=1f34c1ae-a32e-43c5-a81f-b6d7aed32799
PAYOS_API_KEY=10c0a596-02bc-4513-99a2-fee3192b9435
PAYOS_CHECKSUM_KEY=31d9f9c5ff7cdd0d58ff4e8710d7d04b0ccd1390df
```

### Bước 3: Cấu hình Webhook URL trên PayOS

1. Vào https://my.payos.vn
2. Chọn **Kênh thanh toán** → Chọn kênh vừa tạo
3. Tìm mục **Webhook URL** và điền:

```
https://your-domain.com/api/payments/payos/webhook
```

> ⚠️ **Lưu ý**: URL phải là HTTPS và accessible từ internet

### Endpoint PayOS Webhook

```
POST /api/payments/payos/webhook
```

---

## 🔧 Cách 2: Casso (Thay thế)

1. Truy cập: https://my.casso.vn
2. Đăng ký tài khoản với email
3. Liên kết ngân hàng (BIDV, Vietcombank, Techcombank, v.v.)

## 🔗 Bước 2: Cấu hình Webhook

1. Đăng nhập vào https://my.casso.vn
2. Vào menu **Thiết lập** → **Webhook**
3. Nhấn **Thêm Webhook** hoặc **Tạo mới**
4. Điền thông tin:

| Trường | Giá trị |
|--------|---------|
| **Webhook URL** | `https://your-domain.com/api/payments/casso/webhook` |
| **Secure Token** | Tự generate hoặc tạo token bảo mật |
| **Income** | ✅ Bật (Chỉ nhận thông báo tiền vào) |

5. Lưu lại và copy **Secure Token**

## ⚙️ Bước 3: Cấu hình Server

Thêm biến môi trường hoặc cập nhật `.env`:

```properties
# Casso Webhook Secret Token
CASSO_WEBHOOK_SECRET_TOKEN=your_secure_token_here
```

## 📝 Bước 4: Format nội dung chuyển khoản

Khi học sinh thanh toán, hệ thống sẽ generate QR code với nội dung có format:
```
[Tên học sinh] thanh toan hoc phi #PAY-[classId]-[studentId]-[timestamp]
```

Ví dụ:
```
Nguyen Van A thanh toan hoc phi #PAY-123-456-1703123456789
```

Hệ thống sẽ tìm `#PAY-...` để xác định payment tương ứng.

## 🔄 Luồng tự động

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Học sinh  │ --> │  Ngân hàng  │ --> │    Casso    │
│  Quét QR    │     │  Nhận tiền  │     │  Detect TX  │
└─────────────┘     └─────────────┘     └──────┬──────┘
                                               │
                                               v
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Học sinh  │ <-- │   Server    │ <-- │   Webhook   │
│  Được enroll│     │  Xử lý TX   │     │  POST data  │
└─────────────┘     └─────────────┘     └─────────────┘
```

1. **Học sinh quét QR** và chuyển tiền với nội dung có mã order
2. **Ngân hàng** nhận tiền và gửi thông báo cho Casso
3. **Casso** gửi webhook đến server với thông tin giao dịch
4. **Server** nhận webhook, tìm payment theo orderCode
5. **Server** verify số tiền và cập nhật trạng thái PAID
6. **Server** tự động enroll học sinh vào lớp
7. **Server** gửi thông báo cho học sinh

## 📊 Webhook Payload Example

```json
{
  "error": 0,
  "data": [
    {
      "id": 123456789,
      "when": "2024-01-15 14:30:00",
      "amount": 500000,
      "description": "Nguyen Van A thanh toan hoc phi #PAY-123-456-1703123456789",
      "cusum_balance": 10500000,
      "tid": "FT24015123456",
      "bank_sub_acc_id": "1234567890"
    }
  ]
}
```

## ⚠️ Lưu ý quan trọng

1. **Số tiền phải khớp**: Nếu học sinh chuyển sai số tiền, payment sẽ bị đánh dấu FAILED
2. **Nội dung phải đúng**: Học sinh phải giữ nguyên nội dung chuyển khoản (có mã #PAY-...)
3. **Secure Token**: Luôn sử dụng Secure Token để bảo mật webhook
4. **HTTPS**: Webhook URL phải là HTTPS trong production

## 🧪 Test Webhook

Có thể test webhook bằng cách gửi POST request:

```bash
curl -X POST https://your-domain.com/api/payments/casso/webhook \
  -H "Content-Type: application/json" \
  -H "Authorization: Apikey your_secure_token" \
  -d '{
    "error": 0,
    "data": [{
      "id": 1,
      "amount": 500000,
      "description": "#PAY-123-456-1703123456789",
      "tid": "TEST123"
    }]
  }'
```

## 📱 Gói dịch vụ Casso

- **FREE-100**: 100 giao dịch/tháng (miễn phí)
- **Gói trả phí**: Không giới hạn giao dịch

Xem chi tiết tại: https://casso.vn/bang-gia/
