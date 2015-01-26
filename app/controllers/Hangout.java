package controllers;

import java.util.ArrayList;
import java.util.List;

import models.Event;
import models.Message;
import models.Rating;
import models.User;
import play.i18n.Messages;
import utils.JsonUtils;
import utils.RandomUtil;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import email.EmailNotificationBuilder;

public class Hangout extends BaseController
{
    //@Before(only = { "createRoom" })
    static void checkAccess()
    {
        checkAuthorizedAccess();
    }

    public static void instantRoom()
    {
        redirect("/room?id=" + RandomUtil.getUUID());
    }

    public static void room(String id, String transactionId, String tempName) throws Throwable
    {
        final User user = getLoggedUserNotCache();
        final Event event = Event.get(id);
        final Event e = event;

        if (user != null && event != null && !user.isOwner(event))
        {
            List<Rating> ratings = Rating.getByObjectUser(event.uuid, user);
            if (ratings.size() == 0 || (ratings.size() > 0 && ratings.get(0).created.getTime() < (System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 0)))
            {
                final String subject = Messages.getMessage(user.locale, "please-rating-subject");
                final String message = Messages.getMessage(user.locale, "please-rating-message", event.listing.title, getBaseUrl() + "event/" + event.uuid + "#ratings");
                Message.createAdminNotification(user, subject, message);
            }
        }

        if (user == null && tempName == null)
            joinRoom(id);

        if (e != null && !e.isFree())
            checkPayment(e, request.url);

        final String name = user != null ? user.getFullName() : tempName;
        final String room = id;
        final String baseUrl = getBaseUrl().substring(0, getBaseUrl().length() - 1);
        final String socketIo = getProperty(CONFIG_SOCKET_IO);
        render(user, name, room, socketIo, baseUrl);
    }

    public static void joinRoom(String id)
    {
        render(id);
    }

    public static void joinRoomPost(String name, String id)
    {
        final String redirect = "/room?id=" + id + "&tempName=" + name;
        redirect(redirect);
    }

    public static void createRoom()
    {
        User user = getLoggedUser();
        render(user);
    }

    public static void invite() throws Exception
    {
        final User user = getLoggedUserNotCache();
        final String userName = user != null ? user.getFullName() : "Someone";
        final JsonObject jo = JsonUtils.getJson(request.body);
        final List<String> logins = new ArrayList<String>();
        final String room = jo.get("room").getAsString();
        final JsonArray ja = jo.get("invites").getAsJsonArray();
        for (JsonElement el : ja)
            logins.add(el.getAsString());

        for (String login : logins)
        {
            final User toUser = User.getUserByLogin(login);
            final String locale = toUser == null ? "en" : toUser.locale;
            final String subject = Messages.getMessage(locale, "invitation-to-video-call-subject");
            final String message = Messages.getMessage(locale, "invitation-to-video-call-message", userName, getBaseUrl() + "room?id=" + room);
            if (toUser != null)
                Message.createNotification(user, toUser, subject, message);

            EmailNotificationBuilder e = new EmailNotificationBuilder();
            if (toUser != null)
                e.setTo(toUser);
            else
                e.setToEmail(login);
            e.setWidgrFrom()
                    .setToEmail(login)
                    .setSubject(subject)
                    .setMessageWiki(message)
                    .send();
        }
        renderJSON("{\"response\":\"ok\"}");
    }
}