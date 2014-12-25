package dto;

import models.CommentReply;
import play.db.jpa.Model;

public class CommentReplyDTO extends Model
{
    public String uuid;
    public long created;
    public String comment;
    public String createdBy;
    public String createdByName;
    public String createdByAvatarUrl;

    public static CommentReplyDTO convert(CommentReply cr)
    {
        CommentReplyDTO c = new CommentReplyDTO();
        c.uuid = cr.uuid;
        c.created = cr.created.getTime();
        c.comment = cr.comment;
        c.createdBy = cr.user.uuid;
        c.createdByName = cr.user.getFullName();
        c.createdByAvatarUrl = cr.user.getAvatarUrl();
        return c;
    }
}
