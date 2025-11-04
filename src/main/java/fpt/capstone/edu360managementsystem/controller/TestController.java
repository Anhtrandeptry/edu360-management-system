package fpt.capstone.edu360managementsystem.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import fpt.capstone.edu360managementsystem.service.EmailService;
import org.springframework.web.bind.annotation.*;


//for Angular Client (withCredentials)
//@CrossOrigin(origins = "http://localhost:8081", maxAge = 3600, allowCredentials="true")
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/test")
public class TestController {

  @Autowired
  private EmailService emailService;

  @GetMapping("/all")
  public String allAccess() {
    return "Public Content.";
  }

  @GetMapping("/user")
  @PreAuthorize("hasRole('STUDENT') or hasRole('TEACHER') or hasRole('ADMIN')")
  public String userAccess() {
    return "User Content.";
  }

  @GetMapping("/teach")
  @PreAuthorize("hasRole('TEACHER')")
  public String moderatorAccess() {
    return "Teacher Board.";
  }

  @GetMapping("/admin")
  @PreAuthorize("hasRole('ADMIN')")
  public String adminAccess() {
    return "Admin Board.";
  }

  @GetMapping("/parent")
  @PreAuthorize("hasRole('PARENT')")
  public String parentAccess() {return "PARENT Board.";}


  @GetMapping("/send")
  @PreAuthorize("hasRole('ADMIN')")
  public String sendTestEmail(@RequestParam String to) {
    try {
      emailService.sendSimpleMessage(
              to,
              "📬 Test Email từ hệ thống Edu360",
              "Xin chào,\n\nĐây là email test được gửi từ hệ thống Edu360.\nNếu bạn nhận được email này, cấu hình SMTP đã hoạt động thành công!\n\nThân ái,\nEdu360 Team."
      );
      return "✅ Gửi email thành công tới: " + to;
    } catch (Exception e) {
      e.printStackTrace();
      return "❌ Lỗi gửi email: " + e.getMessage();
    }
  }
}
