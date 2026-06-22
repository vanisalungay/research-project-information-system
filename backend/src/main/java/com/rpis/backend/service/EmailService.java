package com.rpis.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendApprovalEmail(String toEmail, String name) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("RPIS Account Approved");
            message.setText("Dear " + name + ",\n\n" +
                    "Your registration for the Research Project Information System (RPIS) has been reviewed and approved by the RII Admin.\n\n" +
                    "You can now log in to the system.\n\n" +
                    "Best regards,\n" +
                    "Research and Innovation Institute (RII) Admin");
            mailSender.send(message);
            System.out.println("Approval email sent successfully to " + toEmail);
        } catch (Exception e) {
            System.err.println("Failed to send approval email to " + toEmail + ": " + e.getMessage());
            System.out.println("[MOCK EMAIL LOG] To: " + toEmail + " | Subject: RPIS Account Approved | Body: Hello " + name + ", your account is approved.");
        }
    }

    public void sendRejectionEmail(String toEmail, String name) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("RPIS Account Registration Status");
            message.setText("Dear " + name + ",\n\n" +
                    "We regret to inform you that your registration request for the Research Project Information System (RPIS) was not approved at this time.\n\n" +
                    "If you believe this was a mistake, please contact the RII Admin.\n\n" +
                    "Best regards,\n" +
                    "Research and Innovation Institute (RII) Admin");
            mailSender.send(message);
            System.out.println("Rejection email sent successfully to " + toEmail);
        } catch (Exception e) {
            System.err.println("Failed to send rejection email to " + toEmail + ": " + e.getMessage());
            System.out.println("[MOCK EMAIL LOG] To: " + toEmail + " | Subject: RPIS Account Registration Status | Body: Hello " + name + ", your account request was not approved.");
        }
    }
}
