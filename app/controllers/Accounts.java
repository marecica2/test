package controllers;

import google.GoogleCalendarClient;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.Account;
import models.AccountPlan;
import models.Attendance;
import models.Listing;
import models.User;
import play.cache.Cache;
import play.i18n.Lang;
import play.i18n.Messages;
import play.mvc.With;
import utils.DateTimeUtils;
import utils.StringUtils;

import com.google.api.services.calendar.model.CalendarListEntry;

@With(controllers.Secure.class)
public class Accounts extends BaseController
{
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
            if (account.currentPlan() != null)
                params.put("type", account.currentPlan().type);
            else
                params.put("type", Account.PLAN_STANDARD);

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

        // displaying recurring payments information
        AccountPlan planLast = AccountPlan.getLast(user.account);
        AccountPlan plan = AccountPlan.getCurrentPlan(account);
        if (plan == null)
        {
            plan = new AccountPlan();
            plan.type = Account.PLAN_STANDARD;
        }

        render(user, baseUrl, account, edit, calendars, plan, planLast);
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
        checkAuthenticity();
        final User user = getLoggedUserNotCache();

        final boolean edit = request.params.get("edit") == null ? false : true;
        final Account account = Account.get(user.account.key);
        final String baseUrl = request.getBase();
        validation.required(firstName);
        validation.required(lastName);

        if (!validation.hasErrors())
        {
            account.name = accName;
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

    public static void addFacebook(String facebookName, String facebookId)
    {
        checkAuthenticity();
        final User user = getLoggedUserNotCache();
        final User usr = User.getUserByFacebook(facebookId);
        if (usr != null)
            flash.error(Messages.get("login-already-used"));

        if (user != null && usr == null)
        {
            user.facebookName = facebookName;
            user.facebookId = facebookId;
            user.save();
        }

        flash.keep();
        account();
    }

    public static void googleCalendarClear(String url)
    {
        checkAuthenticity();
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
        checkAuthenticity();
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
            flash.success(Messages.get("your-publisher-request-was-submited"));
            Account account = Account.get(user.account.key);
            account.type = Account.TYPE_PUBLISHER_REQUEST;
            account.save();
            user.save();
        }

        params.flash();
        validation.keep();
        account();
    }

    public static void payments(String transaction, String sender, Boolean sent)
    {
        final DateTimeUtils dt = new DateTimeUtils(DateTimeUtils.TYPE_DATE_ONLY);
        final User user = getLoggedUser();
        final String timeFrom = request.params.get("filterTimeFrom");
        final String timeTo = request.params.get("filterTimeTo");
        final Date from = dt.fromJson(timeFrom);
        final Date to = dt.fromJson(timeTo);
        final User senderUser = User.getUserByLogin(sender);
        User receiverUser = user;

        if (!user.isAdmin() && StringUtils.getStringOrNull(transaction) != null)
            notFound();

        if (user.isAdmin() && transaction != null)
            receiverUser = User.getUserByLogin(transaction);

        List<Attendance> payments = Attendance.getPayments(receiverUser, from, to, senderUser, sent);

        Map<String, BigDecimal> totals = new HashMap<String, BigDecimal>();
        Map<String, BigDecimal> providers = new HashMap<String, BigDecimal>();
        Map<String, BigDecimal> fees = new HashMap<String, BigDecimal>();
        Map<String, Map<Long, BigDecimal>> mapTotal = new HashMap<String, Map<Long, BigDecimal>>();
        for (Attendance attendance : payments)
        {
            if (attendance.refunded == null || !attendance.refunded)
            {
                // init
                if (!totals.containsKey(attendance.currency))
                {
                    mapTotal.put(attendance.currency, new HashMap<Long, BigDecimal>());
                    totals.put(attendance.currency, new BigDecimal(0));
                    providers.put(attendance.currency, new BigDecimal(0));
                    fees.put(attendance.currency, new BigDecimal(0));
                }

                if (attendance.paid)
                {
                    BigDecimal val = attendance.providerPrice;
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(attendance.paypalTransactionDate);
                    cal.set(Calendar.HOUR, 12);
                    cal.set(Calendar.MINUTE, 0);
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
        }
        params.flash();
        render(user, payments, mapTotal, senderUser);
    }

    public static void facebookClear(String url)
    {
        checkAuthenticity();
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
        redirectTo("/settings");
    }

    public static void publisherInfoDismiss(String url)
    {
        User user = getLoggedUserNotCache();
        user.hideInfoPublisher = true;
        user.save();
        redirectTo(url);
        clearUserFromCache();
    }
}