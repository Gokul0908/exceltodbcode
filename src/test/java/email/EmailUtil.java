package email;

import javax.mail.*;
import javax.mail.internet.*;
import java.io.File;
import java.util.Properties;

public class EmailUtil {

	public static void sendExecutionMail(boolean isSuccess, String runId, String reportPath) {
		try {
			Properties props = new Properties();
			props.put("mail.smtp.auth", "true");
			props.put("mail.smtp.starttls.enable", "true");
			props.put("mail.smtp.host", EmailConfig.SMTP_HOST);
			props.put("mail.smtp.port", EmailConfig.SMTP_PORT);

			Session session = Session.getInstance(props, new Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(EmailConfig.FROM_EMAIL, EmailConfig.PASSWORD);
				}
			});

			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(EmailConfig.FROM_EMAIL));
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(EmailConfig.TO_EMAIL));

			message.setSubject("API Automation Execution - " + (isSuccess ? "SUCCESS ✅" : "FAIL ❌"));

			// Mail body
			BodyPart messageBody = new MimeBodyPart();
			messageBody.setText("Execution Status : " + (isSuccess ? "PASSED" : "FAILED") + "\nRun ID : " + runId);

			Multipart multipart = new MimeMultipart();
			multipart.addBodyPart(messageBody);

			// Attachment
			if (reportPath != null) {
				MimeBodyPart attachment = new MimeBodyPart();
				attachment.attachFile(new File(reportPath));
				multipart.addBodyPart(attachment);
			}

			message.setContent(multipart);
			Transport.send(message);

			System.out.println("📧 Execution email sent successfully");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
