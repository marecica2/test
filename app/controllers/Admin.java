package controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        final Map<String, User> usersMap = new HashMap<String, User>();
        for (User user2 : users)
        {
            usersMap.put(user2.referrerToken, user2);
        }
        render(user, users, usersMap);
    }

    public static void deleteUser(Integer id, String url)
    {
        User user = getLoggedUser();
        if (user == null || !user.isAdmin())
            notFound();
        User.deleteUser(id);
        redirectTo(url);
    }

    public static void block(String uuid, String url)
    {
        User user = User.getUserByUUID(uuid);
        user.blocked = true;
        user.save();
        redirectTo(url);
    }

    public static void unblock(String uuid, String url)
    {
        User user = User.getUserByUUID(uuid);
        user.blocked = null;
        user.save();
        redirectTo(url);
    }

    public static void refreshIndexes(String url)
    {
        flash.success(refreshIndex());
        users();
    }

    public static void deleteData()
    {
        flash.success(delete());
        users();
    }

    public static void approve(String uuid, String url)
    {
        final User user = User.getUserByUUID(uuid);
        final String subject = Messages.getMessage(user.locale, "publisher-request-approved-subject");
        final String message = Messages.getMessage(user.locale, "publisher-request-approved-message");

        Message.createAdminNotification(user, subject, message);
        new EmailNotificationBuilder()
                .setWidgrFrom()
                .setTo(user)
                .setSubject(subject)
                .setMessageWiki(message)
                .send();

        user.account.type = Account.TYPE_PUBLISHER;
        user.account.save();
        Cache.delete(user.login);
        redirectTo(url);
    }

    public static void deny(String uuid, String url)
    {
        final User user = User.getUserByUUID(uuid);
        final String email = getBaseUrl() + "mail?action=new&to=" + getAdminUser().login;
        final String subject = Messages.getMessage(user.locale, "publisher-request-declined-subject");
        final String message = Messages.getMessage(user.locale, "publisher-request-declined-message", email);

        Message.createAdminNotification(user, subject, message);
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

    public static String refreshIndex()
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

    private static String delete()
    {
        try
        {
            String query = "";
            //            query += "DROP TABLE IF EXISTS ratingvote CASCADE;";
            //            query += "DROP TABLE IF EXISTS comment_comment CASCADE;";
            //            query += "DROP TABLE IF EXISTS comment_reply CASCADE;";
            //            query += "DROP TABLE IF EXISTS comment_comment_reply CASCADE;";
            //            query += "DROP TABLE IF EXISTS comment_fileupload CASCADE;";
            //            query += "DROP TABLE IF EXISTS activity CASCADE;";
            //            query += "DROP TABLE IF EXISTS rating CASCADE;";
            //            query += "DROP TABLE IF EXISTS event_comment_fileupload CASCADE;";
            //            query += "DROP TABLE IF EXISTS event_comment CASCADE;";
            //            query += "DROP TABLE IF EXISTS fileupload CASCADE;";
            //            query += "DROP TABLE IF EXISTS comment CASCADE;";
            //            query += "DROP TABLE IF EXISTS message CASCADE;";
            //            query += "DROP TABLE IF EXISTS event CASCADE;";
            //            query += "DROP TABLE IF EXISTS chat_feed CASCADE;";
            //            query += "DROP TABLE IF EXISTS attendance CASCADE;";

            Query q = JPA.em().createNativeQuery(query);
            q.executeUpdate();
            return "tables cleared successfully";
        } catch (Exception e)
        {
            return e.getMessage();
        }
    }

}