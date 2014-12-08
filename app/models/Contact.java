package models;

import java.util.Date;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import play.db.jpa.Model;
import utils.RandomUtil;

@Entity
@Table(name = "\"contact\"")
public class Contact extends Model
{
    @ManyToOne
    public User user;

    @ManyToOne
    public User contact;

    public Date created;
    public Boolean blocked;
    public String uuid;
    public Boolean following;

    public static List<Contact> getContacts(User user)
    {
        return Contact.em().createQuery("from Contact where user = :user order by contact.firstName asc contact.lastName asc").setParameter("user", user).getResultList();
    }

    public static List<Contact> getFollowing(User user)
    {
        return Contact.em().createQuery("from Contact where contact = :user and following = true  order by contact.firstName asc contact.lastName asc").setParameter("user", user)
                .getResultList();
    }

    public static List<Contact> getFollowers(User user)
    {
        return Contact.em().createQuery("from Contact where contact = :user and following = true  order by contact.firstName asc contact.lastName asc").setParameter("user", user)
                .getResultList();

    }

    public static List<Contact> getContacts(User user, String search)
    {
        return Contact
                .find("from Contact where user = ? and lower(concat(contact.login, ' ', contact.firstName, ' ', contact.lastName)) like ? order by contact.firstName asc contact.lastName asc",
                        user, "%" + search + "%").fetch();
    }

    public static Contact get(String uuid)
    {
        return Contact.find("byUuid", uuid).first();
    }

    public static Contact get(User user, User contact)
    {
        return Contact.find("byUserAndContact", user, contact).first();
    }

    public Contact saveContact()
    {
        this.blocked = false;
        this.uuid = RandomUtil.getUUID();
        this.created = new Date();
        Contact a = this.save();
        return a;
    }

    public static Contact isFollowing(User loggedUser, User usr, List<Contact> followees)
    {
        for (Contact contact : followees)
        {
            if (contact.user.equals(loggedUser) && contact.contact.equals(usr))
                return contact;
        }
        return null;
    }
}
