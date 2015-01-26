package models;

import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import play.cache.Cache;
import play.db.jpa.Model;
import play.i18n.Messages;
import utils.RandomUtil;
import utils.WikiUtils;
import controllers.BaseController;

@Entity
public class Message extends Model
{
    @ManyToOne
    public User owner;

    @ManyToOne
    public User fromUser;

    @ManyToOne
    public User toUser;

    @Column(length = 1000)
    public String body;

    public String subject;
    public Boolean read;
    public Boolean replied;
    public Date created;
    public String uuid;
    public String thread;
    public Boolean isMessage;

    public static List<Message> getReceivedTo(User user, Integer from)
    {
        return Message.find("toUser = ? and owner = ? order by created desc", user, user).from(from).fetch(50);
    }

    public static List<Message> getSentBy(User user, Integer from)
    {
        return Message.find("fromUser = ? and owner = ? and isMessage = true order by created desc", user, user).from(from).fetch(50);
    }

    public static List<Message> getByThread(String thread, User user)
    {
        return Message.find("thread = ? and owner = ? order by created asc", thread, user).fetch(500);
    }

    public Message saveMessage()
    {
        this.created = new Date();
        this.uuid = RandomUtil.getUUID();
        Message m = this.save();
        return m;
    }

    public static Message getById(String id)
    {
        return Message.find("uuid = ? order by created desc", id).first();
    }

    public String getHtmlBody()
    {
        return WikiUtils.parseToHtml(this.body);
    }

    public static void createAdminNotification(User toUser, String subject, String body)
    {
        User user = BaseController.getAdminUser();
        body = body + Messages.getMessage(toUser.locale, "regards");
        createNotification(user, toUser, subject, body);
    }

    public static void createNotification(User user, User toUser, String subject, String body)
    {
        if (toUser != null && user != null)
        {
            toUser.refresh();
            toUser.unreadMessages = true;
            toUser.save();

            Message m = new Message();
            m.subject = subject;
            m.uuid = RandomUtil.getUUID();
            m.thread = RandomUtil.getUUID();
            m.fromUser = user;
            m.toUser = toUser;
            m.created = new Date();
            m.body = body;
            m.owner = toUser;
            m.save();

            Cache.delete(toUser.login);
        }
    }
}
