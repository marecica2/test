package controllers;

import models.Rating;
import models.User;
import play.mvc.With;
import utils.RandomUtil;

@With(Secure.class)
public class Ratings extends BaseController
{
    public static void addRating(String uuid, String comment, String type, Integer stars, String url, String userUuid)
    {
        final User user = getLoggedUser();
        final Rating r = new Rating();
        r.user = user;
        if (stars == null)
            stars = 1;
        else
            stars = stars + 1;
        r.stars = stars;
        r.objectUuid = uuid;
        r.userUuid = userUuid;
        r.votes = 0;
        r.abuses = 0;
        r.type = type;
        r.uuid = RandomUtil.getDoubleUUID();
        r.comment = comment;
        r.saveRating();
        redirectTo(url);
    }

    public static void deleteRating(String uuid, String url)
    {
        try
        {
            final Rating r = Rating.getByUuid(uuid);
            r.delete();
            redirectTo(url);
        } catch (Exception e)
        {
            redirectTo(url);
        }
    }

    public static void voteForRating(String uuid, String url)
    {
        final Rating r = Rating.getByUuid(uuid);
        if (r.votes == null)
            r.votes = 1;
        else
            r.votes = r.votes + 1;
        r.save();
        redirectTo(url);
    }

    public static void unvoteForRating(String uuid, String url)
    {
        final Rating r = Rating.getByUuid(uuid);
        if (r.votes == null)
            r.votes = 0;
        else
            r.votes = r.votes - 1;
        r.save();
        redirectTo(url);
    }

    public static void abuseForRating(String uuid, String url)
    {
        final Rating r = Rating.getByUuid(uuid);
        if (r.abuses == null)
            r.abuses = 1;
        else
            r.abuses = r.abuses + 1;
        r.save();
        redirectTo(url);
    }
}