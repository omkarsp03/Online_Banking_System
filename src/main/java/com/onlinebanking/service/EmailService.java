package com.onlinebanking.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Email sent successfully to {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}, error: {}", to, e.getMessage());
        } catch (Exception e) {
            log.warn("Email service is not fully configured. Simulating sending email to {} - Subject: '{}'", to, subject);
        }
    }

    @Async
    public void sendRegistrationEmail(String toEmail, String name) {
        String subject = "Welcome to Online Banking!";
        String content = "<h3>Hello " + name + ",</h3>"
                + "<p>Thank you for registering with our Online Banking system.</p>"
                + "<p>Your account is now active, and you can create Checking or Savings bank accounts to start managing your finances.</p>"
                + "<br/><p>Best Regards,<br/>Online Banking Team</p>";
        sendEmail(toEmail, subject, content);
    }

    @Async
    public void sendTransactionAlert(String toEmail, String name, String accountNumber, String transactionType, BigDecimal amount, String currency) {
        String subject = "Transaction Alert: " + transactionType;
        String content = "<h3>Hello " + name + ",</h3>"
                + "<p>We wanted to inform you of a recent transaction on your account <strong>•••• " + accountNumber.substring(accountNumber.length() - 4) + "</strong>.</p>"
                + "<ul>"
                + "<li><strong>Type:</strong> " + transactionType + "</li>"
                + "<li><strong>Amount:</strong> " + currency + " " + amount + "</li>"
                + "</ul>"
                + "<p>If you did not authorize this transaction, please contact our support team immediately.</p>"
                + "<br/><p>Best Regards,<br/>Online Banking Team</p>";
        sendEmail(toEmail, subject, content);
    }

    @Async
    public void sendTransferConfirmation(String toEmail, String name, String sourceAcc, String destAcc, BigDecimal amount, String currency, boolean isOutgoing) {
        String subject = isOutgoing ? "Transfer Sent Confirmation" : "Transfer Received Alert";
        String content = "<h3>Hello " + name + ",</h3>"
                + (isOutgoing 
                    ? "<p>Your transfer of <strong>" + currency + " " + amount + "</strong> from account <strong>•••• " + sourceAcc.substring(sourceAcc.length() - 4) + "</strong> to account <strong>•••• " + destAcc.substring(destAcc.length() - 4) + "</strong> has been processed successfully.</p>"
                    : "<p>You have received a transfer of <strong>" + currency + " " + amount + "</strong> into your account <strong>•••• " + destAcc.substring(destAcc.length() - 4) + "</strong> from account <strong>•••• " + sourceAcc.substring(sourceAcc.length() - 4) + "</strong>.</p>")
                + "<br/><p>Best Regards,<br/>Online Banking Team</p>";
        sendEmail(toEmail, subject, content);
    }
}
