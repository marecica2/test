package models;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.TypedQuery;

import play.db.jpa.Model;
import utils.RandomUtil;

@Entity
@Table(name = "Attendance")
public class Attendance extends Model
{
    public static final String PAYMENT_METHOD_PAYPAL = "paypal";

    public static final String ATTENDANCE_RESULT_ACCEPTED = "accepted";
    public static final String ATTENDANCE_RESULT_DECLINED = "declined";

    public static final String ATTENDANCE_PAYMENT_PAYPAL = "paypal";
    public static final String ATTENDANCE_PAYMENT_GOOGLE = "google";

    @ManyToOne()
    @JoinColumn(name = "event_id")
    public Event event;

    @ManyToOne
    public User user;

    @ManyToOne
    public User customer;

    public Boolean watchlist;
    public String email;
    public String name;
    public Boolean paid;
    public String uuid;
    public String result;
    public Date created;
    public Boolean isForUser;

    public BigDecimal price;
    public BigDecimal fee;
    public BigDecimal providerPrice;
    public String currency;
    public Boolean refunded;
    public Boolean refundRequested;
    @Column(length = 500)
    public String refundReason;
    public String paymentMethod;

    public String paypalAccessToken;
    public Date paypalAccessTokenValidity;
    public String paypalPayerId;
    public String paypalTransactionId;
    public Date paypalTransactionDate;
    public String paypalTransactionIdProvider;
    public String paypalAccount;
    public String paypalAccountProvider;
    public String paypalAdaptivePayKey;

    public static List<Attendance> getByEvent(Event event)
    {
        return Attendance.find("byEvent", event).fetch();
    }

    public static List<Attendance> getPayments(User user, Date from, Date to)
    {
        String query = "select a from Attendance as a join a.event as event where 1 = 1 and paid = :paid and isForUser = :isForUser ";
        if (from != null)
            query += " and a.paypalTransactionDate >= :from ";
        if (to != null)
            query += " and a.paypalTransactionDate <= :to ";
        if (user != null)
            query += " and event.user = :user ";
        query += " order by paypalTransactionDate desc";

        TypedQuery<Attendance> q = Attendance.em().createQuery(query, Attendance.class);
        q.setParameter("paid", true);
        q.setParameter("isForUser", false);

        if (from != null)
            q.setParameter("from", from);
        if (to != null)
            q.setParameter("to", to);
        if (user != null)
            q.setParameter("user", user);

        List<Attendance> attendances = q.getResultList();
        return attendances;
    }

    public static List<Attendance> getByCustomer(User customer)
    {
        return Attendance.find("from Attendance where customer = ? order by created desc", customer).fetch(500);
    }

    public static Attendance getByCustomerEvent(User customer, Event event)
    {
        return Attendance.find("from Attendance where customer = ? and event = ? order by created desc", customer, event).first();
    }

    public static List<Attendance> getByCustomerUser(User customer, Account account)
    {
        return Attendance.find("byCustomerAndAccount", customer, account).fetch(100);
    }

    public static Attendance get(String uuid, Account account)
    {
        return Attendance.find("byUuidAndAccount", uuid, account).first();
    }

    public static Attendance get(String uuid)
    {
        return Attendance.find("byUuid", uuid).first();
    }

    public Attendance update()
    {
        Attendance a = this.save();
        return a;
    }

    public Attendance saveAttendance()
    {
        this.created = new Date();
        this.uuid = RandomUtil.getUUID();
        Attendance a = this.save();
        return a;
    }

    @Override
    public String toString()
    {
        return "Attendance [event=" + event + ", user=" + user + ", result=" + result + ", created=" + created + "]";
    }

}
