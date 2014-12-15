package models;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.TypedQuery;

import org.hibernate.annotations.Cascade;

import play.db.jpa.Model;

@Entity
public class Event extends Model
{
    public static String EVENT_TYPE_DASHBOARD_UPCOMING = "dashboard-upcoming";
    public static String EVENT_TYPE_DASHBOARD_PREVIOUS = "dashboard-previous";
    public static String EVENT_TYPE_PROFILE_UPCOMING = "profile-upcoming";
    public static String EVENT_TYPE_PROFILE_PREVIOUS = "profile-previous";
    public static String EVENT_TYPE_LISTING_PREVIOUS = "listing-previous";
    public static String EVENT_TYPE_LISTING_UPCOMING = "listing-upcoming";
    public static String EVENT_TYPE_CALENDAR = "calendar";

    public static String EVENT_TYPE_P2P_CALL = "p2p";
    public static String EVENT_TYPE_BROADCAST = "live";
    public static String EVENT_TYPE_INSTANT_BROADCAST = "instant";
    public static String EVENT_VISIBILITY_PUBLIC = "public";
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

    public Date started;

    public Date ended;

    public String roomSecret;

    public String state;

    public Date eventStart;

    public Date eventEnd;

    public Boolean createdByUser;

    public Boolean archived;

    public BigDecimal price;

    public String currency;

    public String charging;

    public String type;

    public String privacy;

    public Boolean chatEnabled;

    public Boolean commentsEnabled;

    public Boolean firstFree;

    public Integer chargingTime;

    public String youtubeId;

    public String hangoutUrl;

    public String googleId;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "event", orphanRemoval = true)
    @Cascade({ org.hibernate.annotations.CascadeType.DELETE })
    public List<Attendance> attendances = new ArrayList<Attendance>();

    @ManyToOne
    @JoinColumn(name = "listing_id")
    public Listing listing;

    public String listing_uuid;

    @ManyToOne
    @JoinColumn(name = "user_id")
    public User user;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    public User customer;

    public Event saveEvent()
    {
        this.created = new Date();
        Event event = this.save();
        return event;
    }

    public static Event get(String uuid)
    {
        Event event = Event.find("byUuid", uuid).first();
        return event;
    }

    public static List<Event> getWatchList(User user)
    {
        String query = "Select distinct ev from Event ev join ev.attendances att where (ev.user = :user or att.customer = :user )and watchlist = true order by ev.eventStart asc";
        TypedQuery<Event> q = Event.em().createQuery(query, Event.class);
        q.setParameter("user", user);
        List<Event> events = q.getResultList();
        return events;
    }

    public static List<Event> getApprovement(User user)
    {
        String query = "Select distinct ev from Event ev where user = :user and state = 'customer_created' order by ev.eventStart asc";
        TypedQuery<Event> q = Event.em().createQuery(query, Event.class);
        q.setParameter("user", user);
        List<Event> events = q.getResultList();
        return events;
    }

    public static List<Event> getBetween(Date from, Date to, User user, Boolean showArchived, Integer first, Integer count,
        String direction, String type, String listing)
    {
        boolean upcoming = "upcoming".equals(direction) ? true : false;
        boolean previous = "previous".equals(direction) ? true : false;
        boolean includeAttendances = "dashboard".equals(type) || "calendar".equals(type) || "request".equals(type) ? true : false;
        boolean isListing = listing != null ? true : false;

        if (upcoming)
        {
            to = null;
        }
        if (previous)
        {
            to = from;
            from = null;
        }

        String query = "Select distinct ev from Event ev left outer join ev.attendances att where 1 = 1 ";

        if (user != null)
        {
            query += " and ev.user = :user ";
        }

        if (!isListing && includeAttendances)
        {
            if (user != null)
                query += " or ";
            else
                query += " and ";

            query += " ( att.customer = :cust ";
            if (from != null)
                query += " and ev.eventStart >= :start ";
            if (to != null)
                query += " and ev.eventStart <= :end ";
            query += " ) or ";

            query += " ( ev.user = :cust ";
            if (from != null)
                query += " and ev.eventStart >= :start ";
            if (to != null)
                query += " and ev.eventStart <= :end ";
            query += " ) ";
        }

        if (isListing)
        {
            query += " and ev.listing_uuid  = :listing ";
            if (from != null)
                query += " and ev.eventStart >= :start ";
            if (to != null)
                query += " and ev.eventStart <= :end ";
        }

        if (upcoming)
        {
            query += " order by ev.eventStart asc";
        } else if (previous)
        {
            query += " order by ev.eventStart desc";
        } else
        {
            query += " order by ev.eventStart desc";
        }

        //Logger.error(query);
        TypedQuery<Event> q = Event.em().createQuery(query, Event.class);

        if (user != null)
            q.setParameter("user", user);
        if (from != null)
            q.setParameter("start", from);
        if (to != null)
            q.setParameter("end", to);
        if (includeAttendances && !isListing)
            q.setParameter("cust", user);
        if (isListing)
            q.setParameter("listing", listing);

        if (first != null && count != null)
        {
            q.setFirstResult(first);
            q.setMaxResults(count);
        }

        List<Event> events = q.getResultList();
        return events;
    }

    public Event deleteEvent()
    {
        // delete attendances
        this.attendances.clear();

        // delete comments
        final List<Comment> comments = Comment.getByEvent(this);
        for (Comment c : comments)
        {
            List<FileUpload> files = c.files;
            for (FileUpload fileUpload : files)
                FileUpload.deleteOnDisc(fileUpload);
            files.clear();
            c.delete();
        }

        List<Activity> act = Activity.getByEvent(this);
        for (Activity activity : act)
            activity.delete();

        this.delete();
        this.em().getTransaction().commit();
        return this;
    }

    @Override
    public String toString()
    {
        return "Event [uuid=" + uuid + ", eventStart=" + eventStart + ", eventEnd=" + eventEnd + "]";
    }

    public Attendance getInviteForCustomer(User user)
    {
        Attendance a = null;
        if (user == null)
            return null;

        for (Attendance attendance : this.attendances)
        {
            if (attendance.customer.equals(user))
            {
                a = attendance;
            }
        }
        return a;
    }

    public Integer getMinutes()
    {
        long diff = this.eventEnd.getTime() - this.eventStart.getTime();
        int minutes = Math.round(diff / (1000 * 60));
        return minutes;
    }

    public BigDecimal getTotalPrice()
    {
        if (this.chargingTime != null)
        {
            final BigDecimal divisor = new BigDecimal(this.chargingTime.toString(), MathContext.DECIMAL32);
            final BigDecimal unitPrice = this.price.divide(divisor, 32, RoundingMode.CEILING);
            final BigDecimal multiplicand = new BigDecimal(this.getMinutes().toString(), MathContext.DECIMAL32);
            BigDecimal totalPrice = unitPrice.multiply(multiplicand);
            totalPrice = totalPrice.setScale(2, BigDecimal.ROUND_HALF_DOWN);
            return totalPrice;
        }
        return null;
    }

    private boolean isEnded()
    {
        if (this.eventEnd.getTime() < System.currentTimeMillis())
            return true;
        return false;
    }

    public boolean hasInviteForCustomer(User user)
    {
        if (user == null)
            return false;
        if (getInviteForCustomer(user) != null)
            return true;
        return false;
    }

    public Boolean isOwner(User user)
    {
        return user != null && user.uuid.equals(this.user.uuid) ? true : false;
    }

    public Boolean isPrivate()
    {
        return this.privacy.equals(EVENT_VISIBILITY_PRIVATE);
    }

    public Boolean isPublic()
    {
        return this.privacy.equals(EVENT_VISIBILITY_PUBLIC);
    }

    public Boolean isFree()
    {
        return this.charging.equals(EVENT_CHARGING_FREE);
    }

    public Boolean isVisible(User user)
    {
        if (this.isPublic())
            return true;
        if (this.isOwner(user))
            return true;
        if (this.hasInviteForCustomer(user))
            return true;
        return false;
    }

    private boolean isLocked()
    {
        // TODO fix the return type later
        if (this.isEnded())
            return false;

        final List<Attendance> list = this.attendances;
        for (Attendance attendance : list)
        {
            if (attendance.paid != null && attendance.paid)
            {
                return false;
            }
        }
        return false;
    }

    public Boolean isEditable(User user)
    {
        if (this.isOwner(user) && !this.isLocked())
            return true;
        if (user != null && this.hasInviteForCustomer(user) && this.state.equals(EVENT_STATE_CUSTOMER_CREATED))
            return true;
        return false;
    }
}
