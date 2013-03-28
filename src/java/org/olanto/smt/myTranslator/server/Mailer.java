/**********
    Copyright © 2010-2012 Olanto Foundation Geneva

   This file is part of myMT.

   myMT is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation, either version 3 of
    the License, or (at your option) any later version.

    myMT is distributed in the hope that it will be useful, but
    WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
    See the GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with myMT.  If not, see <http://www.gnu.org/licenses/>.

**********/
package org.olanto.smt.myTranslator.server;

import org.olanto.smt.translator.entities.Text;
import org.olanto.smt.translator.events.TranslatorListener;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import static org.olanto.smt.myTranslator.server.Config.*;

/**
 * Classe utilisée comme Translator Listener, et qui envoie le résultat par mail.
  */
public class Mailer implements TranslatorListener {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH:mm");
    private static final String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory";
    private final String[] recipients;
    private final Properties props;

    public Mailer(String[] recipients) {
        if (recipients == null || recipients.length == 0) {
            throw new IllegalArgumentException("There must be at least one recipient.");
        }
        this.recipients = recipients;

        this.props = new Properties();
        props.setProperty("mail.transport.protocol", "smtp");
        props.setProperty("mail.smtp.host", SMTP_HOST_NAME);
        if (SMTP_AUTH.equals("YES")) {  // need authentification
            props.setProperty("mail.smtp.auth", "true");
        } else {
            props.setProperty("mail.smtp.auth", "false");
        }
        if (SMTP_AUTH.equals("YES")) {  // need ssl
            props.setProperty("mail.smtp.socketFactory.class", SSL_FACTORY);
            // keep only secure connexions :
            props.setProperty("mail.smtp.socketFactory.fallback", "false");
        }
        props.setProperty("mail.smtp.port", SMTP_PORT);
        props.setProperty("mail.smtp.socketFactory.port", SMTP_PORT);
    }

    @Override
    public void endTranslation(Text text) {
        try {
            Message msg = createMessage();
            // Setting the Subject and Content Type
            msg.setSubject(MAIL_SUBJECT_TXT);

            MimeBodyPart textPart = new MimeBodyPart();

            // *** change this line to modify the mail content ***
            textPart.setText("The translation was completed successfully.\n "
                    + "The result is attached to this email in plain text format.\n");

            MimeBodyPart attachmentPart = new MimeBodyPart();
            // *** change this line to modify the attachment content ***
            DataSource ds = new ByteArrayDataSource(text.toString(), "text/plain;charset=UTF-8");
            DataHandler handler = new DataHandler(ds);
            attachmentPart.setDataHandler(handler);
            // *** change this line to modify the attachment file name ***
            attachmentPart.setFileName(MAIL_FILE_PREFIX+ dateFormat.format(new Date()) + ".txt");



            Multipart mp = new MimeMultipart();
            mp.addBodyPart(textPart);
            mp.addBodyPart(attachmentPart);

            if (text.isFromfile() && text.getUpfile().isSdlxliff()) {  // it' a xliff
                System.out.println("mail a xliff result");

                MimeBodyPart attachmentPartXliff = new MimeBodyPart();
                attachmentPartXliff.setDisposition(MimeBodyPart.ATTACHMENT);
                // *** change this line to modify the attachment content ***
                DataSource dsXliff = new ByteArrayDataSource(text.getXliffDoc(), "text/plain;charset=UTF-8");
                DataHandler handlerXliff = new DataHandler(dsXliff);
                attachmentPartXliff.setDataHandler(handlerXliff);
                // *** change this line to modify the attachment file name ***
                attachmentPartXliff.setFileName(MAIL_FILE_PREFIX+ text.getUpfile().getFileName()); // le nom du fichier
                mp.addBodyPart(attachmentPartXliff);
            }
            if (text.isFromfile() && text.getUpfile().isDocx()) {  // it' a docx
                System.out.println("mail a docx result");

                MimeBodyPart attachmentPartDocx = new MimeBodyPart();
                attachmentPartDocx.setDisposition(MimeBodyPart.ATTACHMENT);
                
                // *** change this line to modify the attachment content ***
                DataSource dsXliff = new ByteArrayDataSource(text.getDocxDoc(), "text/plain;charset=UTF-8");
                DataHandler handlerXliff = new DataHandler(dsXliff);
                attachmentPartDocx.setDataHandler(handlerXliff);
                // *** change this line to modify the attachment file name ***
                attachmentPartDocx.setFileName(MAIL_FILE_PREFIX + text.getUpfile().getFileName()); // le nom du fichier
                mp.addBodyPart(attachmentPartDocx);
            }

            msg.setContent(mp);

            Transport.send(msg);
        } catch (IOException ex) {
            // exception during creating the attachment
            ex.printStackTrace();
        } catch (MessagingException ex) {
            //TODO
            ex.printStackTrace();
        }
    }


    @Override
    public void errorTranslation(Text text, String errorMsg) {
        try {
            Message msg = createMessage();
            // Setting the Subject and Content Type
            msg.setSubject(MAIL_SUBJECT_TXT);
            msg.setContent("An errors occurs during the translation.\n"
                    + "The message is : " + errorMsg, "text/plain");
            Transport.send(msg);
        } catch (MessagingException ex) {
            //TODO
            ex.printStackTrace();
        }
    }

    /**
     * Create a message with all parameters set except Subject and Content.
     * @return the created message.
     * @throws MessagingException if an error occurs during the message or
     * session creation.
     */
    private Message createMessage() throws MessagingException {
        Session session = Session.getInstance(props, new javax.mail.Authenticator() {

            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                //   System.out.println("authorisation:"+SMTP_AUTH_USER+"/"+SMTP_AUTH_PWD);
                return new PasswordAuthentication(SMTP_AUTH_USER, SMTP_AUTH_PWD);
            }
        });

        //TODO change
        session.setDebug(true);

        Message msg = new MimeMessage(session);
        // set the from and to address
        InternetAddress addressFrom = new InternetAddress(FROM_MAIL);
        msg.setFrom(addressFrom);

        InternetAddress[] addressTo = new InternetAddress[recipients.length];
        for (int i = 0; i < recipients.length; i++) {
            addressTo[i] = new InternetAddress(recipients[i]);
        }
        msg.setRecipients(Message.RecipientType.TO, addressTo);

        return msg;
    }
}
