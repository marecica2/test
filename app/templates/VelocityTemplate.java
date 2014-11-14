package templates;

import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;

import models.Attendance;
import models.Event;

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

    public static VelocityContext prepareContext(Event event, Attendance attendance, String baseUrl, String message)
    {
        VelocityContext context = new VelocityContext();
        try
        {
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
            context.put("newAccount", attendance.customer == null && attendance.user == null ? true : false);
            context.put("newAccountUrl", baseUrl + "registration?email=" + attendance.email + "&id=" + attendance.uuid);
            context.put("email", attendance.email);

            String locale = "en";
            if (!attendance.isForUser && attendance.customer != null && attendance.customer.locale != null)
                locale = attendance.customer.locale;

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
}
