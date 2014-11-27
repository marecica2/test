package models;

import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import play.db.jpa.Model;
import utils.RandomUtil;
import utils.WikiUtils;

@Entity
public class Message extends Model
{
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

    public static List<Message> getReceivedTo(User user)
    {
        return Message.find("toUser = ? order by created desc", user).fetch(500);
    }

    public static List<Message> getSentBy(User user)
    {
        return Message.find("fromUser = ? order by created desc", user).fetch(500);
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

}
