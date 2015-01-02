package controllers;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

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
        final User user = getLoggedUserNotCache();
        if (user != null)
        {
            user.locale = locale;
            user.save();
        }
        Lang.change(locale);
        redirectTo(url);
    }

    public static void feeds(String event)
    {
        final List<ChatFeed> feeds = ChatFeed.getByUuid(event);
        final List<ChatFeedDTO> feedsDto = new ArrayList<ChatFeedDTO>();
        for (ChatFeed chatFeed : feeds)
            feedsDto.add(ChatFeedDTO.convert(chatFeed));
        renderJSON(feedsDto);
    }

    public static void feedSave()
    {
        final User user = getLoggedUser();
        if (user == null)
            forbidden();

        final JsonObject jo = JsonUtils.getJson(request.body);
        ChatFeed feed = new ChatFeed();
        feed = feedFromJson(jo, feed);
        feed.saveFeed();
        renderJSON(feed);
    }

    public static void about()
    {
        final User user = getLoggedUser();
        renderTemplate("Application/about.html", user);
    }

    public static void activities(String id, int limit, String uuid)
    {
        final User user = getLoggedUser();
        if (user == null)
            forbidden();

        final List<Activity> activities = Activity.getByUser(user, limit, uuid);
        final List<ActivityDTO> aDto = new ArrayList<ActivityDTO>();
        for (Activity activity : activities)
            aDto.add(ActivityDTO.convert(activity));
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

    private static ChatFeed feedFromJson(final JsonObject jo, ChatFeed feed)
    {
        feed.uuid = StringEscapeUtils.escapeHtml(jo.get("uuid").getAsString());
        feed.comment = StringEscapeUtils.escapeHtml(jo.get("comment").getAsString());
        feed.name = StringEscapeUtils.escapeHtml(jo.get("name").getAsString());
        feed.created = new Date();
        return feed;
    }
}