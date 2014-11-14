package models;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

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
    public static final String ATTENDANCE_RESULT_ACCEPTED = "accepted";
    public static final String ATTENDANCE_RESULT_DECLINED = "declined";

    @ManyToOne()
    @JoinColumn(name = "event_id")
    public Event event;

    @ManyToOne
    public Account account;

    @ManyToOne
    public User user;

    @ManyToOne
    public User customer;

    public String email;
    public String name;
    public Boolean paid;
    public String uuid;
    public String result;
    public Date created;
    public Boolean isForUser;
    public String accessToken;
    public Date accessTokenValidity;
    public String payerId;
    public String transactionId;
    public Date transactionDate;
    public String transactionIdProvider;
    public Boolean refunded;
    public String currency;
    public BigDecimal price;
    public BigDecimal fee;
    public BigDecimal providerPrice;
    public String paypalAccount;
    public String paypalAccountProvider;
    public Boolean watchlist;

    public static List<Attendance> getByEvent(Event event)
    {
        return Attendance.find("byEvent", event).fetch();
    }

    public static List<Attendance> getPayments(Account account, Date from, Date to)
    {
        String query = "from Attendance where 1 = 1 and paid = :paid and isForUser = :isForUser ";

        if (from != null)
            query += " and transactionDate >= :from ";
        if (to != null)
            query += " and transactionDate <= :to ";
        if (account != null)
            query += " and account = :account ";

        query += " order by transactionDate ";

        TypedQuery<Attendance> q = Attendance.em().createQuery(query, Attendance.class);
        q.setParameter("paid", true);
        q.setParameter("isForUser", false);

        if (from != null)
            q.setParameter("from", from);
        if (to != null)
            q.setParameter("to", to);
        if (account != null)
            q.setParameter("account", account);

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

    public Attendance save(Account account)
    {
        this.account = account;
        this.created = new Date();
        this.uuid = RandomUtil.getDoubleUUID();
        Attendance a = this.save();
        return a;
    }

    @Override
    public String toString()
    {
        return "Attendance [event=" + event + ", user=" + user + ", result=" + result + ", created=" + created + "]";
    }

}
