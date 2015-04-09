package models;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.TypedQuery;

import play.db.jpa.Model;

@Entity
public class AccountPlan extends Model
{
    public static final String PLAN_STANDARD = "standard";
    public static final String PLAN_PREMIUM = "premium";
    public static final String PLAN_PRO = "pro";

    public Date validFrom;

    public Date validTo;

    public String type;

    public String profile;

    public Boolean paid;

    public BigDecimal price;

    public Boolean canceled;

    @ManyToOne(cascade = CascadeType.ALL)
    public Account account;

    public static AccountPlan getById(Account account, Long id)
    {
        return AccountPlan.find("byAccountAndId", account, id).first();
    }

    public static List<AccountPlan> getActive(Account account)
    {
        String query = "from AccountPlan where canceled is null";
        TypedQuery<AccountPlan> q = AccountPlan.em().createQuery(query, AccountPlan.class);
        return q.getResultList();
    }

    public static AccountPlan getCurrentPlan(Account account)
    {
        Date now = new Date();
        String query = "from AccountPlan where account = :account and validFrom <= :now and (validTo is null or validTo >= :now) order by id desc ";
        TypedQuery<AccountPlan> q = AccountPlan.em().createQuery(query, AccountPlan.class);
        q.setParameter("account", account);
        q.setParameter("now", now);
        List<AccountPlan> plans = q.getResultList();
        if (plans.size() > 0)
            return plans.get(0);
        return null;
    }

    public AccountPlan getPrevious()
    {
        String query = "from AccountPlan where account = :account order by id desc ";
        TypedQuery<AccountPlan> q = AccountPlan.em().createQuery(query, AccountPlan.class);
        q.setParameter("account", this.account);
        List<AccountPlan> plans = q.getResultList();
        if (plans.size() >= 2)
            return plans.get(1);
        return null;
    }

    public static AccountPlan getLast(Account account)
    {
        String query = "from AccountPlan where account = :account order by id desc ";
        TypedQuery<AccountPlan> q = AccountPlan.em().createQuery(query, AccountPlan.class);
        q.setParameter("account", account);
        List<AccountPlan> plans = q.getResultList();
        if (plans.size() > 0)
            return plans.get(0);
        return null;
    }

    public boolean isStandard()
    {
        if (this.type.equals(PLAN_STANDARD))
            return true;
        return false;
    }
}
