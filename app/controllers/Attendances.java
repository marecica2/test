package controllers;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import models.Activity;
import models.Attendance;
import models.Contact;
import models.Event;
import models.User;
import play.i18n.Messages;
import play.mvc.Before;
import utils.RandomUtil;
import utils.StringUtils;
import utils.UriUtils;
import dto.AttendanceDTO;
import dto.UserDTO;

public class Attendances extends BaseController
{
    @Before(unless = { "attendancesRest" })
    static void checkAccess() throws Throwable
    {
        checkAuthorizedAccess();
    }

    public static void attendancesRest()
    {
        final String eventUuid = request.params.get("event");
        final Event event = Event.get(eventUuid);
        if (event != null)
        {
            List<AttendanceDTO> aDto = new ArrayList<AttendanceDTO>();
            List<Attendance> as = Attendance.getByEvent(event);
            for (Attendance a : as)
            {
                AttendanceDTO eDto = AttendanceDTO.convert(a);
                aDto.add(eDto);
            }
            renderJSON(aDto);
        }
        renderJSON(null);
    }

    public static void invites(String str)
    {
        final User user = getLoggedUser();
        List<Contact> c = Contact.getContacts(user, str.toLowerCase());
        List<UserDTO> contacts = new LinkedList<UserDTO>();
        if (user != null)
        {
            for (Contact contact : c)
                contacts.add(UserDTO.convert(contact.contact));
        }
        renderJSON(contacts);
    }

    public static void attendanceNewSave(String email, String eventId, String url)
    {
        email = StringUtils.extractReturnDefault(email, "\\((.+?)\\)");
        final Event event = Event.get(eventId);
        final User user = getLoggedUserNotCache();
        final Boolean isForUser = getLoggedUser().login.equals(email) ? true : false;
        final User customer = User.getUserByLogin(email);
        final boolean blocked = customer != null ? customer.hasBlockedContact(user) : false;
        if (blocked)
            validation.addError("email", Messages.get("blocked-contact"));

        validation.required(email);
        validation.email(email);

        if (!validation.hasErrors())
        {
            Attendance a = new Attendance();
            a.event = event;
            a.uuid = RandomUtil.getUUID();
            a.user = event.user;
            a.email = StringUtils.htmlEscape(email);
            a.isForUser = isForUser;
            if (customer != null)
            {
                a.customer = customer;
                a.name = customer.getFullName();
            } else
            {
                a.name = email;
            }
            a.saveAttendance();

            final Activity act = new Activity();
            act.type = Activity.ACTIVITY_EVENT_INVITED;
            act.forCustomer = true;
            act.customer = customer;
            act.login = customer != null ? customer.getFullName() : a.name;
            act.user = user;
            act.event = a.event;
            act.eventName = a.event.listing.title;
            act.saveActivity();

            if (customer != null)
            {
                Contact contact1 = Contact.get(user, customer);
                if (contact1 == null)
                {
                    contact1 = new Contact();
                    contact1.user = user;
                    contact1.contact = customer;
                    contact1.following = false;
                    contact1.saveContact();
                }

                Contact contact2 = Contact.get(customer, user);
                if (contact2 == null)
                {
                    contact2 = new Contact();
                    contact2.user = customer;
                    contact2.contact = user;
                    contact2.following = false;
                    contact2.saveContact();
                }
            }
        }
        params.flash();
        validation.keep();
        redirect(UriUtils.redirectStr(url));
    }

    public static void attendanceNewDelete(String uuid, String url)
    {
        User user = getLoggedUser();

        Attendance a = Attendance.get(uuid);
        a.delete();

        if (!a.event.user.equals(user))
        {
            final Activity act = new Activity();
            act.type = Activity.ACTIVITY_EVENT_INVITE_DECLINED;
            act.user = user;
            act.event = a.event;
            act.eventName = a.event.listing.title;
            act.saveActivity();
        }
        redirectTo(url);
    }

    public static void attendanceNewEdit(String uuid, String url, String type)
    {
        try
        {
            final User user = getLoggedUser();
            final String result = type.equals("accepted") ? Attendance.ATTENDANCE_RESULT_ACCEPTED : Attendance.ATTENDANCE_RESULT_DECLINED;
            final Attendance a = Attendance.get(uuid);
            a.result = result;
            a.save();

            final Activity act = new Activity();
            if (result.equals(Attendance.ATTENDANCE_RESULT_ACCEPTED))
                act.type = Activity.ACTIVITY_EVENT_INVITE_ACCEPTED;
            act.user = user;
            act.event = a.event;
            act.eventName = a.event.listing.title;
            act.saveActivity();

        } catch (Exception e)
        {
            e.printStackTrace();
        }
        redirectTo(url);
    }
}