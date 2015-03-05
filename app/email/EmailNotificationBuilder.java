package email;

import java.util.List;

import models.Attendance;
import models.Event;
import models.User;

import org.apache.velocity.VelocityContext;

import templates.VelocityTemplate;
import utils.WikiUtils;
import controllers.BaseController;

public class EmailNotificationBuilder
{
    private User from = null;
    private User to = null;
    private String fromEmail = null;
    private String toEmail = null;
    private String subject = null;
    private String message = null;
    private Event event = null;
    private Attendance attendance = null;

    public EmailNotificationBuilder setAttendance(Attendance attendance)
    {
        this.attendance = attendance;
        return this;
    }

    public EmailNotificationBuilder setEvent(Event event)
    {
        this.event = event;
        return this;
    }

    public EmailNotificationBuilder setWidgrFrom()
    {
        this.fromEmail = "info@wid.gr";
        return this;
    }

    public EmailNotificationBuilder setFrom(User from)
    {
        this.from = from;
        return this;
    }

    public EmailNotificationBuilder setTo(User to)
    {
        this.to = to;
        return this;
    }

    public EmailNotificationBuilder setSubject(String subject)
    {
        this.subject = subject;
        return this;
    }

    public EmailNotificationBuilder setMessageRaw(String message)
    {
        this.message = message;
        return this;
    }

    public EmailNotificationBuilder setMessageWiki(String message)
    {
        this.message = WikiUtils.parseToHtml(message);
        return this;
    }

    public EmailNotificationBuilder setFromEmail(String from)
    {
        this.fromEmail = from;
        return this;
    }

    public EmailNotificationBuilder setToEmail(String to)
    {
        this.toEmail = to;
        return this;
    }

    public void send()
    {
        final EmailProvider ep = new EmailProvider();
        final String baseUrl = BaseController.getProperty(BaseController.CONFIG_BASE_URL);
        final String recipient = this.to != null ? to.login : toEmail;
        final String locale = this.to != null ? to.locale : "en";

        if (to == null || to.emailNotification)
        {
            final VelocityContext ctx = VelocityTemplate.createBasicTemplate(this.to, locale, baseUrl, subject, message);
            final String template = VelocityTemplate.getTemplate(VelocityTemplate.DEFAULT_TEMPLATE);
            final String body = VelocityTemplate.generateTemplate(ctx, template);
            new EmailNotificationRunner(ep, "Widgr - " + subject, recipient, body).execute();
        }
    }

    public void sendMultiple(List<EmailContainer> emails)
    {
        final EmailProvider ep = new EmailProvider();
        final String baseUrl = BaseController.getProperty(BaseController.CONFIG_BASE_URL);
        for (EmailContainer email : emails)
        {
            if (to == null || to.emailNotification)
            {
                final String locale = email.locale != null ? email.locale : "en";
                final VelocityContext ctx = VelocityTemplate.createBasicTemplate(new User(), locale, baseUrl, subject, message);
                final String template = VelocityTemplate.getTemplate(VelocityTemplate.DEFAULT_TEMPLATE);
                final String body = VelocityTemplate.generateTemplate(ctx, template);
                email.body = body;
            }
        }
        new EmailNotificationRunner(ep, emails).execute();
    }

    public void sendInvitation()
    {
        final EmailProvider ep = new EmailProvider();
        final String baseUrl = BaseController.getProperty(BaseController.CONFIG_BASE_URL);
        final String att = attendance != null ? this.attendance.uuid : null;
        final String recipient = this.to != null ? to.login : toEmail;
        final String locale = this.to != null ? to.locale : "en";

        if (to == null || to.emailNotification)
        {
            final VelocityContext ctx = VelocityTemplate.createInvitationTemplate(locale, recipient, from, to, event, baseUrl, att, message);
            if (message != null && message.length() > 0)
            {
                ctx.put("notification", message);
                ctx.put("notificationLabel", from.firstName + ":");
            }
            final String body = VelocityTemplate.generateTemplate(ctx, VelocityTemplate.getTemplate(VelocityTemplate.DEFAULT_TEMPLATE));
            new EmailNotificationRunner(ep, "Widgr - " + subject, recipient, body).execute();
        }
    }

}
