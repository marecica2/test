package models;

import java.util.Date;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Table;

import play.db.jpa.Model;

@Entity
@Table(name = "chat_feed")
public class ChatFeed extends Model
{
    public String uuid;
    public String name;
    public String comment;
    public Date created;
    public String sender;
    public String senderName;
    public String recipient;
    public String recipientName;

    public static List<ChatFeed> getByUuid(String uuid)
    {
        return ChatFeed.find("uuid = ? order by created desc", uuid).fetch(20);
    }

    public static List<ChatFeed> getBySenderRecipient(String sender, String recipient, Integer from, Integer max)
    {
        return ChatFeed.find("sender = ? and recipient = ? or sender = ? and recipient = ? or sender = ? and recipient is null order by created desc",
                sender, recipient, recipient, sender, sender).from(from).fetch(max);
    }

    public ChatFeed saveFeed()
    {
        this.created = new Date();
        ChatFeed a = this.save();
        return a;
    }

}
