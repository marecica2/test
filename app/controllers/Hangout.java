package controllers;

import java.util.ArrayList;
import java.util.List;

import jobs.DelayedNotification;
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
        BaseController.getRandomChannels();
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

        Boolean isOwner = false;
        if (user != null && event != null)
            isOwner = user.isOwner(event);

        if (user != null && event != null && event.user.hasBlockedContact(user))
            forbidden();

        final Event e = event;

        if (e != null && !e.isFree())
            checkPayment(e, request.url);

        // send rating request if user has not rated this channel
        if (user != null && event != null && !user.isTeam(event) && request.cookies.get(e.uuid) == null)
        {
            List<Rating> ratings = Rating.getByObjectUser(event.listing.uuid, user);
            if (ratings.size() == 0)
            {
                final String subject = Messages.getMessage(user.locale, "please-rating-subject");
                final String message = Messages.getMessage(user.locale, "please-rating-message", event.listing.title, getBaseUrl() + "event/" + event.uuid + "#ratings");
                DelayedNotification delayedNotification = new DelayedNotification(user, subject, message);
                delayedNotification.in(300);
                response.setCookie(e.uuid, e.uuid, "12h");
            }
        }

        final String name = user != null ? user.getFullName() : tempName;
        final String room = id;
        final String baseUrl = getBaseUrl().substring(0, getBaseUrl().length() - 1);
        final String socketIo = getProperty(CONFIG_SOCKET_IO);
        final String commentTemp = RandomUtil.getUUID();
        render(user, name, room, socketIo, baseUrl, event, isOwner, e, commentTemp);
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