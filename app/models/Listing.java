package models;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.TypedQuery;

import play.db.jpa.Model;
import utils.WikiUtils;

@Entity
public class Listing extends Model
{
    public static String EVENT_TYPE_DASHBOARD_UPCOMING = "dashboard-upcoming";
    public static String EVENT_TYPE_DASHBOARD_PREVIOUS = "dashboard-previous";
    public static String EVENT_TYPE_PROFILE_UPCOMING = "profile-upcoming";
    public static String EVENT_TYPE_PROFILE_PREVIOUS = "profile-previous";
    public static String EVENT_TYPE_CALENDAR = "calendar";

    public static String EVENT_TYPE_P2P_CALL = "p2p";
    public static String EVENT_TYPE_BROADCAST = "live";
    public static String EVENT_VISIBILITY_PUBLIC = "public";
    public static String EVENT_VISIBILITY_HIDDEN = "hidden";
    public static String EVENT_VISIBILITY_PRIVATE = "private";
    public static String EVENT_STATE_USER_CREATED = "user_created";
    public static String EVENT_STATE_CUSTOMER_CREATED = "customer_created";
    public static String EVENT_STATE_USER_ACCEPTED = "user_accepted";
    public static String EVENT_STATE_USER_DECLINED = "user_declined";

    public static String EVENT_CHARGING_FREE = "free";
    public static String EVENT_CHARGING_BEFORE = "before";
    public static String EVENT_CHARGING_AFTER = "after";

    public String uuid;

    public Date lastModified;

    public Date created;

    public String roomSecret;

    public BigDecimal price;

    public String currency;

    public String charging;

    public String title;

    public String imageUrl;

    public String imageId;

    public String video;

    @Column(length = 1500)
    public String description;

    public String color;

    public String state;

    public String category;

    public String tags;

    public String type;

    public String privacy;

    public Integer ratingCount;

    public Integer ratingStars;

    public Boolean chatEnabled;

    public Boolean commentsEnabled;

    public Boolean deleted;

    public Boolean firstFree;

    public Integer chargingTime;

    @ManyToOne
    @JoinColumn(name = "user_id")
    public User user;

    public static Listing get(String uuid)
    {
        Listing event = Listing.find("byUuid", uuid).first();
        return event;
    }

    public static List<Listing> getAll(Account account)
    {
        return Listing.findAll();
    }

    public static List<Listing> getForUser(User user)
    {
        return Listing.find("from Listing where user = ? and deleted is null", user).fetch(500);
    }

    public static List<Listing> getFiltered(Integer first, Integer count, ListingFilter listing)
    {
        String query = "Select l from Listing l where deleted is null ";
        if (listing.category != null)
            query += " and l.category  = :category ";
        if (listing.search != null)
            query += " and CONCAT(l.title, l.tags) like :search ";

        query += " order by l.created desc";
        //Logger.error(query);
        TypedQuery<Listing> q = Listing.em().createQuery(query, Listing.class);

        if (listing.category != null)
            q.setParameter("category", listing.category);
        if (listing.search != null)
            q.setParameter("search", "%" + listing.search + "%");

        if (first != null && count != null)
        {
            q.setFirstResult(first);
            q.setMaxResults(count);
        }

        List<Listing> listings = q.getResultList();
        return listings;
    }

    public String getDescriptionHtml()
    {
        //String description = this.description.replaceAll("\\*(.+?)\\*", "<strong>$1</strong>");
        //this.description = description.replaceAll("\n", "<br/>");
        //return description;
        return WikiUtils.parseToHtml(this.description);
    }

    public Integer getRatingAvg()
    {
        if (ratingCount == null || ratingStars == null)
            return 0;
        float sum = ratingStars;
        return Math.round(sum / ratingCount);
    }

}
