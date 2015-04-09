package models;

import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.TypedQuery;

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
    public static final String TYPE_LIBRARY = "library";
    public static final String TYPE_GOOGLE_DOCS = "gdoc";

    public Date created;
    public Date updated;

    @ManyToOne(cascade = CascadeType.ALL)
    public User user;

    @ManyToOne(cascade = CascadeType.ALL)
    public Event event;

    @ManyToOne
    public Listing listing;

    @Column(length = 1000)
    public String url;

    @Column(length = 1000)
    public String comment;

    @Column(length = 10)
    public String type;

    @Column(length = 40)
    public String uuid;

    @Column(length = 10)
    public String objectType;

    public Boolean paid;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "comment_upload", joinColumns = @JoinColumn(name = "comment_id"), inverseJoinColumns = @JoinColumn(name = "fileupload_id"))
    public List<FileUpload> files;

    @Cascade({ org.hibernate.annotations.CascadeType.DELETE })
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    public List<CommentReply> replies;

    public static List<Comment> getByEvent(Event event)
    {
        return Comment.find("from Comment where event = ? order by created desc", event).fetch();
    }

    public static List<Comment> getByListing(Listing listing)
    {
        return Comment.find("from Comment where listing = ? order by created desc", listing).fetch();
    }

    public static List<Comment> getByEvent(Event event, Integer first, Integer count)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("from Comment where event = :event order by created desc");

        TypedQuery<Comment> q = Comment.em().createQuery(sb.toString(), Comment.class);
        q.setParameter("event", event);

        q.setFirstResult(first);
        q.setMaxResults(count);
        return q.getResultList();
    }

    public static List<Comment> getByListing(Listing listing, Integer first, Integer count)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("from Comment where listing = :listing order by created desc");

        TypedQuery<Comment> q = Comment.em().createQuery(sb.toString(), Comment.class);
        q.setParameter("listing", listing);

        q.setFirstResult(first);
        q.setMaxResults(count);
        return q.getResultList();
    }

    public static List<Comment> getByFollower(User user, Integer first, Integer count)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(" select distinct comment from Comment comment left outer join Comment.event e left outer join e.attendances as a where (comment.user in ");
        sb.append(" (select contact.contact from Contact contact where contact.following = true and contact.user = :user and (contact.blocked is null or contact.blocked = false) ) ");
        sb.append(" and (comment.objectType != 'event' or e.privacy = 'public' or (e.privacy = 'private' and a.customer = :user) ) ");
        sb.append(" ) or comment.user = :user ");
        sb.append(" order by comment.updated desc ");

        TypedQuery<Comment> q = Comment.em().createQuery(sb.toString(), Comment.class);
        q.setParameter("user", user);

        q.setFirstResult(first);
        q.setMaxResults(count);

        return q.getResultList();
    }

    public static Comment getByUuid(String uuid)
    {
        return Comment.find("byUuid", uuid).first();
    }

    public Comment saveComment()
    {
        this.created = new Date();
        this.updated = new Date();
        Comment a = this.save();
        return a;
    }

    @Override
    public String toString()
    {
        return "Comment [user=" + user + ", comment=" + comment + ", uuid=" + uuid + ", created=" + created + "]";
    }

    public boolean canDelete(User user)
    {
        if (user == null)
            return false;
        if (user.equals(this.user))
            return true;
        if (this.event != null && this.event.user.equals(user))
            return true;
        if (this.listing != null && this.listing.user.equals(user))
            return true;
        return false;
    }
}
