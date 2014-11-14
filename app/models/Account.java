package models;

import java.util.Date;

import javax.persistence.Entity;

import play.db.jpa.Model;

@Entity
public class Account extends Model
{
    public static final String TYPE_STANDARD = "standard";
    public static final String TYPE_PUBLISHER_REQUEST = "publisherReq";
    public static final String TYPE_PUBLISHER = "publisher";

    public String name;
    public String type;
    public String key;
    public String url;
    public String currency;
    public String paypalAccount;

    public Date requestTime;

    public String smtpHost;
    public String smtpPort;
    public String smtpAccount;
    public String smtpPassword;
    public String smtpProtocol;

    public static Account get(String key)
    {
        Account account = Account.find("byKey", key).first();
        return account;
    }

    public static Account getAccountByEvent(String eventUuid)
    {
        Event event = Event.get(eventUuid);
        return event.account;
    }

    @Override
    public String toString()
    {
        return "Account [key=" + key + ", name=" + name + ", url=" + url + "]";
    }

    public Account saveAccount()
    {
        Account c = this;
        if (!c.isPersistent())
            c = c.merge();
        c = c.save();
        return c;
    }

    @Override
    public boolean equals(Object obj)
    {
        Account other = (Account) obj;
        if (key == null)
        {
            if (other.key != null)
                return false;
        } else if (!key.equals(other.key))
            return false;
        return true;
    }

}
