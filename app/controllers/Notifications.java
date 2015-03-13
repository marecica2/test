package controllers;

import java.util.List;

import models.Message;
import models.User;
import play.cache.Cache;
import play.i18n.Messages;
import play.mvc.With;
import utils.RandomUtil;
import utils.StringUtils;

@With(Secure.class)
public class Notifications extends BaseController
{
    public static void send(String toUser, String subject, String emailBody, String url, String thread, String link)
    {
        final User userFrom = getLoggedUserNotCache();
        final User user = userFrom;

        String email = StringUtils.extract(toUser, "\\((.+?)\\)");
        if (email == null)
            email = toUser;

        validation.required("userTo", email);
        validation.required("subject", subject);
        validation.email("userTo", email).message("invalid-user-name");
        validation.required("emailBody", emailBody);
        final User userTo = User.getUserByLogin(email);
        if (userTo != null && userTo.hasBlockedContact(userFrom))
            validation.addError("userTo", Messages.get("you-are-blocked"));

        if (!validation.hasErrors())
        {
            userTo.unreadMessages = true;
            userTo.save();
            clearUserFromCache();
            Cache.delete(userTo.login);

            if (thread == null)
                thread = RandomUtil.getUUID();

            Message m = new Message();
            m.url = link;
            m.fromUser = userFrom;
            m.toUser = userTo;
            m.body = emailBody;
            m.subject = subject;
            m.thread = thread;
            m.isMessage = true;
            m.owner = userFrom;
            m.saveMessage();

            m = new Message();
            m.url = link;
            m.fromUser = userFrom;
            m.toUser = userTo;
            m.body = emailBody;
            m.subject = subject;
            m.thread = thread;
            m.isMessage = true;
            m.owner = userTo;
            m.saveMessage();

            flash.success(Messages.get("message-sent-successfully"));
            flash.keep();
            inbox(null, null);
        }

        params.put("subject", subject);
        params.put("toUser", toUser);
        params.put("emailBody", emailBody);
        params.flash();
        if (thread != null)
        {
            detail(thread);
        } else
        {
            String action = "new";
            renderTemplate("Notifications/inbox.html", action, user);
        }
    }

    public static void delete(String uuid, String url)
    {
        User user = getLoggedUser();
        Message message = Message.getById(uuid);

        if (user == null)
            forbidden();
        if (!user.equals(message.owner))
            forbidden();

        message.delete();
        redirectTo(url);
    }

    public static void inbox(String action, Integer from)
    {
        if (from == null)
            from = 0;

        User user = getLoggedUser();
        if (user.unreadMessages != null)
        {
            user = getLoggedUserNotCache();
            user.unreadMessages = null;
            user.save();
            Cache.delete(user.login);
        }

        List<Message> received = Message.getReceivedTo(user, from);
        List<Message> sent = Message.getSentBy(user, from);
        render(user, received, sent, action, from);
    }

    public static void detail(String id)
    {
        User user = getLoggedUser();
        if (user.unreadMessages != null)
        {
            user = getLoggedUserNotCache();
            user.unreadMessages = null;
            user.save();
            Cache.delete(user.login);
        }
        Message message = Message.getById(id);
        if (message == null)
            redirect("/mail");

        if (message.toUser.equals(user) && message.read == null)
        {
            message.read = true;
            message.save();
        }

        List<Message> thread = null;
        if (message.thread != null)
            thread = Message.getByThread(message.thread, user);
        else
            thread = Message.getByThread(id, user);

        renderTemplate("Notifications/inbox.html", user, thread, message);
    }
}