package models;

import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;

import play.db.jpa.Model;
import play.i18n.Messages;
import utils.RandomUtil;

@Entity
public class Activity extends Model
{
    public static final Long ACTIVITY_OLDEST_MILLIS = 1000L * 60 * 60 * 24 * 30;
    public static final String ACTIVITY_EVENT_INVITED = "invitationCreated";
    public static final String ACTIVITY_EVENT_INVITE_ACCEPTED = "invitationAccepted";
    public static final String ACTIVITY_EVENT_INVITE_DECLINED = "invitationDeclined";
    public static final String ACTIVITY_EVENT_DELETED = "eventDeleted";
    public static final String ACTIVITY_EVENT_DELETED_CUSTOMER = "eventDeletedCust";
    public static final String ACTIVITY_EVENT_CREATED_BY_USER = "eventCreatedUsr";
    public static final String ACTIVITY_EVENT_PROPOSED_BY_CUSTOMER = "eventCreatedCust";
    public static final String ACTIVITY_EVENT_MOVED = "eventMoved";
    public static final String ACTIVITY_EVENT_UPDATED_BY_USER = "eventUpdatedUsr";
    public static final String ACTIVITY_EVENT_UPDATED_BY_CUSTOMER = "eventUpdatedCust";
    public static final String ACTIVITY_EVENT_APPROVED = "eventApproved";
    public static final String ACTIVITY_EVENT_DECLINED = "eventDeclined";
    public static final String ACTIVITY_EVENT_COMMENTED = "eventCommented";
    public static final String ACTIVITY_EVENT_COMMENTED_CUSTOMER = "eventCommentedCust";
    public static final String ACTIVITY_EVENT_STARTED = "eventStarted";

    @ManyToOne(cascade = CascadeType.ALL)
    public Event event;

    public String eventName;

    @ManyToOne(cascade = CascadeType.ALL)
    public User user;

    @ManyToOne(cascade = CascadeType.ALL)
    public User customer;

    public Date created;

    public Boolean forCustomer;
    public String type;
    public String uuid;
    public String login;

    public Activity saveActivity()
    {
        this.created = new Date();
        this.uuid = RandomUtil.getUUID();
        Activity a = this.save();
        return a;
    }

    public String getText(Object... params)
    {
        return Messages.get(this.type, params);
    }

    public static Activity get(String key)
    {
        Activity account = Activity.find("byKey", key).first();
        return account;
    }

    public static List<Activity> getByUser(User user, int limit, String uuid)
    {
        Date now = new Date(System.currentTimeMillis() - ACTIVITY_OLDEST_MILLIS);
        String query = "select distinct act from Activity act join act.event.attendances as att where ";
        query += " act.created > ? and ( exists ( from Attendance a where a = att and a.customer = ? ) or act.user = ? or (act.customer = ? and act.forCustomer = true) ) ";
        query += " order by act.created desc ";
        return Activity.find(query, now, user, user, user).fetch(limit);
    }

    public static List<Activity> getByEvent(Event event)
    {
        return Activity.find("byEvent", event).fetch();
    }
}
