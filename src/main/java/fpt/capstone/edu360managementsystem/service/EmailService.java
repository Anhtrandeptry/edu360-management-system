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

    // Gửi mail đơn giản
    public void sendSimpleMessage(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("crush1003ahihi@gmail.com");
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            emailSender.send(message);
            System.out.println("✅ Email sent successfully to: " + to);
        } catch (MailException e) {
            System.err.println("❌ Failed to send email to: " + to);
            e.printStackTrace();
            throw e;
        }
    }

    // Gửi mail HTML
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
}
