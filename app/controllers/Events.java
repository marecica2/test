package controllers;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.Activity;
import models.Attendance;
import models.Comment;
import models.Event;
import models.FileUpload;
import models.Listing;
import models.Rating;
import models.User;

import org.apache.velocity.VelocityContext;

import play.mvc.Before;
import templates.VelocityTemplate;
import utils.DateTimeUtils;
import utils.JsonUtils;
import utils.NumberUtils;
import utils.RandomUtil;
import utils.StringUtils;
import youtube.LiveStreams;

import com.google.gson.JsonObject;

import dto.EventDTO;
import email.EmailProvider;
import email.Notification;

public class Events extends BaseController
{
    @Before(unless = { "events", "eventNew" })
    static void checkAccess()
    {
        checkAuthorizedAccess();
    }

    public static void events()
    {
        final Boolean showArchived = false;
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
                    convertEvent(eventsDto, event, eDto, user);
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
                        convertEvent(eventsDto, event, eDto, user);
                }
            }
        }
        renderJSON(eventsDto);
    }

    private static void convertEvent(List<EventDTO> eventsDto, Event event, EventDTO eDto, User user)
    {
        // public event render

        final Boolean isPublic = event.privacy.equals(Event.EVENT_VISIBILITY_PUBLIC) ? true : false;
        final Boolean isOwner = user != null ? event.isOwner(user) : false;
        final Boolean isInvited = user != null && event.hasInviteFor(user) ? true : false;
        final Boolean isCustomerCreated = user != null && event.state.equals(Event.EVENT_STATE_CUSTOMER_CREATED) ? true : false;

        // isOwner
        eDto.isOwner = isOwner;

        eDto.isInvited = isInvited;

        // isEditable
        eDto.isEditable = isOwner || isCustomerCreated ? true : false;

        if (!isPublic && !isInvited)
            eDto = EventDTO.postProcessHiddenEvent(eDto);

        eventsDto.add(eDto);
    }

    public static void hangoutYoutubeId(String id)
    {
        final User user = getLoggedUser();
        LiveStreams.getStream(user.uuid);
        redirect("https://hangouts.google.com/onair");
    }

    public static void hangoutYoutubeIdRest(String id)
    {
        final User user = getLoggedUser();
        final String youtubeId = LiveStreams.getStream(user.uuid);
        final Event event = Event.get(id);
        event.youtubeId = youtubeId;
        event.save();
        if (youtubeId != null)
        {
            JsonObject jo = new JsonObject();
            jo.addProperty("youtube", youtubeId);
            renderJSON(jo);
        }
        JsonObject jo = new JsonObject();
        jo.addProperty("resp", youtubeId);
        renderJSON(jo);
    }

    public static void event(String action, String newEvent, String uuid, String url, String type, String listingId)
    {
        final User user = getLoggedUser();
        final Event event = Event.get(uuid);
        final Boolean edit = action != null && action.equals("edit") ? true : false;
        final Boolean isOwner = event != null ? event.isOwner(user) : false;
        final Boolean isNew = newEvent != null ? true : false;
        final Attendance attendance = user != null && event != null ? event.getInviteForCustomer(user) : null;
        final Boolean paid = (user != null && attendance != null && attendance.paid != null && attendance.paid) || isOwner ? true : false;

        // check access
        if (event == null && !isNew)
            notFound();
        if (event != null && !isOwner && edit)
            forbidden();
        if (event != null && event.isPrivate() && user == null)
            forbidden();
        if (event != null && event.isPrivate() && !isOwner && !event.hasInviteFor(user))
            forbidden();

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
            final Integer offset = NumberUtils.parseInt(request.cookies.get("timezoneJs").value);
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
            if (event.price != null)
                params.put("price", event.price.toString());
            if (event.chargingTime != null)
                params.put("chargingTime", event.chargingTime.toString());
            if (event.listing.firstFree)
                params.put("firstFree", "true");
            if (event.chatEnabled)
                params.put("chatEnabled", "true");
            if (event.commentsEnabled)
                params.put("commentsEnabled", "true");
        } else
        {
            params.put("type", listing.type);
            params.put("privacy", listing.privacy);
            params.put("currency", listing.currency);
            params.put("charging", listing.charging);
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

        final String name = user != null ? user.getFullName() : null;
        final String room = event != null ? event.uuid : null;
        final String rmtp = getProperty(CONFIG_RMTP_PATH);
        final String socketIo = getProperty(CONFIG_SOCKET_IO);
        Map<String, String> errs = new HashMap<String, String>();
        render("Listings/listingNew.html", user, isOwner, edit, event, attendance, paid, url, errs, name, room, rmtp,
                socketIo, type, files, temp, commentTemp, comments, ratings, stats, listing, fromEvent);
    }

    public static void eventPost(
        String action,
        String title,
        String description,
        String eventStart,
        String eventDate,
        String eventEnd,
        String privacy,
        String charging,
        Integer chargingTime,
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
        final User user = getLoggedUser();
        final Boolean edit = action != null && action.equals("edit") ? true : false;
        final Boolean fromEvent = true;
        Event event = Event.get(uuid);
        if (event != null)
            listingId = event.listing.uuid;
        final Listing listing = Listing.get(listingId);
        final Boolean isOwner = event != null ? event.isOwner(user) : false;
        final Map<String, String> errs = new HashMap<String, String>();

        // permissions check
        if (!user.isPublisher() || listing == null || !user.isOwner(listing) || (event != null && !user.isOwner(event)))
            forbidden();

        // validation
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
        if (eventSt == null || eventEn == null || eventSt.compareTo(eventEn) > 0)
            validation.addError("time", "Invalid time range");

        if (!validation.hasErrors() && errs.size() == 0)
        {
            boolean create = true;
            if (event == null)
            {
                event = new Event();
                event.listing = listing;
                event.listing_uuid = listing.uuid;
                event.uuid = temp;
                event.roomSecret = RandomUtil.getUUID();
                event.created = new Date();
                event.user = user;
                event.createdByUser = true;
                event.state = Event.EVENT_STATE_USER_CREATED;
            } else
            {
                create = false;
            }
            event.eventStart = eventSt;
            event.eventEnd = eventEn;
            event.privacy = privacy;
            event.type = type;
            event.currency = currency;
            event.price = new BigDecimal(price);
            event.charging = charging;
            event.chargingTime = chargingTime;
            event.chatEnabled = chatEnabled != null ? true : false;
            event.commentsEnabled = commentsEnabled != null ? true : false;
            event.lastModified = new Date();
            event.save();

            // mark file upload as stored
            if (imageId != null)
            {
                List<FileUpload> fu = FileUpload.getByObject(event.uuid);
                for (FileUpload fileUpload : fu)
                {
                    fileUpload.stored = true;
                    fileUpload.save();
                }
            }
            createDefaultAttendances(user, null, event, create, false);
            redirect("/event/" + event.uuid);
        }
        params.flash();
        render("Listings/listingNew.html", user, isOwner, edit, event, url, errs, type, listing, fromEvent);
    }

    public static void eventSaveRest()
    {
        final JsonObject jo = JsonUtils.getJson(request.body);
        final DateTimeUtils time = new DateTimeUtils(DateTimeUtils.TYPE_OTHER);
        final String userId = jo.get("user").getAsString();
        final String listingId = jo.get("listing") != null ? StringUtils.htmlEscape(jo.get("listing").getAsString()) : "";
        final Boolean proposal = jo.get("proposal").getAsBoolean();
        User user = getLoggedUser();
        User customer = user;

        // create event
        Listing listing = Listing.get(listingId);
        Event event = new Event();
        event = eventFromJson(time, jo, event);
        event.listing = listing;
        event.listing_uuid = listing.uuid;
        event.uuid = RandomUtil.getUUID();
        event.roomSecret = RandomUtil.getUUID();
        event.privacy = listing.privacy;
        event.type = listing.type;
        event.currency = listing.currency;
        event.price = listing.price;
        event.charging = listing.charging;
        event.chargingTime = listing.chargingTime;
        event.chatEnabled = listing.chatEnabled;
        event.commentsEnabled = listing.commentsEnabled;
        if (proposal)
        {
            user = User.getUserByUUID(userId);
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
        event = event.save(user.account);

        if (proposal)
        {
            final Activity act = new Activity();
            act.type = Activity.ACTIVITY_EVENT_PROPOSED_BY_CUSTOMER;
            act.user = customer;
            act.event = event;
            act.eventName = event.listing.title;
            act.saveActivity();
        } else
        {
            final Activity act = new Activity();
            act.type = Activity.ACTIVITY_EVENT_CREATED_BY_USER;
            act.user = user;
            act.event = event;
            act.eventName = event.listing.title;
            act.saveActivity();
        }
        createDefaultAttendances(user, customer, event, true, proposal);
        renderJSON(EventDTO.convert(event, user));
    }

    public static void eventUpdateRest()
    {
        final User user = getLoggedUser();
        final JsonObject jo = JsonUtils.getJson(request.body);
        final String uuid = jo.get("uuid").getAsString();
        final Boolean timeChanged = jo.get("changedTime") != null ? true : false;
        final DateTimeUtils time = new DateTimeUtils(DateTimeUtils.TYPE_OTHER);
        Event event = Event.get(uuid);

        // permissions check
        if (user == null || !user.isOwner(event))
            forbidden();

        event = eventFromJson(time, jo, event);
        event.lastModified = new Date();
        event = event.save();

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

        renderJSON(EventDTO.convert(event, user));
    }

    public static void eventDeleteRest()
    {
        final User user = getLoggedUser();
        final JsonObject jo = JsonUtils.getJson(request.body);
        final String uuid = jo.get("uuid").getAsString();
        final Event event = Event.get(uuid);
        if (user == null || !user.isOwner(event))
            forbidden();

        event.deleteEvent();
        renderJSON(EventDTO.convert(event, user));
    }

    public static void eventDelete(String uuid, String url)
    {
        final User user = getLoggedUser();
        final Event event = Event.get(uuid);

        // permissions check
        if (user == null || !user.isOwner(event))
            forbidden();

        if (event != null)
            event.deleteEvent();
        redirectTo(url);
    }

    public static void approve(String event, String url)
    {
        final User user = getLoggedUser();
        final Event e = Event.get(event);

        // permissions check
        if (user == null || !user.isOwner(e))
            forbidden();

        e.state = Event.EVENT_STATE_USER_ACCEPTED;
        e.save();

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
        final User user = getLoggedUser();
        final Event e = Event.get(event);

        // permissions check
        if (user == null || !user.isOwner(e))
            forbidden();

        e.state = Event.EVENT_STATE_USER_ACCEPTED;
        e.save();

        final Activity act = new Activity();
        act.type = Activity.ACTIVITY_EVENT_APPROVED;
        act.user = user;
        act.event = e;
        act.eventName = e.listing.title;
        act.saveActivity();

        redirectTo(url);
    }

    public static void start(String uuid, String url)
    {
        final User user = getLoggedUser();
        final Event e = Event.get(uuid);

        // permissions check
        if (user == null || !user.isOwner(e))
            forbidden();

        e.started = new Date();
        e.save();
        redirectTo(url);
    }

    public static void eventInvite(String message, String eventId, String url, String[] invite)
    {
        final User user = getLoggedUser();
        final Event event = Event.get(eventId);

        // permissions check
        if (user == null || !user.isOwner(event))
            forbidden();

        if (invite != null)
        {
            for (int i = 0; i < invite.length; i++)
            {
                final Attendance attendance = Attendance.get(invite[i]);
                final String subject = "Widgr: " + user.getFullName() + " invited you to event " + attendance.event.listing.title;
                final String locale = "en";

                //                if (attendance.customer != null)
                //                {
                //                if (attendance.isForUser && attendance.user != null)
                //                    locale = attendance.user.locale;
                //                if (!attendance.isForUser && attendance.customer != null)
                //                    locale = attendance.customer.locale;
                //
                //                    mail.body = message;
                //                    mail.created = new Date();
                //                    mail.fromUser = user;
                //                    mail.toUser = attendance.customer;
                //                    mail.subject = subject;
                //                    mail.uuid = RandomUtil.getUUID();
                //                    mail.save();
                //                    attendance.customer.unreadMessages = true;
                //                    attendance.customer.save();
                //                }

                // notify by email
                if (attendance.customer == null && !attendance.isForUser)
                {
                    try
                    {
                        final String baseUrl = getProperty(BaseController.CONFIG_BASE_URL);
                        final EmailProvider emailProvider = new EmailProvider(user.account.smtpHost, user.account.smtpPort,
                                user.account.smtpAccount, user.account.smtpPassword, "10000", user.account.smtpProtocol, true);
                        final VelocityContext ctx = VelocityTemplate.createInvitationTemplate(locale, attendance.email, user, event, baseUrl, attendance.uuid);
                        if (message != null && message.length() > 0)
                        {
                            ctx.put("notification", message);
                            ctx.put("notificationLabel", user.firstName + " says:");

                        }

                        final String body = VelocityTemplate.processTemplate(ctx, VelocityTemplate.getTemplateContent(VelocityTemplate.CONTACT_INVITE_TEMPLATE));
                        new Notification(emailProvider, user.login, subject, attendance.email, body).execute();
                    } catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }
        redirectTo(url);
    }

    private static void createDefaultAttendances(User user, User customer, Event event, Boolean create, Boolean proposal)
    {
        if (create)
        {
            if (!proposal)
            {
                // create attendance on the background for user creator
                Attendance a = new Attendance();
                a.user = user;
                a.result = Attendance.ATTENDANCE_RESULT_ACCEPTED;
                a.name = user.getFullName();
                a.email = user.login;
                a.event = event;
                a.customer = user;
                a.isForUser = true;
                a.saveAttendance();

                final Activity act = new Activity();
                act.type = Activity.ACTIVITY_EVENT_CREATED_BY_USER;
                act.user = user;
                act.event = event;
                act.eventName = event.listing.title;
                act.saveActivity();
            }

            // create attendance on the background for customer creator
            if (proposal)
            {
                // create attendance for user
                Attendance a = new Attendance();
                a.user = user;
                a.name = user.getFullName();
                a.email = user.login;
                a.event = event;
                a.customer = user;
                a.isForUser = true;
                a.saveAttendance();

                // create attendance for customer
                Attendance a1 = new Attendance();
                a1.email = customer.login;
                a1.name = customer.getFullName();
                a1.customer = customer;
                a1.event = event;
                a1.result = Attendance.ATTENDANCE_RESULT_ACCEPTED;
                a1.isForUser = false;
                a1.saveAttendance();
            }
        }
    }

    private static Event eventFromJson(DateTimeUtils time, final JsonObject jo, Event event)
    {
        event.eventStart = time.fromJson(jo.get("eventStart").getAsString());
        event.eventEnd = time.fromJson(jo.get("eventEnd").getAsString());
        return event;
    }

}