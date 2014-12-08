package controllers;

import java.util.List;

import javax.persistence.Query;

import models.Account;
import models.User;
import play.cache.Cache;
import play.db.jpa.JPA;
import play.mvc.Before;

//@With(Secure.class)
public class Admin extends BaseController
{
    @Before(unless = { "home" })
    static void checkAccess() throws Throwable
    {
        checkAdminAccess();
    }

    public static void publishers()
    {
        final User user = getLoggedUser();
        final List<User> publishers = User.getPublisherRequests();
        render(user, publishers);
    }

    public static void users()
    {
        final User user = getLoggedUser();
        final List<User> users = User.getUsers();
        render(user, users);
    }

    public static void refreshIndexes(String url)
    {
        final User user = getLoggedUser();
        if (user == null)
            forbidden();

        flash.success(refreshIndex());
        users();
    }

    private static String refreshIndex()
    {
        try
        {
            String query = "";
            query += "REFRESH MATERIALIZED VIEW search_index;";
            query += "REFRESH MATERIALIZED VIEW tags;";
            Query q = JPA.em().createNativeQuery(query);
            q.executeUpdate();
            return "indexes updated successfully";
        } catch (Exception e)
        {
            return e.getMessage();
        }
    }

    public static void approve(String uuid, String url)
    {
        final User user = User.getUserByUUID(uuid);
        user.account.type = Account.TYPE_PUBLISHER;
        user.account.save();
        Cache.delete(user.login);
        redirectTo(url);
    }

    public static void deny(String uuid, String url)
    {
        final User user = User.getUserByUUID(uuid);
        user.account.type = Account.TYPE_STANDARD;
        user.account.save();
        Cache.delete(user.login);
        redirectTo(url);
    }

}