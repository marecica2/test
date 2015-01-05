package templates;

import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;

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
    public static final String DEFAULT_TEMPLATE = "/templates/invitationTemplate.vm";

    public static String getTemplate(String templatePath)
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

    public static String generateTemplate(VelocityContext ctx, String template)
    {
        try
        {
            Velocity.init();
            StringWriter result = new StringWriter();
            Velocity.evaluate(ctx, result, "invitationTemplate", template);
            return result.toString();
        } catch (Exception e)
        {
            Logger.error(e, "");
        }
        return null;
    }

    public static VelocityContext createInvitationTemplate(String locale, String email, User from, User to, Event event, String baseUrl, String attendance)
    {
        VelocityContext ctx = new VelocityContext();
        ctx.put("color0", "#BF94D1");
        ctx.put("color1", "#954db3");
        ctx.put("color2", "#F8F8F8");
        ctx.put("color3", "#F0F0F0");
        ctx.put("color4", "#363636");
        ctx.put("existing", to != null ? true : false);
        ctx.put("baseUrl", baseUrl);
        ctx.put("logo", baseUrl + "public/images/logo_purple_footer.png");

        ctx.put("user", from.getFullName());
        ctx.put("userImage", baseUrl + from.avatarUrl);
        ctx.put("userUrl", baseUrl + "user/" + from.login);
        ctx.put("userUrlLabel", Messages.get("view-on-widgr"));
        ctx.put("contact", Messages.get("contact"));
        ctx.put("help", Messages.get("help"));
        ctx.put("security", Messages.get("security"));

        ctx.put("regards", Messages.get("regards-html"));
        ctx.put("title", Messages.get("you-have-been-invited-to-widgr", from.getFullName()));
        ctx.put("message", Messages.get("email-what-is-widgr", baseUrl));
        ctx.put("userAbout", "<h3>" + from.getFullName() + "</h3>" + from.userAboutHtml());
        ctx.put("footer", Messages.get("email-footer"));
        ctx.put("url", baseUrl + "registration?email=" + email + "&token=" + from.referrerToken);
        ctx.put("urlLabel", Messages.get("register-now"));

        if (event != null)
        {
            ctx.put("title", Messages.get("you-have-been-invited-to-event", event.listing.title));
            ctx.put("event", event.listing.title);
            ctx.put("eventDescription", event.listing.getDescriptionHtml());
            ctx.put("eventUrl", baseUrl + "event/" + event.uuid);
            ctx.put("eventImage", baseUrl + event.listing.imageUrl);
            DateTimeUtils dt = new DateTimeUtils();
            ctx.put("eventDate", dt.formatDate(event.eventStart, new SimpleDateFormat("d. MMM")));
            ctx.put("eventStart", dt.formatDate(event.eventStart, new SimpleDateFormat("h:mm a")));
            ctx.put("eventEnd", dt.formatDate(event.eventEnd, new SimpleDateFormat("h:mm a")) + " GMT");

            // override registration to add another param
            ctx.put("url", baseUrl + "registration?email=" + email + "&token=" + from.referrerToken + "&invitation=" + attendance);
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
        ctx.put("color4", "#363636");
        ctx.put("baseUrl", baseUrl);
        ctx.put("logo", baseUrl + "public/images/logo_purple_footer.png");

        ctx.put("title", title);
        ctx.put("message", message);

        ctx.put("regards", Messages.get("regards-html"));
        ctx.put("contact", Messages.get("contact"));
        ctx.put("help", Messages.get("help"));
        ctx.put("security", Messages.get("security"));
        ctx.put("footer", Messages.get("email-footer"));
        return ctx;
    }
}
