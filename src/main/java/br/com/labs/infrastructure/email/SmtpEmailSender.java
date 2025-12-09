package br.com.labs.infrastructure.email;

import br.com.labs.domain.auth.EmailSender;
import br.com.labs.domain.auth.MfaCode;
import br.com.labs.domain.user.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class SmtpEmailSender implements EmailSender {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailSender.class);

    private static final String FROM_ADDRESS = "noreply@loginmfa.com";
    private static final String SUBJECT = "Seu código de verificação";

    private final JavaMailSender mailSender;

    public SmtpEmailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    @Override
    public void sendMfaCode(Email to, MfaCode code) {
        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(FROM_ADDRESS);
            helper.setTo(to.value());
            helper.setSubject(SUBJECT);
            helper.setText(buildHtmlContent(code), true);

            mailSender.send(message);
            log.info("MFA code sent to {}", to.value());

        } catch (Exception e) {
            log.error("Failed to send MFA code to {}: {}", to.value(), e.getMessage());
        }
    }

    private String buildHtmlContent(MfaCode code) {
        return """
                <!DOCTYPE html>
                <html lang="pt-BR">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                </head>
                <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                    <div style="background-color: #f8f9fa; border-radius: 8px; padding: 30px; text-align: center;">
                        <h1 style="color: #333; margin-bottom: 10px;">Código de Verificação</h1>
                        <p style="color: #666; margin-bottom: 30px;">
                            Use o código abaixo para completar seu login:
                        </p>
                        <div style="background-color: #fff; border: 2px dashed #007bff; border-radius: 8px; padding: 20px; margin-bottom: 30px;">
                            <span style="font-size: 32px; font-weight: bold; letter-spacing: 8px; color: #007bff;">
                                %s
                            </span>
                        </div>
                        <p style="color: #999; font-size: 14px;">
                            Este código expira em <strong>5 minutos</strong>.
                        </p>
                        <p style="color: #999; font-size: 14px;">
                            Se você não solicitou este código, ignore este email.
                        </p>
                    </div>
                </body>
                </html>
                """.formatted(code.value());
    }
}
