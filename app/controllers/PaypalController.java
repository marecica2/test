package controllers;

import java.util.List;

import models.Account;
import models.Attendance;
import models.Event;
import models.User;

public class PaypalController extends BaseController
{
    public static void payments(String id)
    {
        final Boolean isPublic = true;
        final User user = User.getUserByUUID(id);
        final Account account = user != null ? user.account : null;
        final User customer = getLoggedUser();
        final User cust = customer;
        final List<Attendance> attendances = Attendance.getByCustomer(customer);
        final Boolean showAttendances = true;
        render("Public/customerProfile.html", customer, cust, account, user, attendances, isPublic, id, showAttendances);
    }

    public static void payBefore(String event, String url, String transactionId) throws Throwable
    {
        final Event e = Event.get(event);
        if (e == null)
            notFound();

        checkPayPalPayment(e, transactionId, url);
        redirectTo(url);
    }

    public static void joinRoom(String retUrl, String id, String eventId)
    {
        final Event e = Event.get(eventId);
        final String user = id;
        final User usr = User.getUserByUUID(user);
        final Account account = usr.account;
        final Boolean isPublic = true;
        final Boolean isCustomerLogged = getLoggedUser() == null ? false : true;
        final User customer = getLoggedUser();

        if (request.method.equals("GET"))
        {
            String returnUrl = retUrl;
            render(returnUrl, account, e, isPublic, isCustomerLogged, customer, id, retUrl);
        }
        if (request.method.equals("POST"))
        {
            final String postedName = request.params.get("name");
            System.err.println("posted name " + postedName);
            session.put("customername", postedName);
            redirect(request.params.get("url"));
        }
    }

    private static void checkAccessRights(Event e, String event, User customer, String id) throws Throwable
    {
        // allow only public events for not registered customers
        if (Event.EVENT_VISIBILITY_PUBLIC.equals(e.listing.privacy) && !isUserLogged())
        {
            joinRoom(request.url, id, event);

        } else if (Event.EVENT_VISIBILITY_PUBLIC.equals(e.listing.privacy) && isUserLogged())
        {
            System.err.println("Access granted - public event " + e.listing.title);

        } else if (!Event.EVENT_VISIBILITY_PUBLIC.equals(e.listing.privacy) && customer != null && e.hasInviteFor(customer))
        {
            System.err.println("Access for event " + e.listing.title + " granted to customer " + customer);

        } else
        {
            flash.put("id", id);
            flash.put("url", request.url);
            Secure.login();
        }
    }

}