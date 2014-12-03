package controllers;

import java.util.List;

import models.Activity;
import models.Comment;
import models.Event;
import models.FileUpload;
import models.Listing;
import models.User;

import org.apache.commons.lang.StringEscapeUtils;

import play.mvc.With;
import utils.RandomUtil;

@With(Secure.class)
public class Comments extends BaseController
{
    public static void addComment(String uuid, String objectType, String comment, String type, String url, String tempId)
    {
        final User user = getLoggedUser();

        final Comment c = new Comment();
        c.user = user;
        c.comment = StringEscapeUtils.escapeHtml(comment);
        c.uuid = tempId != null ? tempId : RandomUtil.getUUID();
        c.type = type;
        c.objectType = objectType;

        System.err.println(type);

        if (objectType.equals(Comment.COMMENT_EVENT))
        {
            final Event e = Event.get(uuid);
            c.event = e;

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
        }

        if (objectType.equals(Comment.COMMENT_USER))
        {
            final User u = User.getUserByUUID(uuid);
            c.user = u;
        }

        if (type.equals(Comment.TYPE_FILE))
        {
            List<FileUpload> fu = FileUpload.getByTemp(tempId);
            c.files = fu;
            for (FileUpload fileUpload2 : fu)
                fileUpload2.stored = true;
        }

        c.saveComment();
        redirectTo(url);
    }

    public static void deleteComment(String uuid, String url)
    {
        try
        {
            Comment c = Comment.getByUuid(uuid);
            List<FileUpload> files = c.files;
            for (FileUpload fileUpload : files)
            {
                FileUpload.deleteOnDisc(fileUpload);
                // fileUpload.delete();
            }
            c.delete();
            redirectTo(url);
        } catch (Exception e)
        {
            redirectTo(url);
        }
    }
}