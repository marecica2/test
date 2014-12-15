package controllers;

import java.util.List;

import models.Contact;
import models.User;

import org.apache.velocity.VelocityContext;

import play.i18n.Messages;
import play.mvc.With;
import templates.VelocityTemplate;
import email.EmailProvider;
import email.Notification;

@With(Secure.class)
public class Contacts extends BaseController
{
    public static void contacts()
    {
        User user = getLoggedUser();
        User usr = user;
        List<Contact> contacts = Contact.getContacts(user);
        render(user, usr, contacts);
    }

    public static void contactAdd(String uuid, String url)
    {
        User user = getLoggedUser();
        User c = User.getUserByUUID(uuid);
        Contact contact = new Contact();
        contact.user = user;
        contact.contact = c;
        contact.saveContact();
        redirectTo(url);
    }

    public static void contactInvite(String email)
    {
        User user = getLoggedUser();
        User u = User.getUserByLogin(email);
        if (u != null)
        {
            validation.addError("error", "");
            flash.error(Messages.get("%s is already registered", email));
            params.put("email", email);
            params.flash();
            validation.keep();
            flash("email", email);
        } else
        {
            flash.success(Messages.get("invitation sent to %s", email));

            // baseUrl in format https://localhost:10001/
            final String baseUrl = getProperty(BaseController.CONFIG_BASE_URL);
            final String locale = "en";
            final EmailProvider emailProvider = new EmailProvider(user.account.smtpHost, user.account.smtpPort,
                    user.account.smtpAccount, user.account.smtpPassword, "10000", user.account.smtpProtocol, true);
            final VelocityContext ctx = VelocityTemplate.createInvitationTemplate(locale, email, user, null, baseUrl, null);
            final String body = VelocityTemplate.processTemplate(ctx, VelocityTemplate.getTemplateContent(VelocityTemplate.CONTACT_INVITE_TEMPLATE));

            final String from = user.login;
            final String subject = "Widgr: " + user.getFullName() + " invited you to Widgr";
            new Notification(emailProvider, from, subject, email, body).execute();
        }
        contacts();
    }

    public static void contactBlock(String uuid, String url)
    {
        Contact contact = Contact.get(uuid);
        contact.blocked = true;
        contact.save();
        redirectTo(url);
    }

    public static void contactUnblock(String uuid, String url)
    {
        Contact contact = Contact.get(uuid);
        contact.blocked = false;
        contact.save();
        redirectTo(url);
    }

    public static void contactFollow(String uuid, String url, String usr)
    {
        if (usr != null)
        {
            User user = getLoggedUser();
            User contactUser = User.getUserByUUID(usr);
            Contact contact = Contact.get(user, contactUser);
            if (contact == null)
            {
                contact = new Contact();
                contact.contact = contactUser;
                contact.user = user;
            }
            contact.following = true;
            contact.saveContact();
        } else
        {
            Contact contact = Contact.get(uuid);
            contact.following = true;
            contact.save();
        }
        redirectTo(url);
    }

    public static void contactUnfollow(String uuid, String url)
    {
        Contact contact = Contact.get(uuid);
        contact.following = false;
        contact.save();
        redirectTo(url);
    }

    public static void contactDelete(String uuid, String url)
    {
        Contact contact = Contact.get(uuid);
        contact.delete();
        redirectTo(url);
    }
}