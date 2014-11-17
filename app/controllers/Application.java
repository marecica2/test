package controllers;

import java.util.List;

import models.Account;
import models.Activity;
import models.Attendance;
import models.Event;
import models.Followers;
import models.Listing;
import models.User;
import play.mvc.Before;

//@With(Secure.class)
public class Application extends BaseController
{
    @Before(unless = { "calendarUser", "users", "home" })
    static void checkAccess() throws Throwable
    {
        checkAuthorizedAccess();
    }

    public static void home()
    {
        final Boolean isPublic = false;
        final User user = getLoggedUser();
        final Account account = user != null ? user.account : null;
        List<User> users = User.getUsers();
        List<Listing> listings = null;
        render(user, account, isPublic, users, listings);
    }

    public static void dashboard(String type)
    {
        final User user = getLoggedUser();
        final List<Event> watchList = user != null ? Event.getWatchList(user) : null;
        final List<Event> approved = user != null ? Event.getApprovement(user) : null;
        final List<Listing> listings = user != null ? Listing.getForUser(user) : null;

        //Http.Cookie c = new Http.Cookie();
        //c.name = "timezone";
        //c.value = user.timezone.toString();
        //request.cookies.put("timezone", c);

        render(user, watchList, listings, approved, type);
    }

    public static void calendarUser(String login) throws Throwable
    {
        User userDisplayed = null;
        final User user = getLoggedUser();
        if (login != null)
            userDisplayed = User.getUserByLogin(login);
        final List<Followers> followers = Followers.getFollowers(userDisplayed);
        final List<Followers> followees = Followers.getFollowing(userDisplayed);
        if (request.params.get("listing") != null)
            flash.put("warning", "Click and drag to create time request for event.");
        calendarRender(userDisplayed, user, followers, followees);
    }

    public static void calendar() throws Throwable
    {
        User userDisplayed = null;
        final User user = getLoggedUser();
        calendarRender(userDisplayed, user, null, null);
    }

    private static void calendarRender(User userDisplayed, final User user, List<Followers> followers, List<Followers> followees) throws Throwable
    {
        final Account account = user != null ? user.account : null;
        final String eventId = request.params.get("event");
        final String accept = request.params.get("accept");
        final String type = request.params.get("type") == null ? "calendar" : request.params.get("type");
        final String listingId = request.params.get("listing");
        final Listing listing = Listing.get(listingId);
        final List<Listing> listings = user != null ? Listing.getForUser(user) : null;
        Boolean isPublic = false;

        if (accept != null && !isUserLogged())
        {
            flash.put("url", request.url);
            Secure.login();
        }

        //accept or decline meeting and view event detail
        if (accept != null && isUserLogged())
        {
            final Event event = Event.get(eventId);
            if (event != null)
            {
                final Activity act = new Activity();
                act.user = event.user;
                act.event = event;
                act.eventName = event.listing.title;
                Attendance a = event.getInviteForCustomer(getLoggedUser());
                if (accept.equals(Attendance.ATTENDANCE_RESULT_ACCEPTED))
                {
                    act.type = Activity.ACTIVITY_EVENT_INVITE_ACCEPTED;
                    a.result = Attendance.ATTENDANCE_RESULT_ACCEPTED;
                }
                if (accept.equals(Attendance.ATTENDANCE_RESULT_DECLINED))
                {
                    act.type = Activity.ACTIVITY_EVENT_INVITE_DECLINED;
                    a.result = Attendance.ATTENDANCE_RESULT_DECLINED;
                }
                act.save();
                a.save();
            }
        }
        renderTemplate("/Application/calendar.html", user, account, isPublic, type, userDisplayed, listing, listings, followers, followees);
    }
}