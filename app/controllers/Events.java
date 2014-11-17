package controllers;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import models.Account;
import models.Activity;
import models.Attendance;
import models.Comment;
import models.Event;
import models.FileUpload;
import models.Followers;
import models.Listing;
import models.Message;
import models.Rating;
import models.User;
import play.Logger;
import play.mvc.Before;
import templates.VelocityTemplate;
import utils.DateTimeUtils;
import utils.JsonUtils;
import utils.NetUtils;
import utils.NumberUtils;
import utils.RandomUtil;
import utils.StringUtils;

import com.google.gson.JsonObject;

import dto.EventDTO;
import dto.UserDTO;
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

        List<EventDTO> eventsDto = new ArrayList<EventDTO>();

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

        final Boolean isOwner = user != null ? event.isOwner(user) : false;

        if (isOwner)
        {
            eDto.isEditable = true;
            eventsDto.add(eDto);
        } else if (user != null && event.hasInviteFor(user.login) && event.state.equals(Event.EVENT_STATE_CUSTOMER_CREATED))
        {
            eDto.isEditable = true;
            eventsDto.add(eDto);

        } else if (user != null && !event.hasInviteFor(user.login))
        {
            eDto.isInvited = false;
            eDto = EventDTO.postProcessHiddenEvent(eDto);
            eventsDto.add(eDto);

        } else if (user != null && !event.state.equals(Event.EVENT_STATE_CUSTOMER_CREATED))
        {
            eDto.isEditable = false;
            eventsDto.add(eDto);
        } else if (event.listing.privacy.equals(Event.EVENT_VISIBILITY_PUBLIC))
        {
            eventsDto.add(eDto);

        }
    }

    public static void eventNew(String action, String uuid, String url, String type, String listingId)
    {
        final Boolean edit = action != null && action.equals("edit") ? true : false;
        final Boolean isPublic = false;
        final User user = getLoggedUser();
        final Account account = user != null ? user.account : null;
        final Event event = Event.get(uuid);
        final Listing listing = event != null ? event.listing : Listing.get(listingId);
        final Boolean isOwner = event != null ? event.isOwner(user) : false;
        final Boolean fromEvent = true;

        final Followers follow = event != null && user != null ? Followers.get(user, event.user) : null;
        final List<FileUpload> files = event != null ? FileUpload.getByObject(event.uuid) : null;
        final List<Listing> listings = edit ? Listing.getForUser(user) : null;

        final String temp = RandomUtil.getDoubleUUID();
        final String commentTemp = RandomUtil.getDoubleUUID();
        final List<Comment> comments = event != null ? Comment.getByObject(event.uuid) : null;

        final List<Rating> ratings = event != null ? Rating.getByObject(uuid) : null;
        final Map<String, Object> stats = event != null ? Rating.calculateStats(ratings) : null;

        Attendance attendance = null;
        if (event != null)
        {
            for (Attendance a : event.attendances)
            {
                if (a.customer != null && a.customer.equals(user))
                    attendance = a;
            }
        }

        if (event != null && type != null && type.equals("broadcast") && !event.isOwner(user))
            forbidden();

        if (event == null && !edit)
            notFound();

        if (event != null && edit && !isOwner && !event.state.equals(Event.EVENT_STATE_CUSTOMER_CREATED))
            forbidden();

        if (event != null && !event.listing.privacy.equals(Event.EVENT_VISIBILITY_PUBLIC))
        {
            if (user == null)
                checkAccess();
            if (!event.hasInviteFor(user.login) && !event.isOwner(user))
                forbidden();
        }

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

            if (event.getChatEnabled() != null && event.getChatEnabled())
                params.put("chatEnabled", "true");

            params.put("type", event.getType());
            params.put("privacy", event.getPrivacy());
            params.put("currency", event.getCurrency());
            params.put("charging", event.getCharging());
            params.put("price", event.getPrice());
        } else
        {
            params.put("type", listing.type);
            params.put("privacy", listing.privacy);
            params.put("charging", listing.charging);
            params.put("price", listing.price);
            params.put("currency", user.account.currency);
        }
        params.put("temp", temp);
        params.flash();

        final String name = user != null ? user.getFullName() : null;
        final String room = event != null ? event.uuid : null;
        final String rmtp = getProperty(CONFIG_RMTP_PATH);
        String serverIp = NetUtils.getIp();
        if (isProd() || serverIp == null)
            serverIp = getProperty(CONFIG_SERVER_DOMAIN);
        serverIp = "localhost";
        serverIp = "192.168.1.100";
        //serverIp = "192.168.2.81";
        Map<String, String> errs = new HashMap<String, String>();
        render("Listings/listingNew.html", user, account, isPublic, isOwner, edit, event, url, errs, name, room, rmtp,
                serverIp, type, follow, attendance, files, temp, commentTemp, comments, ratings, stats, listings, listing, fromEvent);
    }

    public static void eventNewPost(
        String action,
        String title,
        String description,
        String eventStart,
        String eventDate,
        String eventEnd,
        String privacy,
        String charging,
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
        String listingId,
        String temp
        )
    {
        Event event = Event.get(uuid);
        boolean edit = action != null && action.equals("edit") ? true : false;
        final Boolean isPublic = false;
        final User user = getLoggedUser();
        if (event != null)
            listingId = event.listing.uuid;
        final Listing listing = Listing.get(listingId);
        final Account account = user.account;
        final Map<String, String> errs = new HashMap<String, String>();
        final DateTimeUtils dt = new DateTimeUtils();

        validation.required(eventStart);
        validation.required(eventEnd);
        validation.required(eventDate);
        Date eventSt = dt.parseDate(eventDate + " " + eventStart, new SimpleDateFormat("dd.MM.yyyy HH:mm"));
        Date eventEn = dt.parseDate(eventDate + " " + eventEnd, new SimpleDateFormat("dd.MM.yyyy HH:mm"));
        eventSt = new Date(eventSt.getTime() + ((Integer.parseInt(offset) * 1000 * 60)));
        eventEn = new Date(eventEn.getTime() + ((Integer.parseInt(offset) * 1000 * 60)));
        validation.required(eventEn);
        validation.required(eventSt);
        if (eventSt.compareTo(eventEn) > 0)
            errs.put("time", "Invalid time range");

        if (!validation.hasErrors() && errs.size() == 0)
        {
            boolean create = true;
            if (event == null)
            {
                event = new Event();
                event.listing = listing;
                event.listing_uuid = listing.uuid;
                event.uuid = temp;
                event.roomSecret = RandomUtil.getDoubleUUID();
                event.created = new Date();
                event.user = user;
                event.account = account;
                event.createdByUser = true;
                event.state = Event.EVENT_STATE_USER_CREATED;
                //event.listing.imageUrl = FileuploadController.PATH_TO_IMAGES + "ava_" + RandomUtil.getRandomInteger(22) + ".png";
            } else
            {
                create = false;
            }
            event.eventStart = eventSt;
            event.eventEnd = eventEn;
            event.lastModified = new Date();
            event.charging = charging.equals(event.listing.charging) ? null : charging;
            event.privacy = charging.equals(event.listing.privacy) ? null : privacy;
            event.price = price.equals(event.listing.price) ? null : price;
            event.currency = currency.equals(event.listing.currency) ? null : currency;
            event.type = type.equals(event.listing.type) ? null : type;
            event.chatEnabled = chatEnabled != null && chatEnabled.equals("chatEnabled") ? true : false;
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
            redirect("/event-detail?action=view&uuid=" + event.uuid);
        }
        params.flash();
        render("Events/eventNew.html", user, account, isPublic, edit, event, errs, listing);
    }

    public static void eventSaveRest()
    {
        try
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
            event.uuid = RandomUtil.getDoubleUUID();
            event.roomSecret = RandomUtil.getDoubleUUID();
            event.listing.imageUrl = FileuploadController.PATH_TO_LISTING_AVATARS + "ava_" + RandomUtil.getRandomInteger(22) + ".png";
            if (proposal)
            {
                user = User.getUserByUUID(userId);
                event.listing.privacy = Event.EVENT_VISIBILITY_PRIVATE;
                event.state = Event.EVENT_STATE_CUSTOMER_CREATED;
                event.listing.type = Event.EVENT_TYPE_P2P_CALL;
                event.customer = customer;
                event.createdByUser = false;
                event.user = user;
                event.account = user.account;

            } else
            {
                event.listing.privacy = Event.EVENT_VISIBILITY_PRIVATE;
                event.state = Event.EVENT_STATE_USER_CREATED;
                event.listing.type = Event.EVENT_TYPE_P2P_CALL;
                event.createdByUser = true;
                event.user = user;
                event.account = user.account;

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
        } catch (Exception e)
        {
            Logger.error(e, e.getMessage());
            response.status = 500;
            renderJSON("Failed to create event. Cause: " + e.getMessage());
        }
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
                a.name = user.getFullName();
                a.account = user.account;
                a.email = user.login;
                a.event = event;
                a.customer = user;
                a.isForUser = true;
                if (!isPublicRequest(request.headers))
                    a.result = Attendance.ATTENDANCE_RESULT_ACCEPTED;
                a.save(user.account);

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
                a.account = user.account;
                a.email = user.login;
                a.event = event;
                a.customer = user;
                a.isForUser = true;
                a.save(user.account);

                // create attendance for customer
                Attendance a1 = new Attendance();
                a1.account = customer.account;
                a1.email = customer.login;
                a1.name = customer.getFullName();
                a1.customer = customer;
                a1.event = event;
                a1.result = Attendance.ATTENDANCE_RESULT_ACCEPTED;
                a1.isForUser = false;
                a1.save(user.account);
            }
        }
    }

    public static void eventUpdateRest()
    {
        try
        {
            final User user = getLoggedUser();
            final JsonObject jo = JsonUtils.getJson(request.body);
            final String uuid = jo.get("uuid").getAsString();
            final Boolean timeChanged = jo.get("changedTime") != null ? true : false;
            System.err.println(timeChanged);
            final DateTimeUtils time = new DateTimeUtils(DateTimeUtils.TYPE_OTHER);
            boolean createActivity = false;
            Event event = Event.get(uuid);

            if (isPublicRequest(request.headers))
            {
                // allow to edit event for customer only in this state 
                if (event != null && event.state != null && event.state.equals(Event.EVENT_STATE_CUSTOMER_CREATED))
                {
                    Event e = new Event();
                    e = eventFromJson(time, jo, e);
                    event.listing.description = e.listing.description;
                    event.listing.title = e.listing.title;
                    event.listing.color = e.listing.color;
                    event.eventEnd = e.eventEnd;
                    event.listing.currency = e.listing.currency;
                    event.eventStart = e.eventStart;
                    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
                    event.lastModified = new Date(cal.getTimeInMillis());
                    event.save();
                    createActivity = true;
                }
            } else
            {
                event = eventFromJson(time, jo, event);
                event.lastModified = new Date();
                event = event.save();
                createActivity = true;
            }

            // create activity
            if (createActivity)
            {
                final Activity act = new Activity();
                if (timeChanged)
                    act.type = Activity.ACTIVITY_EVENT_MOVED;
                else
                    act.type = Activity.ACTIVITY_EVENT_UPDATED_BY_USER;
                act.user = user;
                act.event = event;
                act.eventName = event.listing.title;
                act.saveActivity();
            }

            renderJSON(EventDTO.convert(event, user));
        } catch (Exception e)
        {
            e.printStackTrace();
            response.status = 500;
            renderJSON("Failed to update event. Cause: " + e.getMessage());
        }

    }

    public static void eventDeleteRest()
    {
        try
        {
            final User user = getLoggedUser();
            final JsonObject jo = JsonUtils.getJson(request.body);
            final String uuid = jo.get("uuid").getAsString();
            final Event event = Event.get(uuid);
            event.deleteEvent();
            renderJSON(EventDTO.convert(event, user));
        } catch (Exception e)
        {
            e.printStackTrace();
            response.status = 500;
            renderJSON("Failed to delete event. Cause: " + e.getMessage());
        }
    }

    public static void approve(String event, String url)
    {
        try
        {
            final User user = getLoggedUser();
            final Account account = getAccountByUser();
            //final JsonObject jo = JsonUtils.getJson(request.body);
            Event e = Event.get(event, account);
            e.state = Event.EVENT_STATE_USER_ACCEPTED;
            e.save();

            final Activity act = new Activity();
            act.type = Activity.ACTIVITY_EVENT_APPROVED;
            act.user = user;
            act.event = e;
            act.eventName = e.listing.title;
            act.saveActivity();

            redirectTo(url);
        } catch (Exception e)
        {
            e.printStackTrace();
            redirect("/");
        }
    }

    public static void decline(String event, String url)
    {
        try
        {
            final User user = getLoggedUser();
            final Account account = getAccountByUser();
            Event e = Event.get(event, account);
            e.state = Event.EVENT_STATE_USER_DECLINED;
            e.archived = true;
            e.lastModified = new Date();
            e.save();

            final Activity act = new Activity();
            act.type = Activity.ACTIVITY_EVENT_DECLINED;
            act.user = user;
            act.eventName = e.listing.title;
            act.event = e;
            act.saveActivity();

            redirectTo(url);
        } catch (Exception e)
        {
            e.printStackTrace();
            redirectTo(url);
        }
    }

    public static void invites(String id, String str)
    {
        final User user = getLoggedUser();
        final Account account = user.account;
        List<User> c = User.getCustomersForAccount(account, str);
        List<UserDTO> customers = new ArrayList<UserDTO>();
        for (User customer : c)
            customers.add(UserDTO.convert(customer));
        renderJSON(customers);
    }

    public static void eventInvite(String message, String eventId, String url, String[] invite)
    {
        final Event event = Event.get(eventId);
        final User user = getLoggedUser();
        final Account account = user.account;
        final String baseUrl = getProperty(BaseController.CONFIG_BASE_URL);
        final EmailProvider emailProvider = new EmailProvider(account.smtpHost, account.smtpPort, account.smtpAccount, account.smtpPassword, "10000", account.smtpProtocol, true);

        if (invite != null)
        {
            for (int i = 0; i < invite.length; i++)
            {
                final Attendance attendance = Attendance.get(invite[i]);
                final String subject = event.listing.title;
                String locale = "en";
                if (attendance.isForUser && attendance.user != null)
                    locale = attendance.user.locale;
                if (!attendance.isForUser && attendance.customer != null)
                    locale = attendance.customer.locale;
                final String htmlPart = VelocityTemplate.processEventInvitationTemplate(event, attendance, baseUrl, message);

                if (message != null && message.length() > 0 && attendance.customer != null)
                {
                    Message mail = new Message();
                    mail.body = message;
                    mail.created = new Date();
                    mail.fromUser = user;
                    mail.toUser = attendance.customer;
                    mail.subject = subject;
                    mail.uuid = RandomUtil.getDoubleUUID();
                    mail.save();
                    attendance.customer.unreadMessages = true;
                    attendance.customer.save();
                }

                // notify by email
                if ((attendance.customer == null && attendance.user == null) || (!attendance.isForUser && attendance.customer != null && attendance.customer.emailNotification))
                {
                    try
                    {
                        final String from = user.login;
                        new Notification(emailProvider, from, subject, attendance.email, htmlPart).execute();
                    } catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }
        }
        redirectTo(url);
    }

    private static Event eventFromJson(DateTimeUtils time, final JsonObject jo, Event event)
    {
        event.eventStart = time.fromJson(StringUtils.htmlEscape(jo.get("eventStart").getAsString()));
        event.eventEnd = time.fromJson(StringUtils.htmlEscape(jo.get("eventEnd").getAsString()));
        //event.listing.currency = jo.get("currency") != null ? StringUtils.htmlEscape(jo.get("currency").getAsString()) : "";
        return event;
    }

}