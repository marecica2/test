package controllers;

import java.util.LinkedList;
import java.util.List;

import models.Contact;
import models.User;
import play.i18n.Messages;
import play.mvc.With;
import dto.UserDTO;
import email.EmailNotificationBuilder;

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

    public static void invites(String str)
    {
        final User user = getLoggedUser();
        List<Contact> c = Contact.getContacts(user, str.toLowerCase());
        List<UserDTO> contacts = new LinkedList<UserDTO>();
        if (user != null)
        {
            for (Contact contact : c)
                contacts.add(UserDTO.convert(contact.contact));
        }
        renderJSON(contacts);
    }

    public static void contactAdd(String uuid, String url)
    {
        checkAuthenticity();
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
        checkAuthenticity();
        User user = getLoggedUser();
        User u = User.getUserByLogin(email);

        validation.email(email);
        validation.required(email);
        if (validation.hasErrors())
        {
            flash.error(Messages.get("invalid-email"));
            Application.dashboard(null, null);
        }

        if (u != null)
        {
            validation.addError("error", "");
            flash.success(Messages.get("is-already-registered", email, email));
            params.put("email", email);
            params.flash();
            validation.keep();
            flash("email", email);
        } else
        {
            final String subject = Messages.get("invited-you-to-widgr-subject", user.getFullName());
            new EmailNotificationBuilder()
                    .setFrom(user)
                    .setToEmail(email)
                    .setSubject(subject)
                    .sendInvitation();
            flash.success(Messages.get("invitation-sent-to", email));
        }
        Application.dashboard(null, null);
    }

    public static void contactBlock(String uuid, String url)
    {
        checkAuthenticity();
        Contact contact = Contact.get(uuid);
        contact.blocked = true;
        contact.save();
        redirectTo(url);
    }

    public static void contactUnblock(String uuid, String url)
    {
        checkAuthenticity();
        Contact contact = Contact.get(uuid);
        contact.blocked = false;
        contact.save();
        redirectTo(url);
    }

    public static void contactFollow(String uuid, String url, String usr)
    {
        checkAuthenticity();
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