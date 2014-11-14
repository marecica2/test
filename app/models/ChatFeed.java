package models;

import java.util.Date;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import play.db.jpa.Model;
import utils.RandomUtil;

@Entity
@Table(name = "chat_feed")
public class ChatFeed extends Model
{
    @ManyToOne
    public User user;

    @ManyToOne
    public User customer;

    public String event;
    public String name;
    public String account;
    public String comment;
    public String uuid;
    public Date created;

    public static List<ChatFeed> getByEvent(String event)
    {
        return ChatFeed.find("event = ? order by created desc", event).fetch(100);
    }

    public ChatFeed saveFeed()
    {
        this.created = new Date();
        this.uuid = RandomUtil.getDoubleUUID();
        ChatFeed a = this.save();
        return a;
    }

    @Override
    public String toString()
    {
        return "Comment [event=" + event + ", user=" + user + ", customer=" + customer + ", comment=" + comment + ", uuid=" + uuid + ", created=" + created + "]";
    }
}
