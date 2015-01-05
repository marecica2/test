package controllers;

import google.GoogleCalendarClient;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.Account;
import models.Attendance;
import models.Listing;
import models.User;
import play.cache.Cache;
import play.i18n.Lang;
import play.i18n.Messages;
import play.mvc.With;
import utils.DateTimeUtils;
import utils.JsonUtils;
import utils.StringUtils;

import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.gson.JsonObject;

@With(controllers.Secure.class)
public class Accounts extends BaseController
{
    public static void setStyle()
    {
        final User user = getLoggedUserNotCache();
        final JsonObject jo = JsonUtils.getJson(request.body);
        final String stylesheet = jo.get("stylesheet") != null ? jo.get("stylesheet").getAsString() : null;
        final String pattern = jo.get("pattern") != null ? jo.get("pattern").getAsString() : null;
        final String layout = jo.get("layout") != null ? jo.get("layout").getAsString() : null;
        final String footer = jo.get("footer") != null ? jo.get("footer").getAsString() : null;
        if (pattern != null)
            user.pattern = pattern;
        if (stylesheet != null)
            user.stylesheet = stylesheet;
        if (layout != null)
            user.layout = layout;
        if (footer != null)
            user.footer = footer;
        user.save();
        Cache.delete(user.login);
        renderJSON("ok");
    }

    public static void account()
    {
        final boolean edit = request.params.get("edit") == null ? false : true;
        final User user = getLoggedUserNotCache();
        final Account account = user.account;
        final String baseUrl = request.getBase();
        List<CalendarListEntry> calendars = null;

        if (edit)
        {
            // account
            params.put("type", account.currentPlan());
            params.put("accName", account.name);
            params.put("firstName", user.firstName);
            params.put("lastName", user.lastName);
            params.put("locale", user.locale);
            params.put("emailNotification", user.emailNotification + "");
            params.put("timezone", user.timezone + "");
            params.put("facebookId", user.facebookId);
            params.put("facebookName", user.facebookName);

            params.put("userAbout", user.userAbout);
            params.put("userEducation", user.userEducation);
            params.put("userExperiences", user.userExperiences);

            // about
            params.put("url", account.url);
            params.put("skype", user.skype);
            params.put("facebook", user.facebook);
            params.put("googlePlus", user.googlePlus);
            params.put("twitter", user.twitter);
            params.put("linkedIn", user.linkedIn);

            // calendar
            params.put("workingHourStart", user.workingHourStart);
            params.put("workingHourEnd", user.workingHourEnd);
            params.put("hiddenDays", user.hiddenDays == null ? "" : user.hiddenDays);

            // paypal
            params.put("paypal", account.paypalAccount);
            params.put("currency", account.currency);

            // email
            params.put("smtpHost", account.smtpHost);
            params.put("smtpPort", account.smtpPort);
            params.put("smtpProtocol", account.smtpProtocol);
            params.put("smtpAccount", account.smtpAccount);
            params.put("smtpPassword", account.smtpPassword);
            params.put("smtpProtocol", account.smtpProtocol);

            if (user.googleTokenExpires != null)
                calendars = GoogleCalendarClient.getCalendars(user);

            params.flash();
        }
        render(user, baseUrl, account, edit, calendars);
    }

    public static void accountPost(
        String accPlan,
        String userAbout, String userExperiences, String userEducation,
        String skype, String googlePlus, String linkedIn, String twitter, String facebook, String facebookName,
        String url, String locale, String accName, Boolean emailNotification, String facebookId,
        String paypal, String currency,
        String firstName, String lastName, String smtpHost, String smtpPort,
        String smtpAccount, String smtpPassword, String smtpProtocol,
        String avatarUrl, String googleCalendarId,
        Integer timezone, String workingHourStart, String workingHourEnd, String[] hiddenDays, String imageId, String imageUrl
        )
    {
        validation.required(firstName);
        validation.required(lastName);

        final boolean edit = request.params.get("edit") == null ? false : true;
        final User user = getLoggedUserNotCache();
        final Account account = Account.get(user.account.key);
        final String baseUrl = request.getBase();

        if (!validation.hasErrors())
        {

            if (account.planRequestFrom == null || (account.planRequestFrom.getTime() < System.currentTimeMillis()))
            {
                account.planCurrent = account.planRequest;
                account.planRequest = accPlan;

                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.MONTH, 1);
                calendar.set(Calendar.DATE, calendar.getActualMinimum(Calendar.DAY_OF_MONTH));
                calendar.set(Calendar.HOUR_OF_DAY, 0);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                Date nextMonthFirstDay = calendar.getTime();
                account.planRequestFrom = nextMonthFirstDay;
            }
            account.name = accName;
            account.requestTime = new Date();
            account.url = url;
            account.smtpHost = smtpHost;
            account.smtpPort = smtpPort;
            account.smtpAccount = smtpAccount;
            account.smtpPassword = smtpPassword;
            account.smtpProtocol = smtpProtocol;
            account.paypalAccount = paypal;
            account.currency = currency;
            account.save();

            user.facebookId = facebookId;
            user.facebookName = facebookName;
            user.timezone = timezone;
            user.firstName = firstName;
            user.lastName = lastName;
            user.locale = locale;
            user.emailNotification = emailNotification;
            user.userAbout = StringUtils.htmlEscape(userAbout);
            user.userEducation = StringUtils.htmlEscape(userEducation);
            user.userExperiences = StringUtils.htmlEscape(userExperiences);
            user.facebook = facebook;
            user.googlePlus = googlePlus;
            user.twitter = twitter;
            user.linkedIn = linkedIn;
            user.skype = skype;
            if (googleCalendarId != null)
                user.googleCalendarId = googleCalendarId;

            if (imageUrl != null)
            {
                user.avatarUrl = imageUrl;
                user.avatarId = imageId;
            }
            user.workingHourEnd = workingHourEnd;
            user.workingHourStart = workingHourStart;
            String hidden = "";
            if (hiddenDays != null)
            {
                for (int i = 0; i < hiddenDays.length; i++)
                {
                    hidden += hiddenDays[i] + (i == (hiddenDays.length - 1) ? "" : ",");
                }
            }
            user.hiddenDays = hidden;

            user.save(account);
            Cache.delete(user.login);
            Lang.change(locale);
            redirect("/settings");
        } else
        {
            params.flash();
        }
        render("Accounts/account.html", user, baseUrl, account, edit);
    }

    public static void googleCalendarClear(String url)
    {
        final User user = getLoggedUserNotCache();

        GoogleOAuth.revokeToken();

        user.googleAccessToken = null;
        user.googleCalendarId = null;
        user.googleRefreshToken = null;
        user.googleTokenExpires = null;
        user.save();
        clearUserFromCache();
        redirectTo(url);
    }

    public static void requestPublisher(String url)
    {
        User user = getLoggedUserNotCache();
        List<Listing> listings = Listing.getForUser(user);
        clearUserFromCache();

        if (listings == null || listings.size() == 0)
        {
            validation.addError("type", "");
            flash.error(Messages.get("you-need-at-least-one-listing"));
        }
        if (user.userAbout == null || user.userAbout.length() < 5)
        {
            validation.addError("type", "");
            flash.error(Messages.get("please-fill-in-information-about-you"));
        }

        if (!validation.hasErrors())
        {
            Account account = Account.get(user.account.key);
            account.type = Account.TYPE_PUBLISHER_REQUEST;
            account.requestTime = new Date();
            account.save();
            user.save();
        }

        params.flash();
        validation.keep();
        account();
    }

    public static void checkConnection(String id, String url)
    {
        final User user = getLoggedUserNotCache();
        user.lastOnlineTime = new Date();
        user.save();
        JsonObject resp = new JsonObject();
        resp.addProperty("result", "ok");
        renderJSON(resp);
    }

    public static void payments()
    {
        final DateTimeUtils dt = new DateTimeUtils(DateTimeUtils.TYPE_DATE_ONLY);
        final User user = getLoggedUser();
        final String timeFrom = request.params.get("filterTimeFrom");
        final String timeTo = request.params.get("filterTimeTo");
        final Date from = dt.fromJson(timeFrom);
        final Date to = dt.fromJson(timeTo);

        Map<String, BigDecimal> totals = new HashMap<String, BigDecimal>();
        Map<String, BigDecimal> providers = new HashMap<String, BigDecimal>();
        Map<String, BigDecimal> fees = new HashMap<String, BigDecimal>();

        List<Attendance> payments = Attendance.getPayments(user, from, to);
        Map<String, Map<Long, BigDecimal>> mapTotal = new HashMap<String, Map<Long, BigDecimal>>();
        for (Attendance attendance : payments)
        {
            // init
            if (!totals.containsKey(attendance.currency))
            {
                mapTotal.put(attendance.currency, new HashMap<Long, BigDecimal>());
                totals.put(attendance.currency, new BigDecimal(0));
                providers.put(attendance.currency, new BigDecimal(0));
                fees.put(attendance.currency, new BigDecimal(0));
            }

            if (attendance.paypalTransactionDate != null)
            {
                BigDecimal val = attendance.providerPrice;
                Calendar cal = Calendar.getInstance();
                cal.setTime(attendance.paypalTransactionDate);
                cal.set(Calendar.HOUR, 1);
                cal.set(Calendar.MINUTE, 1);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                Long date = cal.getTimeInMillis();

                if (mapTotal.get(attendance.currency).containsKey(date))
                {
                    val = mapTotal.get(attendance.currency).get(date);
                    val = val.add(attendance.providerPrice);
                    mapTotal.get(attendance.currency).put(date, val);
                } else
                {
                    mapTotal.get(attendance.currency).put(date, val);
                }
            }
        }
        params.flash();
        render(user, payments, mapTotal);
    }

    public static void facebookClear(String url)
    {
        User user = getLoggedUserNotCache();
        if (user == null)
            forbidden();
        user.facebookId = null;
        user.facebookName = null;
        user.facebookPageChannel = null;
        user.facebookPageType = null;
        user.facebookTab = null;
        user.facebook = null;
        user.save();
        redirectTo(url);
    }
}