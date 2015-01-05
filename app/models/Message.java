package models;

import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
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
    @ManyToOne(cascade = CascadeType.ALL)
    public User fromUser;

    @ManyToOne(cascade = CascadeType.ALL)
    public User toUser;

    @Column(length = 1000)
    public String body;

    public String subject;
    public Boolean read;
    public Boolean replied;
    public Date created;
    public String uuid;
    public String thread;

    public static List<Message> getReceivedTo(User user, Integer from)
    {
        return Message.find("toUser = ? order by created desc", user).from(from).fetch(50);
    }

    public static List<Message> getSentBy(User user, Integer from)
    {
        return Message.find("fromUser = ? order by created desc", user).from(from).fetch(50);
    }

    public static List<Message> getByThread(String thread)
    {
        return Message.find("thread = ? or uuid = ? order by created asc", thread, thread).fetch(500);
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
        User user = BaseController.getAdmin();
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
            m.fromUser = user;
            m.toUser = toUser;
            m.created = new Date();
            m.body = body;
            m.save();

            Cache.delete(toUser.login);
        }
    }
}
