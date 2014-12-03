package controllers;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import models.Activity;
import models.ChatFeed;
import models.Contact;
import models.Listing;
import models.Rating;
import models.User;

import org.apache.commons.lang.StringEscapeUtils;

import play.i18n.Lang;
import utils.JsonUtils;

import com.google.gson.JsonObject;

import dto.ActivityDTO;
import dto.ChatFeedDTO;
import facebook4j.Facebook;
import facebook4j.FacebookException;
import facebook4j.FacebookFactory;
import facebook4j.auth.AccessToken;

public class Public extends BaseController
{
    public static void locale(String locale, String url)
    {
        User user = getLoggedUserNotCache();
        if (user != null)
        {
            user.locale = locale;
            user.save();
        }
        System.err.println(locale);
        Lang.change(locale);
        redirectTo(url);
    }

    public static void feeds(String event)
    {
        final List<ChatFeed> feeds = ChatFeed.getByEvent(event);
        final List<ChatFeedDTO> feedsDto = new ArrayList<ChatFeedDTO>();
        for (ChatFeed chatFeed : feeds)
            feedsDto.add(ChatFeedDTO.convert(chatFeed));
        renderJSON(feedsDto);
    }

    public static void about()
    {
        User user = getLoggedUser();
        renderTemplate("Application/about.html", user);
    }

    public static void activities(String id, int limit, String uuid)
    {
        Boolean isPublic = false;
        User user = getLoggedUser();
        if (user == null)
        {
            isPublic = true;
            user = User.getUserByUUID(id);
        }

        List<Activity> activities = new ArrayList<Activity>();
        if (user != null)
            activities = Activity.getByUser(user, limit, uuid, isPublic);

        final List<ActivityDTO> aDto = new ArrayList<ActivityDTO>();
        for (Activity activity : activities)
        {
            aDto.add(ActivityDTO.convert(activity));
        }
        renderJSON(aDto);
    }

    public static void userProfile(String userLogin)
    {
        final boolean userProfile = true;
        final User user = getLoggedUser();
        final User usr = User.getUserByLogin(userLogin);
        final Boolean isOwner = user != null && usr != null && usr.equals(user) ? true : false;
        final Contact contact = user != null ? Contact.get(user, usr) : null;

        final List<Contact> followers = Contact.getFollowers(usr);
        final List<Contact> followees = Contact.getFollowing(usr);
        final Contact follow = Contact.isFollowing(user, usr, followers);

        final List<Rating> ratings = Rating.getByUser(usr.uuid);
        final List<Listing> listings = Listing.getForUser(usr);
        final Map<String, Object> stats = Rating.calculateStats(ratings);

        render(user, usr, userProfile, isOwner, listings, followees,
                followers, follow, ratings, stats, contact);
    }

    public static void feedSave()
    {
        try
        {
            final JsonObject jo = JsonUtils.getJson(request.body);
            ChatFeed feed = new ChatFeed();
            feed = feedFromJson(jo, feed);
            User user = getLoggedUser();
            feed.user = user;
            feed.saveFeed();
            renderJSON(feed);
        } catch (Exception e)
        {
            e.printStackTrace();
            response.status = 500;
            renderJSON("Failed to update event. Cause: " + e.getMessage());
        }
    }

    private static ChatFeed feedFromJson(final JsonObject jo, ChatFeed feed)
    {
        feed.comment = jo.get("comment").getAsString();
        Pattern p = Pattern.compile("\\[url\\](.*)\\[/url\\]\\[name\\](.*)\\[/name\\]");
        Matcher m = p.matcher(feed.comment);
        if (m.matches())
        {
            String url = m.group(1);
            String name = m.group(2);
            feed.comment = "<a href='" + url + "' target='_blank'>" + name + "</a>";
        } else
        {
            feed.comment = StringEscapeUtils.escapeHtml(feed.comment);
        }
        feed.name = jo.get("name").getAsString();
        feed.event = jo.get("event").getAsString();
        feed.created = new Date();
        return feed;
    }

    public static void facebookRegistration(String accessToken)
    {
        String appId = "117287758301883";
        String appSecret = "38ee75a4ea99046b7705101e7485f456";
        Facebook facebook = new FacebookFactory().getInstance();
        facebook.setOAuthAppId(appId, appSecret);
        facebook.setOAuthPermissions("email");
        facebook.setOAuthAccessToken(new AccessToken(accessToken));

        try
        {
            renderJSON(facebook.getMe());
        } catch (FacebookException e)
        {
            renderJSON("{\"error\":\"" + e.getMessage() + "\"}");
            e.printStackTrace();
        }
    }

    public static void wiki()
    {
        renderTemplate("wiki.html");
    }

    public static void activate(String uuid)
    {
        User user = User.getUserByUUID(uuid);
        user.activated = true;
        user.save();
        redirectTo("/login");
    }
}