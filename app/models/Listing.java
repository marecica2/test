package models;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import play.db.jpa.Model;
import utils.WikiUtils;

@Entity
public class Listing extends Model
{
    public static String IMAGE_DEFAULT = "public/images/channel_default";

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

    public String language;

    public Double ratingAvg;

    public Integer ratingStars;

    public Boolean chatEnabled;

    public Boolean commentsEnabled;

    public Boolean deleted;

    public Boolean firstFree;

    public Integer chargingTime;

    public Date started;

    public Boolean available;

    public Date ended;

    public String instantBroadcast;

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

    public static List<Listing> getSearch(Integer first, Integer count, ListingFilter filter)
    {

        String query = "";
        query += " SELECT s.uuid, title, s.firstname, s.lastname, s.avatarUrl, "
                + "s.category, s.privacy, s.charging, s.price, s.currency, s.imageUrl, "
                + "s.tags, s.type, s.ratingStars, s.ratingAvg, s.login, firstFree, s.description, u.available, u.lastOnlineTime, s.language ";
        query += " FROM search_index s ";
        query += " JOIN users u ON u.login = s.login ";
        query += " WHERE 1 = 1 ";

        if (filter.category != null)
        {
            query += " AND category = :category ";
        }

        if (filter.search != null)
        {
            filter.search = filter.search.trim();
            filter.search = filter.search.replaceAll("\\s+", " ");
            filter.search = filter.search.replaceAll("(\\b[^\\s]+\\b)", "$1:*");
            filter.search = filter.search.replaceAll("\\s+", " | ");
            query += " AND document @@ to_tsquery('english', :search) ";
        }
        if (filter.search != null && filter.sort != null && filter.sort.equals("match"))
        {
            query += " ORDER BY ts_rank(document, to_tsquery('english', :search)) DESC, ";
            query += " (ratingStars * ratingAvg) DESC NULLS LAST ";
        } else if (filter.sort != null && filter.sort.equals("availability"))
        {
            query += " ORDER BY u.available DESC NULLS LAST, (ratingStars * ratingAvg) DESC NULLS LAST";
        } else
        {
            query += " ORDER BY (ratingStars * ratingAvg) DESC NULLS LAST ";
        }

        Query q = Listing.em().createNativeQuery(query);
        if (filter.search != null)
            q.setParameter("search", filter.search);
        if (filter.category != null)
            q.setParameter("category", filter.category);

        q.setFirstResult(first);
        q.setMaxResults(count);
        List<Object> result = q.getResultList();
        List<Listing> listings = new LinkedList<Listing>();
        for (Object object : result)
        {
            Object[] item = (Object[]) object;
            Listing l = new Listing();
            User u = new User();
            l.uuid = (String) item[0];
            l.title = (String) item[1];
            u.firstName = (String) item[2];
            u.lastName = (String) item[3];
            u.avatarUrl = (String) item[4];
            l.category = (String) item[5];
            l.privacy = (String) item[6];
            l.charging = (String) item[7];
            l.price = (BigDecimal) item[8];
            l.currency = (String) item[9];
            l.imageUrl = (String) item[10];
            l.tags = (String) item[11];
            l.type = (String) item[12];
            final BigInteger stars = (BigInteger) item[13];
            l.ratingStars = stars != null ? stars.intValue() : null;

            if (item[14] != null && item[14].toString() != null)
            {
                BigDecimal avg = new BigDecimal(item[14].toString());
                l.ratingAvg = avg.doubleValue();
            }

            u.login = (String) item[15];
            l.firstFree = (Boolean) item[16];
            l.user = u;
            l.description = (String) item[17];
            u.available = item[18] != null ? true : false;
            u.lastOnlineTime = (Date) item[19];
            l.language = (String) item[20];
            listings.add(l);
        }
        return listings;
    }

    public static List<String> getTags(String q)
    {
        String query = "";
        query += " SELECT term ";
        query += " FROM tags ";
        query += " WHERE term like ? ";

        Query qr = Listing.em().createNativeQuery(query);
        qr.setParameter(1, "%" + q + "%");
        List<String> res = qr.getResultList();
        return res;
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
        return WikiUtils.parseToHtml(this.description);
    }

    public boolean isAvailable()
    {
        return this.user.isAvailable();
    }
}
