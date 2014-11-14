package models;

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

    public String state;

    public Date eventStart;

    public Date eventEnd;

    public Boolean createdByUser;

    public Boolean archived;

    public String price;

    public String currency;

    public String charging;

    public String type;

    public String privacy;

    public Boolean chatEnabled;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "event", orphanRemoval = true)
    @Cascade({ org.hibernate.annotations.CascadeType.DELETE })
    public List<Attendance> attendances = new ArrayList<Attendance>();

    @ManyToOne
    @JoinColumn(name = "account_id")
    public Account account;

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

    public Event save(Account account)
    {
        this.created = new Date();
        this.account = account;
        Event event = this.save();
        return event;
    }

    public static Event get(String uuid, Account account)
    {
        Event event = Event.find("byUuidAndAccount", uuid, account).first();
        return event;
    }

    public static Event get(String uuid)
    {
        Event event = Event.find("byUuid", uuid).first();
        return event;
    }

    public static Event getByRoomSecret(String uuid)
    {
        Event event = Event.find("byRoomSecret", uuid).first();
        return event;
    }

    public static List<Event> getAll(Account account)
    {
        return Event.findAll();
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
        q.setMaxResults(200);
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

        String query = "Select distinct ev from Event ev join ev.attendances att where 1 = 1 ";

        if (includeAttendances && !isListing)
        {
            query += " and ( att.customer = :cust ";
            if (from != null)
                query += " and ev.eventStart >= :start ";
            if (to != null)
                query += " and ev.eventEnd <= :end ";
            query += " ) or ";

            query += " ( ev.user = :cust ";
            if (from != null)
                query += " and ev.eventStart >= :start ";
            if (to != null)
                query += " and ev.eventEnd <= :end ";
            query += " ) ";
        }

        if (isListing)
        {
            query += " and ev.listing_uuid  = :listing ";
            if (from != null)
                query += " and ev.eventStart >= :start ";
            if (to != null)
                query += " and ev.eventEnd <= :end ";
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
        final List<Comment> comments = Comment.getByObject(uuid);
        for (Comment c : comments)
        {
            List<FileUpload> files = c.files;
            for (FileUpload fileUpload : files)
                FileUpload.deleteOnDisc(fileUpload);
            files.clear();
            c.delete();
        }

        //        // delete event image
        //        FileUpload fu = FileUpload.getByUuid(this.imageId);
        //        if (fu != null)
        //        {
        //            FileUpload.deleteOnDisc(fu);
        //            fu.delete();
        //        }

        List<Activity> act = Activity.getByEvent(this);
        for (Activity activity : act)
            activity.delete();

        this.delete();
        this.em().getTransaction().commit();
        return this;
    }

    public boolean hasInviteFor(String email)
    {
        final List<Attendance> list = this.attendances;
        for (Attendance attendance : list)
        {
            if (email.equals(attendance.email))
            {
                return true;
            }
        }
        return false;
    }

    public boolean hasInviteFor(User user)
    {
        if (getInviteForCustomer(user) == null)
        {
            return false;
        } else
        {
            return true;
        }
    }

    public Attendance getInviteForCustomer(User user)
    {
        List<Attendance> attendances = Attendance.getByEvent(this);
        Attendance a = null;
        for (Attendance attendance : attendances)
        {
            if (attendance.email.equals(user.login))
            {
                a = attendance;
            }
        }
        return a;
    }

    @Override
    public String toString()
    {
        return "Event [uuid=" + uuid + ", account=" + account + ", eventStart=" + eventStart + ", eventEnd=" + eventEnd + "]";
    }

    public Boolean isOwner(User user)
    {
        return user != null && user.uuid.equals(this.user.uuid) ? true : false;
    }

    public String getPrice()
    {
        if (price != null)
            return price;
        else
            return this.listing.price;
    }

    public String getCurrency()
    {
        if (this.currency != null)
            return currency;
        else
            return this.listing.currency;
    }

    public String getCharging()
    {
        if (charging != null)
            return charging;
        else
            return this.listing.charging;
    }

    public String getType()
    {
        if (type != null)
            return type;
        else
            return this.listing.type;
    }

    public String getPrivacy()
    {
        if (privacy != null)
            return privacy;
        else
            return this.listing.privacy;
    }

    public Boolean getChatEnabled()
    {
        if (chatEnabled != null)
            return chatEnabled;
        else
            return this.listing.chatEnabled;
    }

}
