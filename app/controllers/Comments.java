package controllers;

import java.util.List;

import models.Activity;
import models.Comment;
import models.Event;
import models.FileUpload;
import models.User;

import org.apache.commons.lang.StringEscapeUtils;

import play.mvc.With;
import utils.RandomUtil;

@With(Secure.class)
public class Comments extends BaseController
{
    public static void addComment(String uuid, String comment, String type, String url, String fileUpload, String tempId)
    {
        final Event e = Event.get(uuid);
        final User u = User.getUserByUUID(uuid);
        final User user = getLoggedUser();

        final Comment c = new Comment();
        c.user = user;
        c.comment = StringEscapeUtils.escapeHtml(comment);
        c.uuid = tempId != null ? tempId : RandomUtil.getDoubleUUID();
        c.objectUuid = uuid;
        if (u != null)
            c.objectTarget = Comment.USER_COMMENT;
        if (e != null)
            c.objectTarget = Comment.EVENT_COMMENT;

        c.url = url;
        c.type = Comment.TYPE_DEFAULT;
        if (type != null && type.equals(Comment.TYPE_FILE))
        {
            List<FileUpload> fu = FileUpload.getByTemp(tempId);
            c.files = fu;
            System.err.println("uploaded files " + fu.size());
            System.err.println("uploaded files " + fu.size());
            System.err.println("uploaded files " + fu.size());
            for (FileUpload fileUpload2 : fu)
                fileUpload2.stored = true;
        }
        if (type != null && type.equals(Comment.TYPE_LINK))
            c.type = Comment.TYPE_LINK;
        if (type != null && type.equals(Comment.TYPE_GOOGLE_DOCS))
            c.type = Comment.TYPE_GOOGLE_DOCS;
        c.saveComment();

        if (e != null)
        {
            final Activity act = new Activity();
            act.type = Activity.ACTIVITY_EVENT_COMMENTED;
            act.user = user;
            act.event = e;
            act.eventName = e.listing.title;
            act.saveActivity();
        }
        redirectTo(url);
    }

    public static void deleteComment(String uuid, String url)
    {
        try
        {
            Comment c = Comment.getByUuid(uuid);
            List<FileUpload> files = c.files;
            for (FileUpload fileUpload : files)
                FileUpload.deleteOnDisc(fileUpload);
            c.delete();
            redirectTo(url);
        } catch (Exception e)
        {
            redirectTo(url);
        }
    }
}