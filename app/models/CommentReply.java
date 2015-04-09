package models;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import play.db.jpa.Model;

@Entity
@Table(name = "comment_reply")
public class CommentReply extends Model
{
    public Date created;

    @ManyToOne(cascade = CascadeType.ALL)
    public User user;

    @Column(length = 600)
    public String comment;

    @Column(length = 40)
    public String uuid;

    public static CommentReply get(String uuid)
    {
        CommentReply cr = CommentReply.find("byUuid", uuid).first();
        return cr;
    }

}
