package controllers;

import models.Listing;
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
        r.uuid = RandomUtil.getUUID();
        r.comment = comment;
        r.saveRating();

        if (type.equals(Rating.TYPE_LISTING))
        {
            Listing listing = Listing.get(uuid);
            if (listing != null)
            {
                if (listing.ratingCount == null)
                    listing.ratingCount = 0;
                if (listing.ratingStars == null)
                    listing.ratingStars = 0;
                listing.ratingCount += 1;
                listing.ratingStars += stars;
                listing.save();
            }
        }
        redirectTo(url);
    }

    public static void deleteRating(String uuid, String url)
    {
        try
        {
            final Rating r = Rating.getByUuid(uuid);
            r.delete();

            if (r.type.equals(Rating.TYPE_LISTING))
            {
                Listing listing = Listing.get(r.objectUuid);
                if (listing != null)
                {
                    if (listing.ratingCount != null && listing.ratingCount > 0)
                    {
                        listing.ratingCount -= 1;
                    }
                    if (listing.ratingStars != null && listing.ratingStars > 0)
                    {
                        listing.ratingStars -= r.stars;
                    }
                    listing.save();
                }
            }

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