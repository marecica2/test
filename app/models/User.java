package models;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
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

    public String avatarId;

    public String avatarUrl;

    public Boolean unreadMessages;

    public Boolean emailNotification;

    public Boolean reminder;

    public Integer reminderMinutes;

    public Boolean activated;

    public Boolean blocked;

    public Boolean available;

    public String hiddenDays;

    public String workingHourStart;

    public String workingHourEnd;

    public Date lastLoginTime;

    public Date lastOnlineTime;

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

    public String googlePlus;

    public String googlePlusId;

    public String linkedIn;

    public String twitter;

    public String skype;

    public String stylesheet;

    public String googleAccessToken;

    public String googleRefreshToken;

    public Date googleTokenExpires;

    public String googleCalendarId;

    // random secret uuid token for invitations
    public String referrerToken;

    // identifies user who invited this user
    public String registrationToken;

    public Boolean hideInfoPublisher;

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
        return User.all().fetch();
    }

    public static List<User> getPublisherRequests()
    {
        return User.find("from User where account.type = ?", Account.TYPE_PUBLISHER_REQUEST).fetch();
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
        String fullName = firstName + " " + lastName;
        return fullName;
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
