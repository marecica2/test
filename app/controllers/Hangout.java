package controllers;

import java.util.ArrayList;
import java.util.List;

import models.Event;
import models.Message;
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
        final User user = getLoggedUser();
        final Event event = Event.get(id);
        final Event e = event;

        if (user == null && tempName == null)
            joinRoom(id);

        if (e != null && !e.isFree())
            checkPayment(e, request.url);

        final String name = user != null ? user.getFullName() : tempName;
        final String room = id;
        final String socketIo = getProperty(CONFIG_SOCKET_IO);
        render(user, name, room, socketIo);
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

            final String subject = Messages.get("invitation-to-video-call-subject");
            final String message = Messages.get("invitation-to-video-call-subject-message", userName, getBaseUrl() + "room?id=" + room);
            final User toUser = User.getUserByLogin(login);
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