package controllers;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import models.Account;
import models.Activity;
import models.Attendance;
import models.Event;
import models.User;
import play.mvc.Before;
import utils.JsonUtils;
import utils.RandomUtil;
import utils.StringUtils;
import utils.UriUtils;

import com.google.gson.JsonObject;

import dto.AttendanceDTO;

public class Attendances extends BaseController
{
    @Before(unless = { "login", "attendances" })
    static void checkAccess() throws Throwable
    {
        checkAuthorizedAccess();
    }

    public static void attendance()
    {
        final Account account = getAccountByUser();
        final String uuid = request.params.get("uuid");
        final Attendance a = Attendance.get(uuid, account);
        renderJSON(AttendanceDTO.convert(a));
    }

    public static void attendances()
    {
        final String eventUuid = request.params.get("event");
        final Event event = Event.get(eventUuid);
        if (event != null)
        {
            List<AttendanceDTO> aDto = new ArrayList<AttendanceDTO>();
            List<Attendance> as = Attendance.getByEvent(event);
            for (Attendance a : as)
            {
                // if anonymous user is accessing event resources filter out private events
                AttendanceDTO eDto = AttendanceDTO.convert(a);
                aDto.add(eDto);
            }
            renderJSON(aDto);
        }
        renderJSON("null");
    }

    //    public static void attendanceSave()
    //    {
    //        try
    //        {
    //            final JsonObject jo = JsonUtils.getJson(request.body);
    //            final Event e = Event.get(jo.get("event").getAsString());
    //            final String email = jo.get("email") != null ? StringUtils.htmlEscape(jo.get("email").getAsString()) : "";
    //            final User user = User.getUserByLogin(email);
    //            final User customer = User.getUserByLogin(email);
    //            final boolean isForUser = user != null ? true : false;
    //
    //            Attendance a = new Attendance();
    //            a.event = e;
    //            a.user = e.user;
    //            a.email = StringUtils.htmlEscape(email);
    //            a.isForUser = isForUser;
    //
    //            if (isForUser)
    //            {
    //                a.user = user;
    //                a.name = user.getFullName();
    //            } else if (customer != null)
    //            {
    //                a.customer = customer;
    //                a.name = customer.getFullName();
    //            } else
    //            {
    //                a.name = "guest" + RandomUtil.getRandomDigits(5);
    //            }
    //
    //            a.save(e.account);
    //            renderJSON(AttendanceDTO.convert(a));
    //        } catch (Exception e)
    //        {
    //            e.printStackTrace();
    //            response.status = 500;
    //            renderJSON("Failed to update event. Cause: " + e.getMessage());
    //        }
    //    }

    public static void attendanceNewSave(String email, String eventId, String url)
    {
        final User loggedUser = getLoggedUser();
        final Event e = Event.get(eventId);
        final User user = User.getUserByLogin(email);
        final User customer = User.getUserByLogin(email);
        final boolean isForUser = getLoggedUser().login.equals(email) ? true : false;

        validation.required(email);
        validation.email(email);

        if (!validation.hasErrors())
        {
            Attendance a = new Attendance();
            a.event = e;
            a.user = e.user;
            a.email = StringUtils.htmlEscape(email);
            a.isForUser = isForUser;

            if (isForUser)
            {
                a.user = user;
                a.name = user.getFullName();
            } else if (customer != null)
            {
                a.customer = customer;
                a.name = customer.getFullName();
            } else
            {
                a.customer = null;
                a.user = null;
                a.name = "guest" + RandomUtil.getRandomDigits(5);
            }
            a.save(e.account);

            final Activity act = new Activity();
            act.type = Activity.ACTIVITY_EVENT_INVITED;
            act.forCustomer = true;
            act.customer = customer;
            act.login = customer != null ? customer.getFullName() : a.name;
            act.user = loggedUser;
            act.event = a.event;
            act.eventName = a.event.listing.title;
            act.saveActivity();

        }
        params.flash();
        redirect(UriUtils.redirectStr(url));
    }

    public static void attendanceUpdate()
    {
        try
        {
            final User user = getLoggedUser();
            final JsonObject jo = JsonUtils.getJson(request.body);
            final String uuid = jo.get("uuid").getAsString();
            Attendance a = Attendance.get(uuid);
            a.result = jo.get("result").getAsString();
            a.save();

            final Activity act = new Activity();
            if (a.result.equals("accepted"))
                act.type = Activity.ACTIVITY_EVENT_INVITE_ACCEPTED;
            else
                act.type = Activity.ACTIVITY_EVENT_INVITE_DECLINED;
            act.user = user;
            act.event = a.event;
            act.eventName = a.event.listing.title;
            act.saveActivity();

            renderJSON(AttendanceDTO.convert(a));
        } catch (Exception e)
        {
            e.printStackTrace();
            response.status = 500;
            renderJSON("Failed to update event. Cause: " + e.getMessage());
        }

    }

    public static void attendanceDelete()
    {
        try
        {
            final JsonObject jo = JsonUtils.getJson(request.body);
            final String uuid = jo.get("uuid").getAsString();
            Attendance a = Attendance.get(uuid);
            a.delete();
            renderJSON(AttendanceDTO.convert(a));
        } catch (Exception e)
        {
            e.printStackTrace();
            response.status = 500;
            renderJSON("Failed to delete event. Cause: " + e.getMessage());
        }
    }

    public static void watchListAdd(String event, String url)
    {
        final Event e = Event.get(event);
        final User user = getLoggedUser();
        Attendance a = Attendance.getByCustomerEvent(user, e);

        if (a == null)
        {
            a = new Attendance();
            a.customer = user;
            a.event = e;
            a.created = new Date();
            a.email = user.login;
            a.result = Attendance.ATTENDANCE_RESULT_ACCEPTED;
            a.account = user.account;
            a.name = user.getFullName();
            a.watchlist = true;
            a.uuid = RandomUtil.getDoubleUUID();
        } else
        {
            a.watchlist = true;
        }
        a.save();
        redirectTo(url);
    }

    public static void watchListRemove(String id, String url)
    {
        Attendance a = Attendance.get(id);
        if (a != null)
        {
            a.watchlist = null;
            a.save();
        }
        redirectTo(url);
    }

    public static void attendanceNewDelete(String uuid, String url)
    {
        try
        {
            Attendance a = Attendance.get(uuid);
            a.delete();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        redirect(UriUtils.redirectStr(url));
    }

    public static void attendanceNewEdit(String uuid, String url, String type)
    {
        try
        {
            User user = getLoggedUser();
            Attendance a = Attendance.get(uuid);
            a.result = type.equals("accepted") ? Attendance.ATTENDANCE_RESULT_ACCEPTED : Attendance.ATTENDANCE_RESULT_DECLINED;
            a.save();

            final Activity act = new Activity();
            if (a.result.equals("accepted"))
                act.type = Activity.ACTIVITY_EVENT_INVITE_ACCEPTED;
            else
                act.type = Activity.ACTIVITY_EVENT_INVITE_DECLINED;
            act.user = user;
            act.event = a.event;
            act.eventName = a.event.listing.title;
            act.saveActivity();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        redirect(UriUtils.redirectStr(url));
    }

}