package controllers;

import java.util.List;
import java.util.Map;

import models.Listing;
import models.Rating;
import models.RatingVote;
import models.User;
import play.mvc.With;
import utils.RandomUtil;

@With(Secure.class)
public class Ratings extends BaseController
{
    public static void addRating(String uuid, String comment, String type, Integer stars, String url, String userUuid)
    {
        System.err.println("rating add");

        final User user = getLoggedUser();
        final Listing listing = Listing.get(uuid);

        if (user == null)
            forbidden();

        if (listing != null)
        {
            if (listing.user.hasBlockedContact(user))
                forbidden();
        }

        final Rating r = new Rating();
        r.user = user;
        if (stars == null)
            stars = 1;
        else
            stars = stars + 1;
        r.stars = stars;
        r.objectUuid = uuid;
        r.userUuid = userUuid;
        r.type = type;
        r.uuid = RandomUtil.getUUID();
        r.comment = comment;
        r.saveRating();

        if (type.equals(Rating.TYPE_LISTING))
        {
            List<Rating> ratings = Rating.getByObject(r.objectUuid);
            Map<String, Object> stats = Rating.calculateStats(ratings);
            listing.ratingAvg = (Long) stats.get("avgStars");
            listing.ratingStars = (Integer) stats.get("totalStars");
            listing.save();
        }
        redirectTo(url);
    }

    public static void deleteRating(String uuid, String url)
    {
        final User user = getLoggedUser();
        if (user == null)
            forbidden();

        final Rating r = Rating.getByUuid(uuid);
        r.delete();

        if (r.type.equals(Rating.TYPE_LISTING))
        {
            List<Rating> ratings = Rating.getByObject(r.objectUuid);
            Map<String, Object> stats = Rating.calculateStats(ratings);
            Listing listing = Listing.get(r.objectUuid);
            listing.ratingAvg = (Long) stats.get("avgStars");
            listing.ratingStars = (Integer) stats.get("totalStars");
            listing.save();
        }
        redirectTo(url);
    }

    public static void voteForRating(String uuid, String url)
    {
        final User user = getLoggedUser();
        if (user == null)
            forbidden();

        final Rating r = Rating.getByUuid(uuid);
        RatingVote rv = new RatingVote();
        rv.user = user;
        rv.rating = r;
        rv.vote = 1;
        rv.save();
        redirectTo(url);
    }

    public static void unvoteForRating(String uuid, String url)
    {
        final User user = getLoggedUser();
        if (user == null)
            forbidden();

        final Rating r = Rating.getByUuid(uuid);
        RatingVote rv = new RatingVote();
        rv.user = user;
        rv.rating = r;
        rv.vote = -1;
        rv.save();
        redirectTo(url);
    }

    public static void abuseForRating(String uuid, String url)
    {
        final Rating r = Rating.getByUuid(uuid);
        r.save();
        redirectTo(url);
    }
}