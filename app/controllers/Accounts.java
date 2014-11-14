package controllers;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import models.Account;
import models.Attendance;
import models.Followers;
import models.Listing;
import models.User;
import play.Logger;
import play.cache.Cache;
import play.i18n.Lang;
import play.i18n.Messages;
import play.mvc.With;
import utils.DateTimeUtils;
import utils.JsonUtils;
import utils.RandomUtil;

import com.google.gson.JsonObject;

import email.EmailProvider;

@With(controllers.Secure.class)
public class Accounts extends BaseController
{
    public static void account()
    {
        final boolean edit = request.params.get("edit") == null ? false : true;
        final User user = getLoggedUserNotCache();
        final Account account = user.account;
        final String baseUrl = request.getBase();

        if (edit)
        {
            // account
            params.put("type", account.type);
            params.put("accName", account.name);
            params.put("firstName", user.firstName);
            params.put("lastName", user.lastName);
            params.put("locale", user.locale);
            params.put("emailNotification", user.emailNotification + "");
            params.put("timezone", user.timezone + "");

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

            params.flash();
        }
        render(user, baseUrl, account, edit);
    }

    public static void accountPost(
        String accType,
        String userAbout, String userExperiences, String userEducation,
        String skype, String googlePlus, String linkedIn, String twitter, String facebook,
        String url, String locale, String accName, Boolean emailNotification,
        String paypal, String currency,
        String firstName, String lastName, String smtpHost, String smtpPort,
        String smtpAccount, String smtpPassword, String smtpProtocol,
        String avatarUrl,
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
            account.type = accType;
            account.name = accName;
            account.url = url;

            user.timezone = timezone;
            user.firstName = firstName;
            user.lastName = lastName;
            user.locale = locale;
            user.emailNotification = emailNotification;

            user.userAbout = userAbout;
            user.userEducation = userEducation;
            user.userExperiences = userExperiences;
            user.facebook = facebook;
            user.googlePlus = googlePlus;
            user.twitter = twitter;
            user.linkedIn = linkedIn;
            user.skype = skype;

            account.smtpHost = smtpHost;
            account.smtpPort = smtpPort;
            account.smtpAccount = smtpAccount;
            account.smtpPassword = smtpPassword;
            account.smtpProtocol = smtpProtocol;
            account.save();

            account.paypalAccount = paypal;
            account.currency = currency;

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

    public static void requestPublisher(String url)
    {
        User user = getLoggedUserNotCache();
        List<Listing> listings = Listing.getForUser(user);
        clearUserFromCache();

        if (listings == null || listings.size() == 0)
        {
            flash.put("error", "");
            validation.addError("listing", "You need at least one listing.");
        }
        if (user.userAbout == null || user.userAbout.length() < 10)
        {
            flash.put("error", "");
            validation.addError("about", "Please fill in information about you.");
        }

        if (!validation.hasErrors())
        {
            Account account = Account.get(user.account.key);
            account.type = Account.TYPE_PUBLISHER_REQUEST;
            account.requestTime = new Date();
            account.save();
            user.save();
        }

        params.flash(); // add http parameters to the flash scope
        validation.keep(); // keep the errors for the next request
        account();
    }

    public static void follow(String id, String url)
    {
        final User user = getLoggedUser();
        final User userToFollow = User.getUserByUUID(id);
        Followers f = new Followers();
        f.followSource = user;
        f.followTarget = userToFollow;
        f.uuid = RandomUtil.getUUID();
        f.created = new Date();
        f.save();
        redirectTo(url);
    }

    public static void unfollow(String id, String url)
    {
        final User user = getLoggedUser();
        final User userTarget = User.getUserByUUID(id);
        Followers f = Followers.get(user, userTarget);
        f.delete();
        redirectTo(url);
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

    public static void resetEmail(String url)
    {
        User user = getLoggedUserNotCache();
        user.account.smtpHost = "DEFAULT";
        user.account.smtpPort = null;
        user.account.smtpProtocol = null;
        user.account.smtpAccount = null;
        user.account.smtpPassword = null;
        user.account.save();
        redirectTo(url);
    }

    public static void testEmail()
    {
        try
        {
            final User user = getLoggedUser();
            EmailProvider em = new EmailProvider(user.account.smtpHost, user.account.smtpPort, user.account.smtpAccount, user.account.smtpPassword, "10000", user.account.smtpProtocol, true);
            em.sendMessage(user.login, Messages.get("email.test.subject"), Messages.get("email.test.body"));
            renderJSON("{\"response\":\"" + Messages.get("email.test.response.ok") + "\"}");
        } catch (Exception e)
        {
            Logger.error(e, "Error occured while sending notification");
            response.status = 500;
            renderJSON(Messages.get("email.test.response.error") + " " + e.getMessage());
        }
    }

    public static void setAgendaType()
    {
        final User user = getLoggedUserNotCache();
        final JsonObject jo = JsonUtils.getJson(request.body);
        final String type = jo.get("type").getAsString();
        user.agendaType = type;
        user.save();
        clearUserFromCache();
        renderJSON("ok");
    }

    public static void payments()
    {
        final DateTimeUtils dt = new DateTimeUtils(DateTimeUtils.TYPE_DATE_ONLY);
        final User user = getLoggedUser();
        final Account account = user.account;
        final String timeFrom = request.params.get("filterTimeFrom");
        final String timeTo = request.params.get("filterTimeTo");
        final Date from = dt.fromJson(timeFrom);
        final Date to = dt.fromJson(timeTo);

        BigDecimal total = new BigDecimal("0.0");
        BigDecimal provider = new BigDecimal("0.0");
        BigDecimal fee = new BigDecimal("0.0");

        List<Attendance> payments = Attendance.getPayments(account, from, to);
        Map<Long, BigDecimal> mapTotal = new LinkedHashMap<Long, BigDecimal>();
        for (Attendance attendance : payments)
        {
            if (attendance.price != null)
                total = total.add(attendance.price);
            if (attendance.providerPrice != null)
                provider = provider.add(attendance.providerPrice);
            if (attendance.fee != null)
                fee = fee.add(attendance.fee);

            if (attendance.transactionDate != null)
            {
                BigDecimal val = attendance.providerPrice;
                Calendar cal = Calendar.getInstance();
                cal.setTime(attendance.transactionDate);
                cal.set(Calendar.HOUR, 1);
                cal.set(Calendar.MINUTE, 1);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                Long date = cal.getTimeInMillis();

                if (mapTotal.containsKey(date))
                {
                    val = mapTotal.get(date);
                    val = val.add(attendance.providerPrice);
                    mapTotal.put(date, val);
                } else
                {
                    mapTotal.put(date, val);
                }
            }
        }
        params.flash();
        render(user, account, payments, total, provider, fee, mapTotal);
    }

    public static void contacts()
    {
        final User user = getLoggedUser();
        final Account account = user.account;
        List<User> customers = User.getCustomersForAccount(account, null);
        render(customers, account, user);
    }

    public static void passwordReset()
    {
        User user = getLoggedUser();
        render(user);
    }
}