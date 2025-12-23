package fpt.capstone.edu360managementsystem.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender emailSender;

    // Send simple email
    public void sendSimpleMessage(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("crush1003ahihi@gmail.com");
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            emailSender.send(message);
            System.out.println("Email sent successfully to: " + to);
        } catch (MailException e) {
            System.err.println("Failed to send email to: " + to);
            e.printStackTrace();
            throw e;
        }
    }

    // Send HTML mail
    public void sendHtmlMessage(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom("crush1003ahihi@gmail.com");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = isHtml
            emailSender.send(message);
            System.out.println("✅ HTML Email sent successfully to: " + to);
        } catch (MessagingException e) {
            System.err.println("❌ Failed to send HTML email to: " + to);
            e.printStackTrace();
            throw new RuntimeException("Failed to send email", e);
        }
    }

    /**
     * Gửi email xác nhận thanh toán thành công đến học sinh và phụ huynh
     *
     * @param studentEmail Email của học sinh
     * @param parentEmail Email của phụ huynh (có thể null)
     * @param studentName Tên học sinh
     * @param className Tên lớp học
     * @param amount Số tiền đã thanh toán
     */
    public void sendPaymentConfirmationEmail(String studentEmail, String parentEmail,
            String studentName, String className, Long amount) {
        String subject = "✅ Xác nhận thanh toán thành công - 360EDU";

        // Format số tiền với dấu phẩy ngăn cách hàng nghìn
        String formattedAmount = String.format("%,d", amount);

        String htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }
                    .content { background: #f9f9f9; padding: 30px; border: 1px solid #ddd; border-top: none; border-radius: 0 0 10px 10px; }
                    .success-icon { font-size: 48px; margin-bottom: 10px; }
                    .info-box { background: white; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #28a745; }
                    .info-row { display: flex; justify-content: space-between; padding: 10px 0; border-bottom: 1px solid #eee; }
                    .info-row:last-child { border-bottom: none; }
                    .label { color: #666; font-weight: 500; }
                    .value { color: #333; font-weight: 600; }
                    .amount { color: #28a745; font-size: 24px; font-weight: bold; }
                    .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <div class="success-icon">✅</div>
                        <h1>Thanh toán thành công!</h1>
                    </div>
                    <div class="content">
                        <p>Xin chào,</p>
                        <p>Chúng tôi xác nhận đã nhận được thanh toán học phí của bạn. Dưới đây là chi tiết giao dịch:</p>
                        
                        <div class="info-box">
                            <div class="info-row">
                                <span class="label">👤 Học sinh:</span>
                                <span class="value">%s</span>
                            </div>
                            <div class="info-row">
                                <span class="label">📚 Lớp học:</span>
                                <span class="value">%s</span>
                            </div>
                            <div class="info-row">
                                <span class="label">💰 Số tiền:</span>
                                <span class="amount">%s VNĐ</span>
                            </div>
                        </div>
                        
                        <p>Học sinh đã được tự động đăng ký vào lớp học. Vui lòng đăng nhập vào hệ thống để xem lịch học chi tiết.</p>
                        
                        <p>Trân trọng,<br><strong>Đội ngũ 360EDU</strong></p>
                    </div>
                    <div class="footer">
                        <p>Email này được gửi tự động từ hệ thống 360EDU. Vui lòng không trả lời email này.</p>
                        <p>© 2025 360EDU - Hệ thống quản lý giáo dục</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(studentName, className, formattedAmount);

        // Gửi email cho học sinh
        if (studentEmail != null && !studentEmail.isBlank()) {
            try {
                sendHtmlMessage(studentEmail, subject, htmlContent);
                System.out.println("✅ Payment confirmation email sent to student: " + studentEmail);
            } catch (Exception e) {
                System.err.println("❌ Failed to send payment email to student: " + e.getMessage());
            }
        }

        // Gửi email cho phụ huynh
        if (parentEmail != null && !parentEmail.isBlank()) {
            try {
                sendHtmlMessage(parentEmail, subject, htmlContent);
                System.out.println("✅ Payment confirmation email sent to parent: " + parentEmail);
            } catch (Exception e) {
                System.err.println("❌ Failed to send payment email to parent: " + e.getMessage());
            }
        }
    }
}
