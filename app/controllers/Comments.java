package controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import models.Activity;
import models.Comment;
import models.CommentReply;
import models.Event;
import models.FileUpload;
import models.Listing;
import models.User;

import org.apache.commons.lang.StringEscapeUtils;

import utils.JsonUtils;
import utils.RandomUtil;
import utils.StringUtils;

import com.google.gson.JsonObject;

import dto.CommentDTO;

public class Comments extends BaseController
{
    public static void getComments(String type, String listing, String event)
    {
        final User user = getLoggedUser();
        final Integer first = request.params.get("first") != null ? Integer.parseInt(request.params.get("first")) : 0;
        final Integer count = request.params.get("count") != null ? Integer.parseInt(request.params.get("count")) : 4;

        List<Comment> comments = null;
        if (type.equals("event"))
        {
            Event e = Event.get(event);
            comments = Comment.getByEvent(e, first, count);
        }
        else if (type.equals("listing"))
        {
            Listing l = Listing.get(listing);
            comments = Comment.getByListing(l, first, count);
        }
        else if (type.equals("user") && user != null)
        {
            comments = Comment.getByFollower(user, first, count);
        }

        List<CommentDTO> commentsDto = new ArrayList<CommentDTO>();
        for (Comment comment : comments)
            commentsDto.add(CommentDTO.convert(comment, user));

        renderJSON(commentsDto);
    }

    public static void addReply()
    {
        checkAuthenticity();
        final JsonObject body = JsonUtils.getJson(request.body);
        final User user = getLoggedUser();
        final String id = body.get("id").getAsString();
        final String comment = body.get("comment").getAsString();

        if (user == null)
            forbidden();

        Comment c = Comment.getByUuid(id);
        if (c == null)
            forbidden();

        if (c.user.hasBlockedContact(user))
            forbidden();

        if (user != null)
        {
            CommentReply cr = new CommentReply();
            cr.comment = StringUtils.htmlEscape(comment);
            cr.user = user;
            cr.created = new Date();
            cr.uuid = RandomUtil.getUUID();
            cr.save();

            if (c.event != null)
            {
                final Activity act = new Activity();
                act.type = Activity.ACTIVITY_EVENT_COMMENTED;
                act.user = user;
                act.event = c.event;
                act.eventName = c.event.listing.title;
                act.saveActivity();
            }

            c.replies.add(cr);
            c.updated = new Date();
            c.save();
        }
        renderJSON("{\"response\":\"ok\"}");
    }

    public static void deleteReply(String uuid)
    {
        checkAuthenticity();
        final User user = getLoggedUser();
        if (user != null)
        {
            CommentReply cr = CommentReply.get(uuid);
            if (cr != null)
                cr.delete();
        }
        renderJSON("{\"response\":\"ok\"}");
    }

    public static void addComment() throws IOException
    {
        checkAuthenticity();
        final JsonObject jo = JsonUtils.getJson(request.body);
        final String comment = JsonUtils.getString(jo, "comment");
        final String tempId = JsonUtils.getString(jo, "tempId");
        final String type = JsonUtils.getString(jo, "type");
        final String objectType = JsonUtils.getString(jo, "objectType");
        final String uuid = JsonUtils.getString(jo, "uuid");
        final Boolean paid = Boolean.parseBoolean(JsonUtils.getString(jo, "paid"));

        final User user = getLoggedUser();
        if (user == null)
            forbidden();

        final Comment c = new Comment();
        c.user = user;
        c.paid = paid;
        c.comment = StringEscapeUtils.escapeHtml(comment);
        c.uuid = tempId != null ? tempId : RandomUtil.getUUID();
        c.type = type;
        c.objectType = objectType;

        if (objectType.equals(Comment.COMMENT_EVENT))
        {
            final Event e = Event.get(uuid);
            c.event = e;

            if (e.user.hasBlockedContact(user))
                forbidden();

            final Activity act = new Activity();
            act.type = Activity.ACTIVITY_EVENT_COMMENTED;
            act.user = user;
            act.event = e;
            act.eventName = e.listing.title;
            act.saveActivity();
        }

        if (objectType.equals(Comment.COMMENT_LISTING))
        {
            final Listing l = Listing.get(uuid);
            c.listing = l;

            if (l.user.hasBlockedContact(user))
                forbidden();
        }

        if (objectType.equals(Comment.COMMENT_USER))
        {
            final User u = User.getUserByUUID(uuid);
            c.user = u;
        }

        if (type != null && type.equals(Comment.TYPE_FILE))
        {
            List<FileUpload> fu = FileUpload.getByTemp(tempId);
            c.files = fu;
            for (FileUpload fileUpload2 : fu)
                fileUpload2.stored = true;
        }
        c.saveComment();
        renderJSON("{\"response\":\"ok\"}");
    }

    public static void deleteComment(String uuid, String url)
    {
        checkAuthenticity();
        final User user = getLoggedUser();
        Comment c = Comment.getByUuid(uuid);
        if (c.canDelete(user))
        {
            List<FileUpload> files = c.files;
            for (FileUpload fileUpload : files)
            {
                FileUpload.deleteOnDisc(fileUpload);
            }
            c.delete();
        }
        renderText("ok");
    }
}