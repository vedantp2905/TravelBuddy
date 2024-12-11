package TravelBuddy.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendVerificationEmail(String to, String token) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);

            String htmlMsg = "<h3>Email Verification for TravelBuddy</h3>"
                    + "<p>Please verify your email by clicking on this link: "
                    + "<a href='http://coms-3090-010.class.las.iastate.edu:8080/api/users/verify?token=" + token + "'>Verify Email</a></p>"
                    + "<p>If you didn't request this, please ignore this email.</p>"
                    + "<p>Best regards,<br>The TravelBuddy Team</p>";

            helper.setText(htmlMsg, true); // Use this to send an HTML email
            helper.setTo(to);
            helper.setSubject("TravelBuddy - Email Verification");
            helper.setFrom("noreply@travelbuddy.com");

            mailSender.send(mimeMessage);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send verification email: " + e.getMessage(), e);
        }
    }

        public void sendNewsletter(String to, String newsletter) {
           try {
               MimeMessage mimeMessage = mailSender.createMimeMessage();
               MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

               helper.setText(newsletter, true);
               helper.setTo(to);
               helper.setSubject("TravelBuddy Newsletter");
               helper.setFrom("noreply@travelbuddy.com");

               mailSender.send(mimeMessage);
           } catch (Exception e) {
               throw new RuntimeException("Failed to send newsletter: " + e.getMessage(), e);
           }
        }

    public void sendItineraryEmail(String to, String itineraryHtml, String subject, byte[] pdfAttachment) {
        try {
            if (to == null || to.trim().isEmpty()) {
                throw new IllegalArgumentException("Email recipient cannot be null or empty");
            }
            
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "utf-8");

            // Add some styling to the HTML content
            String styledHtml = "<html><head>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
                "h1 { color: #2c3e50; }" +
                "h2 { color: #34495e; }" +
                ".container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
                "</style></head>" +
                "<body><div class='container'>" +
                itineraryHtml +
                "<p>Please find attached a PDF copy of your itinerary.</p>" +
                "</div></body></html>";

            helper.setText(styledHtml, true);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setFrom("noreply@travelbuddy.com");
            
            // Attach PDF
            helper.addAttachment("itinerary.pdf", new ByteArrayResource(pdfAttachment));

            mailSender.send(mimeMessage);
            System.out.println("Email sent successfully to: " + to);
        } catch (Exception e) {
            System.err.println("Failed to send itinerary email: " + e.getMessage());
            throw new RuntimeException("Failed to send itinerary email: " + e.getMessage(), e);
        }
    }

    public void sendPasswordResetEmail(String to, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Password Reset Request");
        message.setText("To reset your password, click the following link: " + resetLink + 
                "\n\nThis link will expire in 24 hours.");
        
        mailSender.send(message);
    }

    public void sendPasswordResetCode(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Password Reset Code");
        message.setText("Your password reset code is: " + code + "\n\nThis code will expire in 15 minutes.");
        mailSender.send(message);
    }

    public void sendSimpleEmail(String to, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);
        message.setFrom("noreply@travelbuddy.com");
        
        try {
            mailSender.send(message);
            System.out.println("Simple email sent successfully to: " + to);
        } catch (Exception e) {
            System.err.println("Failed to send simple email: " + e.getMessage());
            throw new RuntimeException("Failed to send simple email: " + e.getMessage(), e);
        }
    }
}
