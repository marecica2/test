package models;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Query;
import javax.persistence.Transient;

import org.hibernate.annotations.Cascade;

import play.db.jpa.JPA;
import play.db.jpa.Model;

@Entity
public class Rating extends Model
{
    public static final String TYPE_LISTING = "listing";

    @ManyToOne
    public User user;

    public String type;
    @Transient
    public Long votes;

    public Integer stars;
    public String uuid;
    public Date created;
    public String objectUuid;
    public String userUuid;

    @Column(length = 1000)
    public String comment;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "rating", orphanRemoval = true)
    @Cascade({ org.hibernate.annotations.CascadeType.DELETE })
    public List<RatingVote> voteList = new ArrayList<RatingVote>();

    public static List<Rating> getByObject(String uuid)
    {

        Query query = JPA.em().createQuery(
                "select r,  COALESCE(sum(vote.vote),0) as sumVotes from Rating r "
                        + "left outer join r.voteList as vote where r.objectUuid = ? "
                        + "group by r order by COALESCE(sum(vote.vote), 0) desc nulls first");
        query.setParameter(1, uuid);
        List<Object[]> result = query.getResultList();
        List<Rating> ratings = new ArrayList<Rating>();
        for (Object[] objects : result)
        {
            final Rating rating = (Rating) objects[0];
            rating.votes = (Long) objects[1];
            ratings.add(rating);
        }
        return ratings;
    }

    public static List<Rating> getByObjectUser(String uuid, User user)
    {
        Query query = JPA.em().createQuery(
                "select r,  COALESCE(sum(vote.vote),0) as sumVotes from Rating r "
                        + "left outer join r.voteList as vote where r.objectUuid = ? and r.user = ? "
                        + "group by r order by r.created desc");
        query.setParameter(1, uuid);
        query.setParameter(2, user);

        List<Object[]> result = query.getResultList();

        List<Rating> ratings = new ArrayList<Rating>();
        for (Object[] objects : result)
        {
            final Rating rating = (Rating) objects[0];
            rating.votes = (Long) objects[1];
            ratings.add(rating);
        }
        return ratings;
    }

    public static List<Rating> getByUser(String uuid)
    {
        return Rating.find("userUuid = ? order by created desc", uuid).fetch();
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
        double sum = 0;

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
            final long avg = Math.round(sum / total);
            stats.put("avgStars", avg);
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

    public boolean hasVoted(User user)
    {
        for (RatingVote rv : this.voteList)
        {
            if (rv.user.equals(user))
                return true;
        }
        return false;
    }

}
