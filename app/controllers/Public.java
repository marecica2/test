package controllers;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import models.Account;
import models.Activity;
import models.ChatFeed;
import models.Followers;
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

    public static void userProfile(String id, String userLogin)
    {
        final boolean userProfile = true;
        final User user = getLoggedUser();
        User usr = null;
        if (userLogin != null)
            usr = User.getUserByLogin(userLogin);
        if (usr == null)
            notFound();

        final Boolean isOwner = user != null && usr != null && usr.equals(user) ? true : false;
        final List<Followers> followers = Followers.getFollowers(usr);
        final List<Followers> followees = Followers.getFollowing(usr);
        final Followers follow = user != null ? Followers.get(user, usr) : null;
        final List<Rating> ratings = Rating.getByUser(usr.uuid);
        final List<Listing> listings = Listing.getForUser(usr);
        final Map<String, Object> stats = Rating.calculateStats(ratings);

        Account account = null;
        if (user != null)
            account = user.account;
        render(user, usr, account, isOwner, followees, followers, follow, ratings, stats, listings, userProfile);
    }

    public static void feedSave()
    {
        try
        {
            final JsonObject jo = JsonUtils.getJson(request.body);
            ChatFeed feed = new ChatFeed();
            feed = feedFromJson(jo, feed);

            if (isPublicRequest(request.headers))
            {
                User customer = getLoggedUser();
                feed.customer = customer;
            } else
            {
                User user = getLoggedUser();
                feed.user = user;
            }
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
}