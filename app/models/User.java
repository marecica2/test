package models;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Query;
import javax.persistence.Table;

import play.Logger;
import play.db.jpa.Model;
import utils.DateTimeUtils;
import utils.StringUtils;
import utils.WikiUtils;
import controllers.FileuploadController;
import controllers.PaymentController;

@Entity
@Table(name = "\"users\"")
public class User extends Model
{
    public static final String ROLE_SUPERADMIN = "superadmin";
    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_USER = "user";

    public String uuid;

    @ManyToOne
    public Account account;

    public String login;

    public String password;

    public String role;

    public String locale;

    public Integer timezone;

    public String firstName;

    public String lastName;

    public String avatarUrl;

    public Date created;

    public Boolean unreadMessages;

    public Boolean emailNotification;

    public Boolean activated;

    public Boolean blocked;

    public Date lastLoginTime;

    public Date lastOnlineTime;

    // random secret uuid token for invitations
    public String referrerToken;

    // identifies user who invited this user
    public String registrationToken;

    // user settings

    public Boolean reminder;

    public Integer reminderMinutes;

    public Boolean available;

    public String hiddenDays;

    public String workingHourStart;

    public String workingHourEnd;

    @Column(length = 2000)
    public String userAbout;

    @Column(length = 2000)
    public String userEducation;

    @Column(length = 2000)
    public String userExperiences;

    public String facebook;

    public String facebookId;

    public String facebookName;

    public String facebookTab;

    public String facebookPageType;

    public String facebookPageChannel;

    public String googleAccessToken;

    public String googleRefreshToken;

    public Date googleTokenExpires;

    public String googleCalendarId;

    public String googlePlus;

    public String googlePlusId;

    public String linkedIn;

    public String twitter;

    public String skype;

    public static User getUserByLogin(String login)
    {
        return User.find("byLogin", login).first();
    }

    public static User getUserByFacebook(String id)
    {
        return User.find("byFacebookId", id).first();
    }

    public static User getUserByFacebookPage(String pageId)
    {
        return User.find("byFacebookTab", pageId).first();
    }

    public static User getUserByToken(String token)
    {
        return User.find("byReferrerToken", token).first();
    }

    public static User getUserByUUID(String uuid)
    {
        return User.find("byUuid", uuid).first();
    }

    public User save(Account account)
    {
        this.account = account;

        User u = this;
        if (!u.isPersistent())
            u = u.merge();
        u = u.save();
        return u;
    }

    public static List<User> getUsers()
    {
        return User.find("from User order by account.key asc, created desc ").fetch();
    }

    public static List<User> getUsersByAccount(Account account)
    {
        return User.find("from User where account = ? order by created desc ", account).fetch();
    }

    public static List<User> getPublisherRequests()
    {
        return User.find("from User where account.type = ?", Account.TYPE_PUBLISHER_REQUEST).fetch();
    }

    public static void deleteUser(Integer id)
    {
        String query = "";
        query += "delete from activity where user_id = :userId ;";
        query += "delete from activity where customer_id = :userId ;";
        query += "delete from activity where event_id in (select id from event where user_id = :userId);";
        query += "delete from activity where event_id in (select id from event where customer_id = :userId);";
        query += "delete from message where fromuser_id = :userId ;";
        query += "delete from message where owner_id = :userId ;";
        query += "delete from message where touser_id = :userId ;";
        query += "delete from comment_comment_reply where comment_id in (select id from comment where user_id = :userId);";
        query += "delete from comment_comment_reply where replies_id in (select id from comment_reply where user_id = :userId);";
        query += "delete from comment where user_id = :userId ;";
        query += "delete from comment_reply where user_id = :userId ;";
        query += "delete from attendance where user_id = :userId ;";
        query += "delete from attendance where customer_id = :userId ;";
        query += "delete from attendance where event_id in (select id from event where customer_id = :userId);";
        query += "delete from attendance where event_id in (select id from event where user_id = :userId);";
        query += "delete from event where user_id = :userId ;";
        query += "delete from event where customer_id = :userId ;";
        query += "delete from fileupload where owner_id  = :userId ;";
        query += "delete from listing where user_id = :userId ;";
        query += "delete from contact where user_id = :userId ;";
        query += "delete from contact where contact_id = :userId ;";
        query += "delete from ratingvote where user_id = :userId ;";
        query += "delete from rating where user_id = :userId ;";
        query += "delete from users where id = :userId ;";

        Query q = User.em().createNativeQuery(query);
        q.setParameter("userId", id);
        q.executeUpdate();
    }

    @Override
    public String toString()
    {
        return "User [uuid=" + uuid + ", account=" + account + ", login=" + login + ", password=" + password + ", role=" + role + ", firstName=" + firstName + ", lastName=" + lastName + "]";
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        User other = (User) obj;
        if (uuid == null)
        {
            if (other.uuid != null)
                return false;
        } else if (!uuid.equals(other.uuid))
            return false;
        return true;
    }

    public String getAvatarUrl()
    {
        if (avatarUrl == null)
            return FileuploadController.PATH_TO_IMAGES + "/avatar";
        else
            return FileuploadController.PATH_TO_UPLOADS + avatarUrl + "";
    }

    public String userAboutHtml()
    {
        return WikiUtils.parseToHtml(this.userAbout);
    }

    public String userEducationHtml()
    {
        return WikiUtils.parseToHtml(this.userEducation);
    }

    public String userExperiencesHtml()
    {
        return WikiUtils.parseToHtml(this.userExperiences);
    }

    public String getFullName()
    {
        return firstName + " " + lastName;
    }

    public String getFullNameAccount()
    {
        String acc = "";
        if (this.account != null && StringUtils.getStringOrNull(this.account.name) != null)
            acc = ", " + account.name;
        return firstName + " " + lastName + acc;
    }

    public String getAccountName()
    {
        if (StringUtils.getStringOrNull(this.account.name) != null)
            return account.name;
        else
            return firstName + " " + lastName;
    }

    public Boolean isPublisher()
    {
        if (this.account.type != null && this.account.type.equals(Account.TYPE_PUBLISHER))
            return true;
        return false;
    }

    public Boolean isStandard()
    {
        if (this.account.type != null && this.account.type.equals(Account.TYPE_STANDARD))
            return true;
        return false;
    }

    public Boolean isAdmin()
    {
        if (this.role.equals(ROLE_SUPERADMIN))
            return true;
        return false;
    }

    public Boolean isOwner(Listing listing)
    {
        if (listing != null && listing.user != null && this.equals(listing.user))
            return true;
        return false;
    }

    public Boolean isOwner(Event event)
    {
        if (event != null && this.equals(event.user))
            return true;
        return false;
    }

    public Boolean isTeam(Listing listing)
    {
        if (listing != null && listing.user.account != null && this.account.equals(listing.user.account))
            return true;
        return false;
    }

    public Boolean isTeam(Event event)
    {
        if (event != null && this.account.equals(event.user.account))
            return true;
        return false;
    }

    public Boolean isOnline()
    {
        return (this.lastOnlineTime != null && (this.lastOnlineTime.getTime() > (System.currentTimeMillis() - 200000))) ? true : false;
    }

    public Boolean isAvailable()
    {
        if (this.isOnline() && this.available != null && this.available)
            return true;
        return false;
    }

    public boolean hasBlockedContact(User u)
    {
        final Contact blockedContact = Contact.get(this, u);
        if (blockedContact != null && blockedContact.blocked)
            return true;
        return false;
    }

    public boolean hasContact(User u)
    {
        final Contact contact = Contact.get(this, u);
        if (contact != null)
            return true;
        return false;
    }

    public boolean paidForCurrentMonth()
    {
        final AccountPlan currentPlan = this.account.currentPlan();
        if (currentPlan == null)
            return true;
        if (currentPlan != null && currentPlan.type.equals(Account.PLAN_STANDARD))
            return true;
        if (currentPlan != null && !currentPlan.type.equals(Account.PLAN_MONTH_PREMIUM) && currentPlan.profile != null)
            return true;
        if (currentPlan != null && !currentPlan.type.equals(Account.PLAN_MONTH_PRO) && currentPlan.profile != null)
            return true;
        return false;
    }

    public boolean hasValidPaymentAccount()
    {
        if (StringUtils.getStringOrNull(this.account.paypalAccount) == null)
            return false;
        return true;
    }

    public boolean syncWithGoogle()
    {
        if (this.googleTokenExpires != null && this.googleCalendarId != null && this.googleAccessToken != null)
            return true;
        return false;
    }

    public Integer availableEvents()
    {
        try
        {
            final AccountPlan currentPlan = this.account.currentPlan();
            if (currentPlan == null || (currentPlan != null && currentPlan.type.equals(Account.PLAN_STANDARD)))
                return 99999;

            if (currentPlan != null && currentPlan.profile != null && currentPlan.type.equals(Account.PLAN_MONTH_PREMIUM))
            {
                Map<String, String> respMap = PaymentController.getPayPalRecurringDetail(this.account);
                DateTimeUtils dt = new DateTimeUtils(DateTimeUtils.TYPE_PAYPAL);
                final String date = respMap.get("NEXTBILLINGDATE");
                Date next = currentPlan.validTo;
                if (date != null)
                    next = dt.fromString(date);

                final Calendar cal = Calendar.getInstance();
                cal.setTime(next);
                cal.add(Calendar.MONTH, -1);
                final Date last = cal.getTime();

                List<Event> events = Event.getPaidFromDate(last, next, this);
                return 10 - events.size();
            }
        } catch (Exception e)
        {
            Logger.error(e, "availableEvents ERROR");
        }
        return 99999;
    }

    public void detach()
    {
        this.em().detach(this);
    }
}
