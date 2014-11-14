package models;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import play.db.jpa.Model;

@Entity
public class Rating extends Model
{
    public static final String TYPE_EVENT = "event";
    public static final String TYPE_USER = "user";
    public static final String TYPE_TOPIC = "topic";

    @ManyToOne
    public User user;

    public String type;
    public Integer votes;
    public Integer abuses;
    public Integer stars;
    public String uuid;
    public Date created;
    public String objectUuid;
    public String userUuid;

    @Column(length = 1000)
    public String comment;

    public static List<Rating> getByObject(String uuid)
    {
        return Rating.find("objectUuid = ? order by votes desc, created desc", uuid).fetch();
    }

    public static List<Rating> getByUser(String uuid)
    {
        return Rating.find("userUuid = ? order by votes desc, created desc", uuid).fetch();
    }

    public static Rating getByUuid(String uuid)
    {
        return Rating.find("uuid = ?", uuid).first();
    }

    public Rating saveRating()
    {
        this.created = new Date();
        Rating a = this.save();
        return a;
    }

    @Override
    public String toString()
    {
        return "Rating [user=" + user + ", comment=" + comment + ", uuid=" + uuid + ", created=" + created + "]";
    }

    public static Map<String, Object> calculateStats(final List<Rating> ratings)
    {
        Map<String, Object> stats = new HashMap<String, Object>();

        Integer fiveStars = 0;
        Integer fourStars = 0;
        Integer threeStars = 0;
        Integer twoStars = 0;
        Integer oneStars = 0;
        Integer total = ratings.size();
        Integer sum = 0;

        if (ratings != null && ratings.size() > 0)
        {
            for (Rating r : ratings)
            {
                if (r.stars.intValue() == 1)
                    oneStars++;
                if (r.stars.intValue() == 2)
                    twoStars++;
                if (r.stars.intValue() == 3)
                    threeStars++;
                if (r.stars.intValue() == 4)
                    fourStars++;
                if (r.stars.intValue() == 5)
                    fiveStars++;
                sum = sum + r.stars;
            }

            stats.put("totalStars", total);
            stats.put("avgStars", sum / total);
            stats.put("oneStars", oneStars);
            stats.put("oneStarsPercent", (oneStars * 1.0) / total * 100.0);
            stats.put("twoStars", twoStars);
            stats.put("twoStarsPercent", (twoStars * 1.0) / total * 100.0);
            stats.put("threeStars", threeStars);
            stats.put("threeStarsPercent", (threeStars * 1.0) / total * 100.0);
            stats.put("fourStars", fourStars);
            stats.put("fourStarsPercent", (fourStars * 1.0) / total * 100.0);
            stats.put("fiveStars", fiveStars);
            stats.put("fiveStarsPercent", (fiveStars * 1.0) / total * 100.0);
        }
        return stats;
    }
}
