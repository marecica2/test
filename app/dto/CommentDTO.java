package dto;

import java.util.ArrayList;
import java.util.List;

import models.Comment;
import models.CommentReply;
import models.FileUpload;
import models.User;
import utils.WikiUtils;

public class CommentDTO
{
    public String uuid;
    public String comment;
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
    public String listingName;
    public String listingImage;
    public boolean isDeletable;
    public List<AttachmentDTO> attachments = new ArrayList<AttachmentDTO>();
    public List<CommentReplyDTO> replies = new ArrayList<CommentReplyDTO>();

    public static CommentDTO convert(Comment com, User user)
    {
        CommentDTO c = new CommentDTO();
        c.uuid = com.uuid;
        c.comment = WikiUtils.parseToHtml(com.comment);
        c.created = com.created.getTime();
        c.createdBy = com.user.uuid;
        c.createdByLogin = com.user.login;
        c.createdByName = com.user.getFullName();
        c.createdByAvatarUrl = com.user.getAvatarUrl();
        c.objectType = com.objectType;
        c.isDeletable = com.canDelete(user);
        if (com.event != null)
        {
            c.event = com.event.uuid;
            c.eventStart = com.event.eventStart.getTime();
            c.eventEnd = com.event.eventEnd.getTime();
            c.listingName = com.event.listing.title;
            c.listingImage = com.event.listing.imageUrl;
        }
        if (com.listing != null)
        {
            c.listing = com.listing.uuid;
            c.listingName = com.listing.title;
            c.listingImage = com.listing.imageUrl;
        }

        for (FileUpload fu : com.files)
            c.attachments.add(new AttachmentDTO(fu.name, fu.contentType, fu.url, fu.size));
        for (CommentReply cr : com.replies)
            c.replies.add(CommentReplyDTO.convert(cr));

        return c;
    }
}
