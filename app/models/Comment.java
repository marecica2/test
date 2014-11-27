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
@Table(name = "event_comment")
public class Comment extends Model
{
    public static final String EVENT_COMMENT = "event";
    public static final String USER_COMMENT = "user";
    public static final String LISTING_COMMENT = "listing";

    public static final String TYPE_DEFAULT = "text";
    public static final String TYPE_FILE = "file";
    public static final String TYPE_LINK = "link";
    public static final String TYPE_GOOGLE_DOCS = "gdoc";

    @ManyToOne
    public User user;

    public String objectTarget;
    public String objectUuid;

    @Cascade({ org.hibernate.annotations.CascadeType.DELETE })
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    public List<FileUpload> files;

    @Column(length = 1000)
    public String url;

    @Column(length = 1000)
    public String comment;

    public String type;
    public String uuid;
    public Date created;

    public static List<Comment> getByObject(String uuid)
    {
        return Comment.find("byObjectUuid", uuid).fetch();
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
