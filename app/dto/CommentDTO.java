package dto;

import java.util.ArrayList;
import java.util.List;

import models.Attendance;
import models.Comment;
import models.CommentReply;
import models.FileUpload;
import models.User;
import play.i18n.Messages;
import utils.WikiUtils;

public class CommentDTO
{
    public String uuid;
    public String comment;
    public String commentRaw;
    public long created;
    public String createdBy;
    public String createdByName;
    public String createdByLogin;
    public String createdByAvatarUrl;
    public String objectType;
    public String event;
    public long eventStart;
    public long eventEnd;
    public String listing;
    public Boolean paid;
    public String listingName;
    public String listingImage;
    public boolean isDeletable;
    public Boolean commentsEnabled;
    public List<AttachmentDTO> attachments = new ArrayList<AttachmentDTO>();
    public List<CommentReplyDTO> replies = new ArrayList<CommentReplyDTO>();

    public static CommentDTO convert(Comment com, User user)
    {
        CommentDTO c = new CommentDTO();
        c.uuid = com.uuid;
        c.comment = WikiUtils.parseToHtml(com.comment);
        c.commentRaw = com.comment;
        c.created = com.created.getTime();
        c.createdBy = com.user.uuid;
        c.createdByLogin = com.user.login;
        c.createdByName = com.user.getFullName();
        c.createdByAvatarUrl = com.user.getAvatarUrl();
        c.objectType = com.objectType;
        c.isDeletable = com.canDelete(user);
        c.paid = com.paid != null ? com.paid : null;

        if (com.event != null)
        {
            c.event = com.event.uuid;
            c.eventStart = com.event.eventStart.getTime();
            c.eventEnd = com.event.eventEnd.getTime();
            c.listingName = com.event.listing.title;
            c.listingImage = com.event.listing.imageUrl;
            c.commentsEnabled = com.event.commentsEnabled;
        }
        if (com.listing != null)
        {
            c.listing = com.listing.uuid;
            c.listingName = com.listing.title;
            c.listingImage = com.listing.imageUrl;
            c.commentsEnabled = com.listing.commentsEnabled;
        }

        for (FileUpload fu : com.files)
            c.attachments.add(new AttachmentDTO(fu.name, fu.contentType, fu.url, fu.size));
        for (CommentReply cr : com.replies)
            c.replies.add(CommentReplyDTO.convert(cr));

        if (com.paid != null && com.paid && com.event != null)
        {
            Attendance attendance = com.event.getInviteForCustomer(user);
            if ((attendance != null && attendance.paid != null && attendance.paid) || user.isOwner(com.event))
            {

            } else
            {
                c.comment = Messages.get("paid-comment-note");
                c.attachments = new ArrayList<AttachmentDTO>();
                c.replies = new ArrayList<CommentReplyDTO>();
            }
        }

        return c;
    }
}
