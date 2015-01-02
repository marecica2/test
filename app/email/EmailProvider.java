package email;

import java.util.Properties;

import javax.activation.MailcapCommandMap;
import javax.activation.MimetypesFileTypeMap;
import javax.mail.BodyPart;
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

import utils.Constants;

public class EmailProvider
{
    public static final String PROTOCOL_SMTP = "smtp";
    public static final String PROTOCOL_SMTPS = "smtps";

    private final String host;
    private final String port;
    private final String username;
    private final String password;
    private String protocol;
    private boolean tls = false;
    private final Properties props = System.getProperties();
    private Session session = null;

    public EmailProvider(String host, String port, String email, String password, String timeout, String protocol, boolean tls)
    {
        if (host.equals("DEFAULT"))
        {
            this.host = Constants.MAIL_HOST;
            this.port = Constants.MAIL_PORT;
            this.username = Constants.MAIL_ACCOUNT;
            this.password = Constants.MAIL_PASSWORD;
            this.protocol = Constants.MAIL_PROTOCOL_SMTP;
            this.tls = true;
        } else
        {
            this.host = host;
            this.port = port;
            this.username = email;
            this.password = password;
            this.protocol = protocol.toLowerCase();
            this.tls = tls;
        }
        emailSettings();
        createSession();
    }

    public EmailProvider()
    {
        this.host = Constants.MAIL_HOST;
        this.port = Constants.MAIL_PORT;
        this.username = Constants.MAIL_ACCOUNT;
        this.password = Constants.MAIL_PASSWORD;
        this.protocol = Constants.MAIL_PROTOCOL_SMTP;
        this.tls = true;
        emailSettings();
        createSession();
    }

    private void emailSettings()
    {
        //        props.put("mail.transport.protocol", protocol);
        //        props.put("mail." + protocol + ".host", host);
        //        props.put("mail." + protocol + ".starttls.enable", tls);
        //        props.put("mail." + protocol + ".port", port);
        //        props.put("mail." + protocol + ".auth", "true");
        //        props.put("mail." + protocol + ".timeout", "10000");

        this.protocol = "smtps";
        props.put("mail.transport.protocol", protocol);
        props.put("mail." + protocol + ".host", "mail.wid.gr");
        props.put("mail." + protocol + ".starttls.enable", "true");
        props.put("mail." + protocol + ".port", 587);
        props.put("mail." + protocol + ".auth", "true");

        props.put("mail." + protocol + ".socketFactory.class", "email.HotmailSSLSocketFactory");

        if (host.contains("live.com"))
            props.put("mail." + protocol + ".socketFactory.class", "email.HotmailSSLSocketFactory");

        //        if ("smtps".equals(protocol))
        //            props.put("mail." + protocol + ".ssl.enable", "true");
        //        else
        //            props.put("mail." + protocol + ".ssl.enable", "false");

    }

    private void createSession()
    {
        session = Session.getInstance(props, new javax.mail.Authenticator()
        {
            @Override
            protected PasswordAuthentication getPasswordAuthentication()
            {
                return new PasswordAuthentication(username, password);
            }
        });
        //session.setDebug(true);
    }

    public boolean sendMessage(String toEmail, String subject, String msg) throws Exception
    {
        return sendMessage(toEmail, null, subject, msg);
    }

    public boolean sendMessage(String toEmail, String[] ccs, String subject, String msg) throws Exception
    {
        Transport t = session.getTransport(protocol);
        try
        {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(this.username));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            if (ccs != null)
            {
                for (int i = 0; i < ccs.length; i++)
                {
                    message.addRecipient(Message.RecipientType.CC, new InternetAddress(ccs[i]));
                }
            }
            message.setSubject(subject);
            message.setContent(msg, "text/html; charset=UTF-8");

            t.connect(username, password);
            t.sendMessage(message, message.getAllRecipients());
            return true;

        } catch (Exception e)
        {
            throw new Exception(e);
        } finally
        {
            t.close();
        }
    }

    public void sendEmail(String subject, String recipient, String htmlPart) throws Exception
    {
        MimetypesFileTypeMap mimetypes = (MimetypesFileTypeMap) MimetypesFileTypeMap.getDefaultFileTypeMap();
        mimetypes.addMimeTypes("text/calendar ics ICS");

        MailcapCommandMap mailcap = (MailcapCommandMap) MailcapCommandMap.getDefaultCommandMap();
        mailcap.addMailcap("text/calendar;; x-java-content-handler=com.sun.mail.handlers.text_plain");

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress("info@wid.gr"));
        message.setSubject(subject);
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));

        Multipart multipart = new MimeMultipart("alternative");

        BodyPart messageBodyPart = buildHtmlTextPart(htmlPart);
        multipart.addBodyPart(messageBodyPart);
        message.setContent(multipart);

        Transport transport = session.getTransport(protocol);
        transport.connect(username, password);
        transport.sendMessage(message, message.getAllRecipients());
        transport.close();
    }

    private BodyPart buildHtmlTextPart(String htmlPart) throws MessagingException
    {
        MimeBodyPart descriptionPart = new MimeBodyPart();
        descriptionPart.setContent(htmlPart, "text/html; charset=utf-8");
        return descriptionPart;
    }

    //private final SimpleDateFormat iCalendarDateFormat = new SimpleDateFormat("yyyyMMdd'T'HHmm'00'");

    //    public BodyPart buildCalendarPart(Event event, Attendance attendance, String baseUrl) throws Exception
    //    {
    //        BodyPart calendarPart = new MimeBodyPart();
    //        Date start = event.eventStart;
    //        Date end = event.eventEnd;
    //
    //        String calendarContent = "BEGIN:VCALENDAR\n";
    //        calendarContent += "METHOD:REQUEST\n";
    //        calendarContent += "PRODID: New Event \n";
    //        calendarContent += "VERSION:2.0\n";
    //        calendarContent += "BEGIN:VEVENT\n" + "DTSTAMP:" + iCalendarDateFormat.format(start) + "\n";
    //        calendarContent += "DTSTART:" + iCalendarDateFormat.format(start) + "\n";
    //        calendarContent += "DTEND:" + iCalendarDateFormat.format(end) + "\n";
    //        calendarContent += "SUMMARY:" + event.listing.title + " \n" + "UID:" + event.uuid + "\n";
    //        calendarContent += "ATTENDEE;ROLE=REQ-PARTICIPANT;PARTSTAT=NEEDS-ACTION;RSVP=TRUE:MAILTO:" + event.user.login + "\n";
    //        calendarContent += "ORGANIZER:MAILTO:" + event.user.login + "\n";
    //
    //        if (!attendance.isForUser)
    //            calendarContent += "LOCATION:" + baseUrl + "public/calendar?event=" + event.uuid + "&id=" + event.user.uuid + " \n";
    //        else
    //            calendarContent += "LOCATION:" + baseUrl + "?event=" + event.uuid + "&id=" + event.user.uuid + " \n";
    //
    //        calendarContent += "DESCRIPTION:" + event.listing.title + "\n";
    //        calendarContent += "SEQUENCE:0\n";
    //        calendarContent += "PRIORITY:5\n";
    //        calendarContent += "CLASS:PUBLIC\n";
    //        calendarContent += "STATUS:CONFIRMED\n";
    //        calendarContent += "TRANSP:OPAQUE\n";
    //        calendarContent += "BEGIN:VALARM\n";
    //        calendarContent += "ACTION:DISPLAY\n";
    //        calendarContent += "DESCRIPTION:REMINDER\n";
    //        calendarContent += "TRIGGER;RELATED=START:-PT00H15M00S\n";
    //        calendarContent += "END:VALARM\n";
    //        calendarContent += "END:VEVENT\n";
    //        calendarContent += "END:VCALENDAR";
    //        calendarPart.addHeader("Content-Class", "urn:content-classes:calendarmessage");
    //        calendarPart.setContent(calendarContent, "text/calendar;method=CANCEL");
    //        return calendarPart;
    //    }
}
