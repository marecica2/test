package controllers;

import google.GoogleCalendarClient;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jobs.QuartzServise;
import models.Account;
import models.Activity;
import models.Attendance;
import models.Comment;
import models.Contact;
import models.Event;
import models.FileUpload;
import models.Listing;
import models.Message;
import models.Rating;
import models.User;
import play.Logger;
import play.i18n.Messages;
import play.mvc.Before;
import utils.DateTimeUtils;
import utils.JsonUtils;
import utils.NumberUtils;
import utils.RandomUtil;

import com.google.gson.JsonObject;

import dto.EventDTO;
import email.EmailNotificationBuilder;

public class Events extends BaseController
{
    @Before(unless = { "events", "event" })
    static void checkAccess()
    {
        checkAuthorizedAccess();
    }

    public static void events() throws Exception
    {
        final Boolean showArchived = false;
        final Boolean googleCalSync = request.params.get("gcal") != null && request.params.get("gcal").equals("true") ? true : false;
        final DateTimeUtils dt = new DateTimeUtils(DateTimeUtils.TYPE_OTHER);
        final Date from = dt.fromJson(request.params.get("start"));
        final Date to = dt.fromJson(request.params.get("end"));
        final User user = getLoggedUser();

        final String direction = request.params.get("direction");
        final String type = request.params.get("type");
        final String providerId = request.params.get("user");
        final String listing = request.params.get("listing");

        final Integer first = request.params.get("first") != null ? Integer.parseInt(request.params.get("first")) : null;
        final Integer count = request.params.get("count") != null ? Integer.parseInt(request.params.get("count")) : null;
        final List<EventDTO> eventsDto = new ArrayList<EventDTO>();

        // if user is logged
        if (user != null)
        {
            // show events where user is invited
            List<Event> invitedEvents = Event.getBetween(from, to, user, showArchived, first, count, direction, type, listing);
            for (Event event : invitedEvents)
            {
                EventDTO eDto = EventDTO.convert(event, user);
                if (!eventsDto.contains(eDto))
                    eventsDto.add(eDto);
            }
        }

        // show provider events in calendar
        if (providerId != null)
        {
            User provider = User.getUserByLogin(providerId);
            if (provider != null)
            {
                List<Event> providerEvents = Event.getBetween(from, to, provider, showArchived, first, count, direction, type, listing);
                for (Event event : providerEvents)
                {
                    EventDTO eDto = EventDTO.convert(event, user);
                    if (!eventsDto.contains(eDto))
                        eventsDto.add(eDto);
                }
            }
        }

        // google calendar sync
        if (user != null && googleCalSync && user.googleCalendarId != null)
        {
            List<EventDTO> eventsDtoGoogle = GoogleCalendarClient.getEvents(user, from, to);
            for (EventDTO eDto : eventsDtoGoogle)
            {
                if (!eventsDto.contains(eDto))
                    eventsDto.add(eDto);
            }
        }

        for (EventDTO eventDTO : eventsDto)
            if (eventDTO.invisible)
                eventDTO = eventDTO.postProcessHiddenEvent(eventDTO);
        renderJSON(eventsDto);
    }

    public static void event(String action,
        String newEvent,
        String uuid,
        String url,
        String type,
        String listingId
        )
    {
        final User user = getLoggedUser();
        final Boolean edit = action != null && action.equals("edit") ? true : false;
        final Boolean isNew = newEvent != null ? true : false;
        final String baseUrl = getBaseUrl().substring(0, getBaseUrl().length() - 1);

        final Event event = !isNew ? Event.get(uuid) : null;
        final List<Listing> listings = event != null ? Listing.getForUser(event.listing.user) : null;
        final Boolean isOwner = event != null ? event.isOwner(user) : false;
        final Attendance attendance = user != null && event != null ? event.getInviteForCustomer(user) : null;
        final Boolean paid = (user != null && attendance != null && attendance.paid != null && attendance.paid) || isOwner ? true : false;

        // check access
        if (!isNew && event == null)
            notFound();

        if (edit && event != null && edit && !event.isEditable(user))
            forbidden();

        if (isNew && !user.account.type.equals(Account.TYPE_PUBLISHER))
            forbidden();

        if (event != null && !event.isVisible(user))
            forbidden();

        if (event != null && event.type.equals(Event.EVENT_TYPE_INSTANT_BROADCAST))
            checkPayment(event, request.url);

        if (event != null && event.type.equals(Event.EVENT_TYPE_BROADCAST))
            checkPayment(event, request.url);

        if (edit || isNew)
        {
            if (!user.hasValidPaymentAccount())
                flash.success(Messages.get("setup-paypal-account-warning"));
            if (!user.paidForCurrentMonth())
                flash.success(Messages.get("you-have-not-paid-for-current-month"));
        }

        final Listing listing = event != null ? event.listing : Listing.get(listingId);
        final Boolean fromEvent = true;
        final List<FileUpload> files = event != null ? FileUpload.getByObject(event.uuid) : null;
        final String temp = RandomUtil.getUUID();
        final String commentTemp = RandomUtil.getUUID();
        final List<Comment> comments = event != null ? Comment.getByEvent(event) : null;
        final List<Rating> ratings = listing != null ? Rating.getByObject(listing.uuid) : null;
        final Map<String, Object> stats = listing != null ? Rating.calculateStats(ratings) : null;

        if (event != null)
        {
            DateTimeUtils dt = new DateTimeUtils();
            final Integer offset = getTimezoneOffset();
            final Date eventStartOffset = DateTimeUtils.applyOffset(event.eventStart, offset);
            final Date eventEndOffset = DateTimeUtils.applyOffset(event.eventEnd, offset);
            final String eventStart = dt.formatDate(eventStartOffset, new SimpleDateFormat("HH:mm"));
            final String eventEnd = dt.formatDate(eventEndOffset, new SimpleDateFormat("HH:mm"));
            final String eventDate = dt.formatDate(eventStartOffset, new SimpleDateFormat("dd.MM.yyyy"));

            params.put("eventStart", eventStart);
            params.put("eventEnd", eventEnd);
            params.put("eventDate", eventDate);

            params.put("title", event.listing.title);
            params.put("category", event.listing.category);
            params.put("tags", event.listing.tags);
            params.put("description", event.listing.description);
            params.put("color", event.listing.color);
            params.put("image", event.listing.imageUrl);

            // can be modified
            params.put("type", event.type);
            params.put("privacy", event.privacy);
            params.put("currency", event.currency);
            params.put("charging", event.charging);
            if (event.language != null)
                params.put("language", event.language);
            if (event.price != null)
                params.put("price", event.price.toString());
            if (event.chargingTime != null)
                params.put("chargingTime", event.chargingTime.toString());
            if (event.listing.firstFree != null && event.listing.firstFree)
                params.put("firstFree", "true");
            if (event.chatEnabled != null && event.chatEnabled)
                params.put("chatEnabled", "true");
            if (event.commentsEnabled != null && event.commentsEnabled)
                params.put("commentsEnabled", "true");
        } else
        {
            params.put("type", listing.type);
            params.put("privacy", listing.privacy);
            params.put("currency", listing.currency);
            params.put("charging", listing.charging);

            if (listing.language != null)
                params.put("language", listing.language);
            if (listing.price != null)
                params.put("price", listing.price.toString());
            if (listing.chargingTime != null)
                params.put("chargingTime", listing.chargingTime.toString());
            if (listing.price != null)
                params.put("price", listing.price.toString());
            if (listing.chargingTime != null)
                params.put("chargingTime", listing.chargingTime.toString());
            if (listing.firstFree != null && listing.firstFree)
                params.put("firstFree", "true");
            if (listing.chatEnabled != null && listing.chatEnabled)
                params.put("chatEnabled", "true");
            if (listing.commentsEnabled != null && listing.commentsEnabled)
                params.put("commentsEnabled", "true");
        }
        params.put("temp", temp);
        params.flash();

        final String name = user != null ? user.getFullName() : Messages.get("anonymous") + RandomUtil.getRandomDigits(5);
        String room = event != null ? event.uuid : null;
        if (event != null && event.privateRoom != null && event.privateRoom)
            room = event.roomSecret;
        final String rmtp = getProperty(CONFIG_RMTP_PATH);
        final String socketIo = getProperty(CONFIG_SOCKET_IO);
        Map<String, String> errs = new HashMap<String, String>();
        render("Listings/listing.html", user, isOwner, edit, event, attendance, paid, url, errs, name, room, rmtp,
                socketIo, type, files, temp, commentTemp, comments, listings, ratings, stats, listing, fromEvent, baseUrl);
    }

    public static void eventPost(
        String action,
        String newEvent,
        String title,
        String description,
        String eventStart,
        String eventDate,
        String eventEnd,
        String privacy,
        String charging,
        Integer chargingTime,
        String language,
        String type,
        String category,
        String tags,
        String price,
        String currency,
        String color,
        String url,
        String offset,
        String uuid,
        String image,
        String imageId,
        String chatEnabled,
        String commentsEnabled,
        String listingId,
        String temp
        )
    {
        checkAuthenticity();
        final User user = getLoggedUser();
        final Boolean edit = action != null && action.equals("edit") ? true : false;
        final Boolean fromEvent = true;
        final Boolean isNew = newEvent != null ? true : false;
        int availableEvents = 9999;

        Event event = !isNew ? Event.get(uuid) : null;
        if (event != null)
            listingId = event.listing.uuid;
        final Listing listing = Listing.get(listingId);
        final Boolean isOwner = event != null ? event.isOwner(user) : false;
        final Map<String, String> errs = new HashMap<String, String>();

        // permissions check
        if (!user.isPublisher() || listing == null || !user.isOwner(listing) || (event != null && !user.isOwner(event)))
            forbidden();

        // validation
        checkAuthenticity();

        validation.required(eventStart);
        validation.required(eventEnd);
        validation.required(eventDate);
        DateTimeUtils dt = new DateTimeUtils();
        Date eventSt = dt.parseDate(eventDate + " " + eventStart, new SimpleDateFormat("dd.MM.yyyy HH:mm"));
        Date eventEn = dt.parseDate(eventDate + " " + eventEnd, new SimpleDateFormat("dd.MM.yyyy HH:mm"));
        eventSt = eventSt != null ? new Date(eventSt.getTime() + ((Integer.parseInt(offset) * 1000 * 60))) : null;
        eventEn = eventEn != null ? new Date(eventEn.getTime() + ((Integer.parseInt(offset) * 1000 * 60))) : null;
        validation.required(eventEn);
        validation.required(eventSt);
        if (charging != null && !charging.equals(Event.EVENT_CHARGING_FREE))
        {
            if (NumberUtils.parseDecimal(price) == null)
                validation.addError("price", Messages.get("invalid-price"));
            if (NumberUtils.parseDecimal(price) != null && NumberUtils.parseDecimal(price).compareTo(new BigDecimal("0")) <= 0)
                validation.addError("price", Messages.get("invalid-price"));

            availableEvents = user.availableEvents();
            if (availableEvents <= 0)
            {
                validation.addError("charging", Messages.get("limit-reached"));
                flash.error(Messages.get("limit-reached"));
            }
        }
        if (eventSt == null || eventEn == null || eventSt.compareTo(eventEn) > 0)
            validation.addError("time", Messages.get("invalid-time-range"));
        if (!charging.equals(Event.EVENT_CHARGING_FREE) && !user.hasValidPaymentAccount())
        {
            validation.addError("charging", Messages.get("invalid-paypal-account-warning"));
            flash.error(Messages.get("invalid-paypal-account-warning"));
        }
        if (!charging.equals(Event.EVENT_CHARGING_FREE) && !user.paidForCurrentMonth())
        {
            validation.addError("charging", Messages.get("you-have-not-paid-for-current-month"));
            flash.error(Messages.get("you-have-not-paid-for-current-month"));
        }

        if (!validation.hasErrors() && errs.size() == 0)
        {
            boolean create = true;
            if (event == null)
            {
                event = new Event();
                event.listing = listing;
                event.listing_uuid = listing.uuid;
                event.uuid = RandomUtil.getUUID();
                event.roomSecret = RandomUtil.getUUID();
                event.created = new Date();
                event.user = user;
                event.createdByUser = true;
                event.state = Event.EVENT_STATE_USER_CREATED;
            } else
            {
                create = false;
            }

            Date eventStartLast = event.eventStart;
            event.eventStart = eventSt;
            event.eventEnd = eventEn;
            event.privacy = privacy;
            event.language = language;
            event.type = type;
            event.currency = currency;
            event.price = NumberUtils.parseDecimal(price);
            event.charging = charging;
            event.chargingTime = chargingTime;
            event.chatEnabled = chatEnabled != null ? true : false;
            event.commentsEnabled = commentsEnabled != null ? true : false;
            event.lastModified = new Date();
            event = event.save();

            // check available events
            if (availableEvents < 20)
                flash.success(Messages.get("events-left") + ": " + (availableEvents - 1));

            // create default attendances
            Attendance.createDefaultAttendances(user, null, event, create, false);

            // scheduler update
            if (eventStartLast == null || eventStartLast.compareTo(eventSt) != 0)
            {
                event = event.refresh();
                QuartzServise.scheduleEvent(event);
            }

            redirect("/event/" + event.uuid);
        }
        params.flash();
        flash.error(Messages.get("invalid-channel-data"));
        render("Listings/listing.html", user, isOwner, edit, event, url, errs, type, listing, fromEvent);
    }

    public static void eventSaveRest()
    {
        checkAuthenticity();
        final User user = getLoggedUserNotCache();
        final JsonObject jo = JsonUtils.getJson(request.body);
        final DateTimeUtils time = new DateTimeUtils(DateTimeUtils.TYPE_OTHER);
        final String userId = jo.get("user").getAsString();
        final User userTo = User.getUserByUUID(userId);
        final String listingId = jo.get("listing") != null ? jo.get("listing").getAsString() : null;
        final String googleId = jo.get("googleId") != null ? jo.get("googleId").getAsString() : null;
        final Boolean proposal = jo.get("proposal").getAsBoolean();
        final User customer = user;
        Listing listing = Listing.get(listingId);

        if (user == null)
            forbidden();

        if (!proposal && !user.isPublisher())
            forbidden();

        if (proposal)
        {
            if (!userTo.isPublisher())
                forbidden();

            if (userTo.hasBlockedContact(customer))
                forbidden();

            int availableEvents = listing.user.availableEvents();
            if (availableEvents <= 0)
                forbidden();
        } else
        {
            int availableEvents = user.availableEvents();
            if (availableEvents <= 0)
                forbidden();
        }

        final Date start = time.fromJson(jo.get("eventStart").getAsString());
        final Date end = time.fromJson(jo.get("eventEnd").getAsString());

        if (start.getTime() > end.getTime())
            forbidden();
        if ((end.getTime() - start.getTime()) > (1000 * 60 * 60 * 2))
            forbidden();

        // create event
        Event event = new Event();
        event.eventStart = start;
        event.eventEnd = end;
        event.listing = listing;
        event.listing_uuid = listing.uuid;
        event.uuid = RandomUtil.getUUID();
        if (googleId != null)
            event.uuid = googleId;
        event.roomSecret = RandomUtil.getUUID();
        event.privacy = listing.privacy;
        event.type = listing.type;
        if (listing.type.equals(Event.EVENT_TYPE_INSTANT_BROADCAST))
            event.type = Event.EVENT_TYPE_BROADCAST;
        event.currency = listing.currency;
        event.price = listing.price;
        event.charging = listing.charging;
        event.chargingTime = listing.chargingTime;
        event.chatEnabled = listing.chatEnabled;
        event.commentsEnabled = listing.commentsEnabled;

        if (proposal)
        {
            event.charging = listing.charging;
            event.price = listing.price;
            event.currency = listing.currency;
            event.state = Event.EVENT_STATE_CUSTOMER_CREATED;
            event.customer = customer;
            event.createdByUser = false;
            event.user = listing.user;
        } else
        {
            event.state = Event.EVENT_STATE_USER_CREATED;
            event.createdByUser = true;
            event.user = user;
        }
        event = event.saveEvent();

        if (proposal)
        {
            final String locale = userTo == null ? "en" : userTo.locale;
            final String subject = Messages.getMessage(locale, "new-event-proposed-for-channel-subject", event.listing.title);
            final String message = Messages.getMessage(locale, "new-event-proposed-for-channel-message", event.customer.getFullName(), event.listing.title, getBaseUrl() + "event/"
                    + event.uuid);
            Message.createNotification(user, userTo, subject, message);
            new EmailNotificationBuilder()
                    .setFrom(user)
                    .setTo(userTo)
                    .setSubject(subject)
                    .setMessageWiki(message)
                    .send();

            final Activity act = new Activity();
            act.type = Activity.ACTIVITY_EVENT_PROPOSED_BY_CUSTOMER;
            act.user = customer;
            act.customer = customer;
            act.event = event;
            act.eventName = event.listing.title;
            act.saveActivity();

            createContacts(user, event.user);
        } else
        {
            final Activity act = new Activity();
            act.type = Activity.ACTIVITY_EVENT_CREATED_BY_USER;
            act.user = user;
            act.event = event;
            act.eventName = event.listing.title;
            act.saveActivity();
        }

        // google event sync

        Attendance.createDefaultAttendances(userTo, customer, event, true, proposal);

        // schedule 
        event = event.refresh();
        QuartzServise.scheduleEvent(event);

        renderJSON(EventDTO.convert(event, user));
    }

    private static void createContacts(User from, User to)
    {
        if (!from.hasContact(to))
        {
            Contact c = new Contact();
            c.user = from;
            c.contact = to;
            c.following = true;
            c.saveContact();
        }

        if (!to.hasContact(from))
        {
            Contact c1 = new Contact();
            c1.user = to;
            c1.contact = from;
            c1.following = true;
            c1.saveContact();
        }
    }

    public static void eventSyncGoogleRest(String uuid)
    {
        checkAuthenticity();
        final Event event = Event.get(uuid);
        if (event == null)
            notFound();

        // sync with google calendars
        for (Attendance a : event.attendances)
        {
            if (a.customer != null && !a.isForUser && a.customer.syncWithGoogle())
            {
                GoogleCalendarClient.upsertGoogleEvent(a, getBaseUrl());
                a.googleId = a.uuid;
                a.save();
            }
            if (a.user != null && a.isForUser && a.user.syncWithGoogle())
            {
                GoogleCalendarClient.upsertGoogleEvent(a, getBaseUrl());
                a.googleId = a.uuid;
                a.save();
            }
        }
        renderText("ok");
    }

    public static void eventUpdateRest()
    {
        checkAuthenticity();
        final User user = getLoggedUser();
        final JsonObject jo = JsonUtils.getJson(request.body);
        final String uuid = jo.get("uuid").getAsString();
        final Boolean timeChanged = jo.get("changedTime") != null ? true : false;
        final DateTimeUtils time = new DateTimeUtils(DateTimeUtils.TYPE_OTHER);
        Event event = Event.get(uuid);

        // permissions check
        if (!event.isEditable(user))
            forbidden();

        Date eventStartLast = event.eventStart;
        event.eventStart = time.fromJson(jo.get("eventStart").getAsString());
        event.eventEnd = time.fromJson(jo.get("eventEnd").getAsString());
        event.lastModified = new Date();
        event = event.save();

        // scheduler update
        if (eventStartLast == null || eventStartLast.compareTo(event.eventStart) != 0)
        {
            QuartzServise.scheduleEvent(event);
        }

        // create activity
        final Activity act = new Activity();
        if (timeChanged)
            act.type = Activity.ACTIVITY_EVENT_MOVED;
        else
            act.type = Activity.ACTIVITY_EVENT_UPDATED_BY_USER;
        act.user = user;
        act.event = event;
        act.eventName = event.listing.title;
        act.saveActivity();

        //CalendarClient.updateEvent(user, user.googleCalendarId, event.googleId, event.eventStart, event.eventEnd);
        renderJSON(EventDTO.convert(event, user));
    }

    public static void eventDeleteRest()
    {
        checkAuthenticity();
        final User user = getLoggedUser();
        final JsonObject jo = JsonUtils.getJson(request.body);
        final String uuid = jo.get("uuid").getAsString();
        final Event event = Event.get(uuid);

        // permissions check
        if (!event.isEditable(user))
            forbidden();

        // sync with google calendars
        for (Attendance a : event.attendances)
            GoogleCalendarClient.deleteFromGoogleCalendar(a);

        // unschedule quartz
        QuartzServise.unScheduleEvent(event);
        event.deleteEvent();
        renderJSON(EventDTO.convert(event, user));
    }

    public static void eventDelete(String uuid, String url)
    {
        checkAuthenticity();
        final User user = getLoggedUser();
        final Event event = Event.get(uuid);

        // permissions check
        if (user == null || !user.isOwner(event))
            forbidden();

        if (event != null)
        {
            QuartzServise.unScheduleEvent(event);
            event.deleteEvent();
        }

        // sync with google calendars
        for (Attendance a : event.attendances)
            GoogleCalendarClient.deleteFromGoogleCalendar(a);

        redirectTo(url);
    }

    public static void approve(String event, String url)
    {
        checkAuthenticity();
        final User user = getLoggedUserNotCache();
        final Event e = Event.get(event);

        // permissions check
        if (user == null || !user.isOwner(e))
            forbidden();
        e.state = Event.EVENT_STATE_USER_ACCEPTED;
        e.save();

        // notification
        final String subject = Messages.getMessage(e.customer.locale, "your-proposal-approved-subject", user.getFullName());
        final String body = Messages.getMessage(e.customer.locale, "your-proposal-approved-message", user.getFullName(), e.listing.title, getBaseUrl() + "event/" + e.uuid);
        if (e.customer != null)
            Message.createNotification(user, e.customer, subject, body);
        if (e.customer != null && e.customer.emailNotification)
        {
            EmailNotificationBuilder eb = new EmailNotificationBuilder();
            eb.setTo(e.customer);
            eb.setFrom(user)
                    .setSubject(subject)
                    .setMessageWiki(body)
                    .send();
        }

        Attendance attendance = e.getInviteForCustomer(user);
        attendance.result = Attendance.ATTENDANCE_RESULT_ACCEPTED;
        attendance.save();

        final Activity act = new Activity();
        act.type = Activity.ACTIVITY_EVENT_APPROVED;
        act.user = user;
        act.event = e;
        act.eventName = e.listing.title;
        act.saveActivity();

        redirectTo(url);
    }

    public static void decline(String event, String url)
    {
        checkAuthenticity();
        final User user = getLoggedUserNotCache();
        final Event e = Event.get(event);

        // permissions check
        if (user == null || !user.isOwner(e))
            forbidden();

        // notification
        final String subject = Messages.getMessage(e.customer.locale, "your-proposal-declined-subject", user.getFullName());
        final String body = Messages.getMessage(e.customer.locale, "your-proposal-declined-message", user.getFullName(), e.listing.title, getBaseUrl() + "channel/" + e.listing.uuid);
        if (e.customer != null)
            Message.createNotification(user, e.customer, subject, body);
        if (e.customer != null && e.customer.emailNotification)
        {
            EmailNotificationBuilder eb = new EmailNotificationBuilder();
            eb.setTo(e.customer);
            eb.setFrom(user)
                    .setSubject(subject)
                    .setMessageWiki(body)
                    .send();
        }

        e.deleteEvent();
        redirectTo(url);
    }

    public static void start(String event, String url)
    {
        checkAuthenticity();
        final User user = getLoggedUser();
        final Event e = Event.get(event);

        // permissions check
        if (!e.isEditable(user))
            forbidden();

        e.started = new Date();
        e.ended = null;
        e.save();
        redirectTo(url);
    }

    public static void stop(String event, String url)
    {
        checkAuthenticity();
        final User user = getLoggedUser();
        final Event e = Event.get(event);

        // permissions check
        if (!e.isEditable(user))
            forbidden();

        e.started = null;
        e.ended = new Date();
        e.save();
        redirectTo(url);
    }

    public static void hangoutCallback(String uuid, String ret, String yt)
    {
        Logger.info("");
        Logger.info("======= hangout callback ======");
        Logger.info(uuid);
        Logger.info(ret);
        Logger.info(yt);

        if (ret != null)
        {
            Event e = Event.get(ret);
            e.hangoutUrl = uuid;
            e.youtubeId = yt;
            e.started = new Date();
            e.ended = null;
            e.save();
        }
        renderJSON("var t = '';");
    }

    public static void eventInvite(String message, String eventId, String url, String[] invite)
    {
        checkAuthenticity();
        final User user = getLoggedUserNotCache();
        final Event event = Event.get(eventId);

        // permissions check
        if (!event.isEditable(user))
            forbidden();

        if (invite != null)
        {
            for (int i = 0; i < invite.length; i++)
            {
                final Attendance attendance = Attendance.get(invite[i]);
                final String locale = attendance.customer == null ? "en" : attendance.customer.locale;
                final String subject = Messages.getMessage(locale, "you-have-been-invited-subject", attendance.event.listing.title);
                final String body = Messages.getMessage(locale, "you-have-been-invited-message", event.listing.title, user.getFullName(), event.listing.title, getBaseUrl() + "event/"
                        + event.uuid);

                if (attendance.customer != null)
                {
                    Message.createNotification(user, attendance.customer, subject, body);
                }

                if (attendance.customer == null || attendance.customer != null && attendance.customer.emailNotification)
                {
                    EmailNotificationBuilder e = new EmailNotificationBuilder();
                    if (attendance.customer != null)
                        e.setTo(attendance.customer);
                    else
                        e.setToEmail(attendance.email);
                    e.setFrom(user)
                            .setEvent(event)
                            .setAttendance(attendance)
                            .setSubject(subject)
                            .setMessageWiki(body)
                            .sendInvitation();
                }
            }
        }
        redirectTo(url);
    }

}