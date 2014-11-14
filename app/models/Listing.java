package models;

import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.TypedQuery;

import play.db.jpa.Model;

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
    //public static String EVENT_STATE_USER_APPROVEMENT_WAIT = "user_approvement_wait";
    //public static String EVENT_STATE_CUSTOMER_APPROVEMENT_WAIT = "customer_approvement_wait";
    //public static String EVENT_STATE_CUSTOMER_ACCEPTED = "customer_accepted";
    //public static String EVENT_STATE_USER_DECLINED = "user_declined";
    //public static String EVENT_STATE_CUSTOMER_DECLINED = "customer_declined";
    //public static String EVENT_STATE_USER_CANCELED = "user_canceled";
    //public static String EVENT_STATE_CUSTOMER_CANCELED = "customer_canceled";
    public static String EVENT_CHARGING_FREE = "free";
    public static String EVENT_CHARGING_BEFORE = "before";
    public static String EVENT_CHARGING_AFTER = "after";

    public String uuid;

    public Date lastModified;

    public Date created;

    public String roomSecret;

    public String price;

    public String currency;

    public String charging;

    public String title;

    public String imageUrl;

    public String imageId;

    @Column(length = 1500)
    public String description;

    public String color;

    public String state;

    public String category;

    public String tags;

    public String type;

    public String privacy;

    public Boolean chatEnabled;

    @ManyToOne
    @JoinColumn(name = "account_id")
    public Account account;

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
        return Listing.find("byUser", user).fetch(500);
    }

    public static List<Listing> getFiltered(Integer first, Integer count, Listing listing)
    {
        String query = "Select l from Listing l where 1 = 1 ";
        if (listing.category != null)
            query += " and l.category  = :category ";

        query += " order by l.created desc";
        //Logger.error(query);
        TypedQuery<Listing> q = Listing.em().createQuery(query, Listing.class);

        if (listing.category != null)
            q.setParameter("category", listing.category);

        if (first != null && count != null)
        {
            q.setFirstResult(first);
            q.setMaxResults(count);
        }

        List<Listing> listings = q.getResultList();
        return listings;
    }

    public String getDescription()
    {
        String description = this.description.replaceAll("\\*(.+?)\\*", "<strong>$1</strong>");
        this.description = description.replaceAll("\n", "<br/>");
        return description;
    }

}
