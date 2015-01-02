package controllers;

import java.util.List;

import models.Message;
import models.User;
import play.cache.Cache;
import play.i18n.Messages;
import play.mvc.With;
import utils.StringUtils;

@With(Secure.class)
public class Notifications extends BaseController
{
    public static void send(String toUser, String subject, String emailBody, String url, String thread)
    {
        final User userFrom = getLoggedUser();
        String email = StringUtils.extract(toUser, "\\((.+?)\\)");
        if (email == null)
            email = toUser;

        validation.required("userTo", email);
        validation.required("subject", subject);
        validation.required("emailBody", emailBody);
        validation.email("userTo", email).message("invalid-user-name");

        if (!validation.hasErrors())
        {
            System.err.println(email);
            final User userTo = User.getUserByLogin(email);
            userTo.unreadMessages = true;
            userTo.save();
            clearUserFromCache();
            Cache.delete(userTo.login);

            Message m = new Message();
            m.fromUser = userFrom;
            m.toUser = userTo;
            m.body = emailBody;
            m.subject = subject;
            m.thread = thread;
            m.saveMessage();

            flash.success(Messages.get("message-sent-successfully"));
            flash.keep();
            inbox(null, null);
        }

        params.put("subject", subject);
        params.put("toUser", toUser);
        params.put("emailBody", emailBody);
        params.flash();
        validation.keep();
        if (thread != null)
        {
            detail(thread);
        } else
        {
            String action = "new";
            renderTemplate("Messages/inbox.html", action);
        }
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
        if (message.toUser.equals(user) && message.read == null)
        {
            message.read = true;
            message.save();
        }

        List<Message> thread = null;
        if (message.thread != null)
            thread = Message.getByThread(message.thread);
        else
            thread = Message.getByThread(id);

        renderTemplate("Messages/inbox.html", user, thread, message);
    }
}