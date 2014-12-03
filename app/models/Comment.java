package models;

import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.Cascade;

import play.db.jpa.Model;

@Entity
@Table(name = "comment")
public class Comment extends Model
{
    public static final String COMMENT_EVENT = "event";
    public static final String COMMENT_LISTING = "listing";
    public static final String COMMENT_USER = "user";
    public static final String TYPE_DEFAULT = "text";
    public static final String TYPE_LINK = "link";
    public static final String TYPE_FILE = "file";
    public static final String TYPE_GOOGLE_DOCS = "gdoc";

    public Date created;

    @ManyToOne
    public User user;

    @ManyToOne
    public Event event;

    @ManyToOne
    public Listing listing;

    @Column(length = 1000)
    public String url;

    @Column(length = 1000)
    public String comment;

    @Column(length = 10)
    public String type;

    @Column(length = 20)
    public String uuid;

    @Column(length = 10)
    public String objectType;

    @Cascade({ org.hibernate.annotations.CascadeType.DELETE })
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    public List<FileUpload> files;

    public static List<Comment> getByEvent(Event event)
    {
        return Comment.find("from Comment where event = ? order by created desc", event).fetch();
    }

    public static List<Comment> getByListing(Listing listing)
    {
        return Comment.find("from Comment where listing = ? order by created desc", listing).fetch();
    }

    public static List<Comment> getByFollower(User user, Integer results)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(" select comment from Comment comment left outer join Comment.event e left outer join e.attendances as a where (comment.user in ");
        sb.append(" (select contact.contact from Contact contact where contact.following = true and contact.user = ? ) ");
        sb.append(" and (comment.objectType != 'event' or e.privacy = 'public' or (e.privacy = 'private' and a.customer = ?) ) ");
        sb.append(" ) or comment.user = ? ");
        sb.append(" order by comment.created desc ");
        return Comment.find(sb.toString(), user, user, user).fetch(results);
    }

    public static Comment getByUuid(String uuid)
    {
        return Comment.find("byUuid", uuid).first();
    }

    public Comment saveComment()
    {
        this.created = new Date();
        Comment a = this.save();
        return a;
    }

    @Override
    public String toString()
    {
        return "Comment [user=" + user + ", comment=" + comment + ", uuid=" + uuid + ", created=" + created + "]";
    }

}
