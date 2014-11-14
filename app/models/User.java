package models;

import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Query;
import javax.persistence.Table;

import play.db.jpa.Model;
import controllers.FileuploadController;

@Entity
@Table(name = "\"users\"")
public class User extends Model
{
    public static final String ROLE_SUPERADMIN = "superadmin";
    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_USER = "user";
    public static final String ROLE_CUSTOMER = "customer";

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

    public String hiddenDays;

    public String workingHourStart;

    public String workingHourEnd;

    public String agendaType;

    public Date lastLoginTime;

    public Date lastOnlineTime;

    @Column(length = 2000)
    public String userAbout;

    @Column(length = 2000)
    public String userEducation;

    @Column(length = 2000)
    public String userExperiences;

    public String facebook;

    public String googlePlus;

    public String linkedIn;

    public String twitter;

    public String skype;

    public String getFullName()
    {
        return firstName + " " + lastName;
    }

    public static User getUserByLogin(String login)
    {
        return User.find("byLogin", login).first();
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

    public String getAvatarUrlSmall()
    {
        //        String md5 = RandomUtil.getMD5Hex(this.login);
        //        return "//www.gravatar.com/avatar/" + md5 + "?s=25";
        return FileuploadController.PATH_TO_UPLOADS + avatarUrl + "_64x64";
    }

    public String getAvatarUrlTiny()
    {
        return FileuploadController.PATH_TO_UPLOADS + avatarUrl + "_16x16";
    }

    public static List<User> getCustomersForAccount(Account account, String queryString)
    {
        //        String query = "select distinct c from Customer c join c.attendances as al where al.account = :account";
        //        if (str != null)
        //            query += " and concat(c.login, ' ', c.firstName, ' ', c.lastName) like :login ";
        //
        //        Query q = Customer.em().createQuery(query);
        //        q.setParameter("account", account);
        //        if (str != null)
        //        {
        //            q.setParameter("login", "%" + str + "%");
        //        }

        String query = "select distinct c from Attendance a, User c where a.account = :account and a.customer.uuid = c.uuid ";
        if (queryString != null)
            query += " and lower(concat(a.email, ' ', c.firstName, ' ', c.lastName)) like :queryString";

        Query q = User.em().createQuery(query);
        q.setParameter("account", account);
        if (queryString != null)
        {
            q.setParameter("queryString", "%" + queryString.toLowerCase() + "%");
        }
        List<User> customers = q.getResultList();
        return customers;
    }

    public static List<User> getUsers()
    {
        return User.all().fetch();
    }
}
