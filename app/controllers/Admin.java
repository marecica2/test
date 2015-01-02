package controllers;

import java.util.List;

import javax.persistence.Query;

import models.Account;
import models.Message;
import models.User;
import play.cache.Cache;
import play.db.jpa.JPA;
import play.i18n.Messages;
import play.mvc.With;
import email.EmailNotificationBuilder;

@With(Secure.class)
public class Admin extends BaseController
{
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
        flash.success(refreshIndex());
        users();
    }

    public static void approve(String uuid, String url)
    {
        final User user = User.getUserByUUID(uuid);
        final String subject = Messages.get("publisher-request-approved-subject");
        final String message = Messages.get("publisher-request-approved-message");
        //"h3. Your request for publisher account was approved \nFrom now your account is fully functional and you can start to create events for your channel"

        Message.createNotification(getAdmin(), user, subject, message);
        new EmailNotificationBuilder()
                .setWidgrFrom()
                .setTo(user)
                .setSubject(subject)
                .setMessageRaw(message)
                .send();

        user.account.type = Account.TYPE_PUBLISHER;
        user.account.save();
        Cache.delete(user.login);
        redirectTo(url);
    }

    public static void deny(String uuid, String url)
    {
        final User user = User.getUserByUUID(uuid);
        final String subject = Messages.get("publisher-request-declined-subject");
        final String message = Messages.get("publisher-request-declined-message");
        //"h3. Your request for publisher account was declined \nTry again";

        Message.createNotification(getAdmin(), user, subject, message);
        new EmailNotificationBuilder()
                .setWidgrFrom()
                .setTo(user)
                .setSubject(subject)
                .setMessageWiki(message)
                .send();

        user.account.type = Account.TYPE_STANDARD;
        user.account.save();
        Cache.delete(user.login);
        redirectTo(url);
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

}