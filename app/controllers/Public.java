package controllers;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import models.Activity;
import models.ChatFeed;
import models.Event;
import models.Listing;
import models.Message;
import models.Rating;
import models.User;

import org.apache.commons.lang.StringEscapeUtils;

import play.i18n.Lang;
import utils.JsonUtils;

import com.google.gson.JsonObject;

import dto.ActivityDTO;
import dto.ChatFeedDTO;
import email.EmailNotificationBuilder;

public class Public extends BaseController
{
    public static void embed(String channel) throws Throwable
    {
        final User user = getLoggedUser();
        final Listing listing = channel != null ? Listing.get(channel) : null;
        final List<Rating> ratings = listing != null ? Rating.getByObject(listing.uuid) : null;
        final Map<String, Object> stats = listing != null ? Rating.calculateStats(ratings) : null;
        final String baseUrl = getBaseUrlWithoutSlash();
        final String socketIo = getProperty(CONFIG_SOCKET_IO);
        final User userDisplayed = listing.user;
        render(user, userDisplayed, listing, baseUrl, socketIo, ratings, stats);
    }

    public static void checkConnection()
    {
        User user = getLoggedUser();
        if (user == null)
            forbidden();

        user = getLoggedUserNotCache();
        user.lastOnlineTime = new Date();
        user.save();

        JsonObject resp = new JsonObject();
        resp.addProperty("logged", "true");
        if (user.unreadMessages != null && user.unreadMessages)
            resp.addProperty("email", "true");
        renderJSON(resp.toString());
    }

    public static void locale(String locale, String url)
    {
        final User user = getLoggedUserNotCache();
        if (user != null)
        {
            user.locale = locale;
            user.save();
        }
        Lang.change(locale);
        redirectTo(url);
    }

    public static void feeds(String id, String uuid, String sender, String recipient, Integer from, Integer max)
    {
        List<ChatFeed> feeds = null;
        if (id != null)
            feeds = ChatFeed.getByUuid(id, from, max);

        else if (sender != null || recipient != null)
            feeds = ChatFeed.getBySenderRecipient(sender, recipient, from, max, uuid);

        final List<ChatFeedDTO> feedsDto = new ArrayList<ChatFeedDTO>();
        for (ChatFeed chatFeed : feeds)
            feedsDto.add(ChatFeedDTO.convert(chatFeed));
        renderJSON(feedsDto);
    }

    public static void feedsClear(String uuid, String url)
    {
        User user = getLoggedUser();
        Listing l = Listing.get(uuid);
        if (l != null && !user.equals(l.user))
            forbidden();
        Event e = Event.get(uuid);
        if (e != null && !user.equals(e.user))
            forbidden();

        checkAuthenticity();

        final List<ChatFeed> feeds = ChatFeed.getByUuid(uuid);
        for (ChatFeed chatFeed : feeds)
            chatFeed.delete();

        redirectTo(url);
    }

    public static void feedSave()
    {
        final JsonObject jo = JsonUtils.getJson(request.body);
        ChatFeed feed = new ChatFeed();
        feed = feedFromJson(jo, feed);
        feed.saveFeed();
        renderJSON(feed);
    }

    public static void activities(String id, int limit, String uuid)
    {
        final User user = getLoggedUser();
        if (user == null)
            forbidden();

        final List<Activity> activities = Activity.getByUser(user, limit, uuid);
        final List<ActivityDTO> aDto = new ArrayList<ActivityDTO>();
        for (Activity activity : activities)
            aDto.add(ActivityDTO.convert(activity, user));
        renderJSON(aDto);
    }

    public static void wiki()
    {
        renderTemplate("wiki.html");
    }

    private static ChatFeed feedFromJson(final JsonObject jo, ChatFeed feed)
    {
        feed.created = new Date();
        feed.uuid = StringEscapeUtils.escapeHtml(jo.get("uuid").getAsString());
        feed.comment = StringEscapeUtils.escapeHtml(jo.get("comment").getAsString());
        if (jo.get("name") != null)
            feed.name = StringEscapeUtils.escapeHtml(jo.get("name").getAsString());
        if (jo.get("listing") != null)
            feed.listing = StringEscapeUtils.escapeHtml(jo.get("listing").getAsString());
        if (jo.get("sender") != null)
            feed.sender = StringEscapeUtils.escapeHtml(jo.get("sender").getAsString());
        if (jo.get("senderName") != null)
            feed.senderName = StringEscapeUtils.escapeHtml(jo.get("senderName").getAsString());
        if (jo.get("recipient") != null)
            feed.recipient = StringEscapeUtils.escapeHtml(jo.get("recipient").getAsString());
        if (jo.get("recipientName") != null)
            feed.recipientName = StringEscapeUtils.escapeHtml(jo.get("recipientName").getAsString());
        return feed;
    }

    public static void postMessage()
    {
        final JsonObject jo = JsonUtils.getJson(request.body);
        final String sender = jo.get("sender") != null ? jo.get("sender").getAsString() : null;
        final String userId = jo.get("user") != null ? jo.get("user").getAsString() : null;
        final String subject = jo.get("subject") != null ? jo.get("subject").getAsString() : null;
        final String body = jo.get("body") != null ? jo.get("body").getAsString() : null;
        final String recipientId = jo.get("recipient").getAsString();

        User toUser = User.getUserByUUID(recipientId);
        if (userId != null)
            Notifications.send(toUser.login, subject, body, null, null, null);
        else
        {
            String emailBody = "Sender: " + sender + " (" + sender + ")\n\n";
            emailBody += body;

            // send notification
            Message.createNotification(null, toUser, subject, emailBody);

            // send email
            if (toUser != null && toUser.emailNotification)
            {
                EmailNotificationBuilder eb = new EmailNotificationBuilder();
                eb.setTo(toUser)
                        .setSubject(subject)
                        .setMessageWiki(emailBody)
                        .send();
            }
        }
    }
}