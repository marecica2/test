package templates;

import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;

import models.Attendance;
import models.Event;
import models.User;

import org.apache.commons.io.IOUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import play.Logger;
import play.i18n.Messages;
import utils.DateTimeUtils;
import controllers.Application;

public class VelocityTemplate
{
    public static final String DEFAULT_EVENT_TEMPLATE = "/templates/eventTemplate.vm";
    public static final String CONTACT_INVITE_TEMPLATE = "/templates/invitationTemplate.vm";

    public static VelocityContext prepareContext(Event event, Attendance attendance, String baseUrl, String message)
    {
        VelocityContext context = new VelocityContext();
        try
        {
            String locale = "en";
            DateTimeUtils dt = new DateTimeUtils(DateTimeUtils.TYPE_DEFAULT);
            context.put("user", event.user.getFullName());
            final String imagePath = baseUrl + event.user.getAvatarUrl() + "_64x64";
            context.put("userImage", imagePath);
            context.put("userAbout", event.listing.description);
            context.put("title", event.listing.title);
            context.put("message", message);
            context.put("description", event.listing.description);
            context.put("image", baseUrl + event.listing.imageUrl);
            context.put("url", baseUrl + "event-detail?uuid=" + event.uuid);
            context.put("color", event.listing.color);
            context.put("date", dt.formatDate(event.eventStart, new SimpleDateFormat(dt.TYPE_DATE_ONLY)));
            context.put("start", dt.formatDate(event.eventStart, new SimpleDateFormat(dt.TYPE_TIME_ONLY)));
            context.put("end", dt.formatDate(event.eventEnd, new SimpleDateFormat(dt.TYPE_TIME_ONLY)));

            if (attendance != null)
            {
                context.put("newAccount", attendance.customer == null && attendance.user == null ? true : false);
                context.put("newAccountUrl", baseUrl + "registration?email=" + attendance.email + "&id=" + attendance.uuid);
                context.put("email", attendance.email);
                if (!attendance.isForUser && attendance.customer != null && attendance.customer.locale != null)
                    locale = attendance.customer.locale;
            }

            context.put("emailRegister1", Messages.getMessage(locale, "email.register1"));
            context.put("emailRegister2", Messages.getMessage(locale, "email.register2"));
            context.put("emailRegister3", Messages.getMessage(locale, "email.register3"));
        } catch (Exception e)
        {
            Logger.error(e, "Error occured while creating velocity context");
        }

        return context;
    }

    public static String getTemplateContent(String templatePath)
    {
        try
        {
            String templateContent;
            templateContent = IOUtils.toString(Application.class.getResourceAsStream(templatePath));
            return templateContent;
        } catch (IOException e)
        {
            Logger.error(e, "");
        }
        return null;
    }

    public static String processTemplate(VelocityContext ctx, String template)
    {
        try
        {
            Velocity.init();
            StringWriter result = new StringWriter();
            if (template == null)
                template = getTemplateContent(DEFAULT_EVENT_TEMPLATE);
            Velocity.evaluate(ctx, result, "defaultEmailTemplate", template);
            return result.toString();
        } catch (Exception e)
        {
            Logger.error(e, "");
        }
        return null;
    }

    public static String processEventInvitationTemplate(Event event, Attendance attendance, String baseUrl, String message)
    {
        VelocityContext ctx = VelocityTemplate.prepareContext(event, attendance, baseUrl, message);
        final String msgContent = VelocityTemplate.processTemplate(ctx, VelocityTemplate.getTemplateContent(VelocityTemplate.DEFAULT_EVENT_TEMPLATE));
        return msgContent;
    }

    public static VelocityContext createInvitationTemplate(String locale, String email, User user, Event event, String baseUrl, String attendance)
    {
        VelocityContext ctx = new VelocityContext();
        ctx.put("color0", "#BF94D1");
        ctx.put("color1", "#954db3");
        ctx.put("color2", "#F8F8F8");
        ctx.put("color3", "#F0F0F0");
        ctx.put("logo", baseUrl + "public/images/logo_purple.png");

        ctx.put("user", user.getFullName());
        ctx.put("userImage", baseUrl + user.avatarUrl);
        ctx.put("userUrl", baseUrl + "user/" + user.login);
        ctx.put("userUrlLabel", "view on Widgr");

        ctx.put("userAbout", "<h2>About " + user.getFullName() + "</h2>" + user.userAboutHtml());
        ctx.put("title", "Invited you to Widgr.");

        ctx.put("message",
                "<h2>What is Widgr?</h2><p>With Widgr you can get help anytime from people with expertise across a range of topics - teachers, counselors, doctors, home repair specialists, personal trainers, hobby enthusiasts, and more.</p><p>You can choose who to get help from based on qualifications, availability, ratings and reviews. Also, you can choose to get help right away or schedule a Helpout for later.</p><p><h3>Just fill simple registration and start using Widgr even today</h3></p>");

        ctx.put("url", baseUrl + "registration?email=" + email + "&token=" + user.referrerToken);
        ctx.put("urlLabel", "Register now");
        ctx.put("url1", baseUrl + "about");
        ctx.put("urlLabel1", "Learn more about Widgr");

        if (event != null)
        {
            ctx.put("title", "Invited you event " + event.listing.title);
            ctx.put("event", event.listing.title);
            ctx.put("eventUrl", baseUrl + "event/" + event.uuid);
            ctx.put("eventImage", baseUrl + event.listing.imageUrl);
            DateTimeUtils dt = new DateTimeUtils();
            ctx.put("eventDate", dt.formatDate(event.eventStart, new SimpleDateFormat("d. MMM yyyy")));
            ctx.put("eventStart", dt.formatDate(event.eventStart, new SimpleDateFormat("h.MM a")));
            ctx.put("eventEnd", dt.formatDate(event.eventEnd, new SimpleDateFormat("h.MM a")));

            // override registration to add another param
            ctx.put("url", baseUrl + "registration?email=" + email + "&token=" + user.referrerToken + "&invitation=" + attendance);
        }

        return ctx;
    }

    public static VelocityContext createBasicTemplate(String locale, String baseUrl, String title, String message)
    {
        VelocityContext ctx = new VelocityContext();
        ctx.put("color0", "#BF94D1");
        ctx.put("color1", "#954db3");
        ctx.put("color2", "#F8F8F8");
        ctx.put("color3", "#F0F0F0");
        ctx.put("logo", baseUrl + "public/images/logo_purple.png");

        ctx.put("title", title);
        ctx.put("message", message);
        return ctx;
    }
}
