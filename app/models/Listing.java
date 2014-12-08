package models;

import java.math.BigDecimal;
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

    public Long ratingAvg;

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

    public static List<Listing> getSearch(Integer first, Integer count, ListingFilter filter)
    {

        String query = "";
        query += " SELECT uuid, title,firstname, lastname, avatarUrl, "
                + "category, privacy, charging, price, currency, imageUrl, "
                + "tags, type, ratingStars, ratingAvg, login ";
        query += " FROM search_index ";
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
            filter.search = filter.search.replaceAll("\\s+", " & ");
            System.err.println("query string " + filter.search);
            query += " AND document @@ to_tsquery('english', :search) ";
        }

        if (filter.category != null && filter.category.equals("match"))
            query += " ORDER BY ts_rank(document, to_tsquery('english', :search)) DESC ";
        else
            query += " ORDER BY (ratingStars * ratingAvg) desc ";

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
            l.ratingStars = (Integer) item[13];
            l.ratingAvg = item[14] != null ? new Long(item[14].toString()) : null;
            u.login = (String) item[15];
            l.user = u;
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
        //String description = this.description.replaceAll("\\*(.+?)\\*", "<strong>$1</strong>");
        //this.description = description.replaceAll("\n", "<br/>");
        //return description;
        return WikiUtils.parseToHtml(this.description);
    }

    public Long getRatingAvg()
    {
        if (ratingAvg == null)
            return 0L;
        return ratingAvg;
    }

}
