package controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.Contact;
import models.Event;
import models.Listing;
import models.Rating;
import models.Search;
import models.User;
import play.cache.Cache;
import play.mvc.Before;

//@With(Secure.class)
public class Application extends BaseController
{
    @Before(unless = { "home", "channels", "calendarUser" })
    static void checkAccess() throws Throwable
    {
        checkAuthorizedAccess();
    }

    public static void home()
    {
        final User user = getLoggedUser();
        Map<String, Object> ratings = initRatings();

        if (user != null && !user.isPublisher())
            flash.success("Help others and become a publisher. Request for publisher account <a href='/settings'>here</a>");
        render(user, ratings);
    }

    private static Map<String, Object> initRatings()
    {
        Map<String, Object> ratings = (Map<String, Object>) Cache.get("ratings");

        //TODO update this condition
        if (ratings == null || true)
        {
            ratings = new HashMap<String, Object>();
            Object r1 = Rating.getPopularByCategory("education");
            Object r2 = Rating.getPopularByCategory("sport");
            Object r3 = Rating.getPopularByCategory("health");
            ratings.put("education", r1);
            ratings.put("sport", r2);
            ratings.put("health", r3);
            Cache.add("ratings", ratings, "1h");
        }
        return ratings;
    }

    public static void search(String query)
    {
        List<String> result = Search.tags(query.toLowerCase());
        renderJSON(result);
    }

    public static void channels()
    {
        final User user = getLoggedUser();
        render(user);
    }

    public static void dashboard(String type)
    {
        final User user = getLoggedUser();
        final Boolean isOwner = true;
        final List<Event> watchList = user != null ? Event.getWatchList(user) : null;
        final List<Event> approved = user != null ? Event.getApprovement(user) : null;
        final List<Listing> listings = user != null ? Listing.getForUser(user) : null;
        final List<Contact> contacts = Contact.getContacts(user);

        if (user != null && !user.isPublisher())
            flash.success("Help others and become a publisher. Request for publisher account <a href='/settings'>here</a>");
        //Http.Cookie c = new Http.Cookie();
        //c.name = "timezone";
        //c.value = user.timezone.toString();
        //request.cookies.put("timezone", c);

        render(user, watchList, listings, approved, type, isOwner, contacts);
    }

    public static void calendarUser(String login) throws Throwable
    {
        final User user = getLoggedUser();
        // for event request user must be logged in
        if (request.params.get("listing") != null)
        {
            if (user == null)
                redirectToLogin(request.url);
            flash.put("warning", "Click and drag to create time request for event.");
        }

        final User userDisplayed = User.getUserByLogin(login);

        final List<Contact> followers = Contact.getFollowers(userDisplayed);
        final List<Contact> followees = Contact.getFollowing(userDisplayed);
        final Contact follow = Contact.isFollowing(user, userDisplayed, followees);
        calendarRender(userDisplayed, user, followers, followees, follow);
    }

    public static void calendar() throws Throwable
    {
        final User user = getLoggedUser();
        final User userDisplayed = null;
        calendarRender(userDisplayed, user, null, null, null);
    }

    private static void calendarRender(final User userDisplayed, final User user, final List<Contact> followers, final List<Contact> followees, Contact follow) throws Throwable
    {
        final String type = request.params.get("type") == null ? "calendar" : request.params.get("type");
        final String listingId = request.params.get("listing");
        final Listing listing = Listing.get(listingId);
        final List<Listing> listings = user != null ? Listing.getForUser(user) : null;
        Boolean isPublic = false;

        renderTemplate("/Application/calendar.html",
                user, isPublic, type, userDisplayed,
                listing, listings, followers, followees, follow);
    }
}