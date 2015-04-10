package models;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Entity;

import play.db.jpa.Model;

@Entity
public class Account extends Model
{
    public static final String TYPE_STANDARD = "standard";
    public static final String TYPE_PUBLISHER_REQUEST_PREPARE = "publish";
    public static final String TYPE_PUBLISHER_REQUEST = "publisherReq";
    public static final String TYPE_PUBLISHER = "publisher";

    public static final String PLAN_STANDARD = "standard";
    public static final String PLAN_MONTH_PREMIUM = "premium";
    public static final String PLAN_MONTH_PRO = "pro";

    public static final BigDecimal PRICE_PLAN_PREMIUM = new BigDecimal("10");
    public static final BigDecimal PRICE_PLAN_PRO = new BigDecimal("25");
    public static final String PRICE_PLAN_CURRENCY = "USD";

    public String name;
    public String type;
    public String key;
    public String url;
    public String currency;
    public String paypalAccount;
    public Date requestTime;
    public Date created;

    public static Account get(String key)
    {
        Account account = Account.find("byKey", key).first();
        return account;
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

    public String formatUrl(String url)
    {
        if (url != null && url.contains("http://"))
            return url;
        if (url != null && !url.contains("http://"))
            return "http://" + url;
        return url;
    }

    public AccountPlan currentPlan()
    {
        AccountPlan plan = AccountPlan.getCurrentPlan(this);
        return plan;
    }

    public BigDecimal getPlanPrice()
    {
        final AccountPlan currentPlan = this.currentPlan();
        if (currentPlan != null && currentPlan.type.equals(PLAN_MONTH_PREMIUM))
            return PRICE_PLAN_PREMIUM;
        if (currentPlan != null && currentPlan.type.equals(PLAN_MONTH_PRO))
            return PRICE_PLAN_PRO;
        return null;
    }
}
