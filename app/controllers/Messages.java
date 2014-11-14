package controllers;

import java.util.List;

import models.Message;
import models.User;
import play.cache.Cache;
import play.mvc.With;

@With(Secure.class)
public class Messages extends BaseController
{
    public static void send(String toUser, String subject, String emailBody, String url, String thread)
    {
        final User userFrom = getLoggedUser();
        final User userTo = User.getUserByLogin(toUser);
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
        redirectTo(url);
    }

    public static void inbox(String action)
    {
        User user = getLoggedUser();
        if (user.unreadMessages != null)
        {
            user = getLoggedUserNotCache();
            user.unreadMessages = null;
            user.save();
            Cache.delete(user.login);
        }

        List<Message> received = Message.getReceivedTo(user);
        List<Message> sent = Message.getSentBy(user);
        render(user, received, sent, action);
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