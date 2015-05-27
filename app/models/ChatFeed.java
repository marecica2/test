package models;

import java.util.Date;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.TypedQuery;

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
    public String listing;

    public static List<ChatFeed> getByUuid(String uuid)
    {
        return ChatFeed.find("uuid = ? order by created desc", uuid).fetch();
    }

    public static List<ChatFeed> getByUuid(String uuid, Integer from, Integer max)
    {
        return ChatFeed.find("uuid = ? and recipient is null order by created desc", uuid).from(from).fetch(max);
    }

    public static List<ChatFeed> getBySenderRecipient(String sender, String recipient, Integer from, Integer max, String listing)
    {
        String query = "from ChatFeed where sender = :s1 ";
        if (recipient != null)
            query += " and recipient = :r1 ";
        query += " or recipient = :r2 ";
        if (recipient != null)
            query += " and sender = :s2 ";
        if (listing != null)
            query += " and listing = :listing";
        query += " order by created desc ";

        TypedQuery<ChatFeed> q = ChatFeed.em().createQuery(query, ChatFeed.class);
        q.setParameter("s1", sender);
        q.setParameter("r2", sender);
        if (recipient != null)
            q.setParameter("r1", recipient);
        if (recipient != null)
            q.setParameter("s2", recipient);
        if (listing != null)
            q.setParameter("listing", listing);
        return q.setFirstResult(from).setMaxResults(max).getResultList();
    }

    public ChatFeed saveFeed()
    {
        this.created = new Date();
        ChatFeed a = this.save();
        return a;
    }

}
