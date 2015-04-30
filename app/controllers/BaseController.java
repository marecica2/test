package controllers;

import java.util.List;
import java.util.Map;

import models.Attendance;
import models.Event;
import models.Listing;
import models.User;
import play.Logger;
import play.Play;
import play.cache.Cache;
import play.i18n.Lang;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Http.Header;
import utils.NumberUtils;
import utils.UriUtils;

public class BaseController extends Controller
{
    public static final String CONFIG_EMAIL_HOST = "star.configuration.email.host";
    public static final String CONFIG_EMAIL_PORT = "star.configuration.email.port";
    public static final String CONFIG_EMAIL_ACCOUNT = "star.configuration.email.account";
    public static final String CONFIG_EMAIL_PASSWORD = "star.configuration.email.password";

    public static final String CONFIG_BASE_URL = "star.configuration.baseurl";
    public static final String CONFIG_SOCKET_IO = "star.configuration.socketio";
    public static final String CONFIG_RMTP_PATH = "star.configuration.rmtp";
    public static final String CONFIG_STREAM_PATH = "star.configuration.stream";

    public static final String CONFIG_GOOGLE_OAUTH_CALLBACK = "star.google.oauth.callback";

    public static String CONFIG_PAYPAL_PROVIDER_ACCOUNT = "star.configuration.paypal.provider.account";
    public static String CONFIG_PAYPAL_PROVIDER_ACCOUNT_MICROPAYMENT = "star.configuration.paypal.provider.account.micropayment";
    public static String CONFIG_PAYPAL_ENDPOINT = "star.configuration.paypal.endpoint";
    public static String CONFIG_PAYPAL_URL = "star.configuration.paypal.payment.url";

    public static String CONFIG_PAYPAL_USER = "star.configuration.paypal.user";
    public static String CONFIG_PAYPAL_PWD = "star.configuration.paypal.pwd";
    public static String CONFIG_PAYPAL_SIGNATURE = "star.configuration.paypal.signature";
    public static String CONFIG_PAYPAL_PERCENTAGE = "star.configuration.paypal.percentage";

    public static String CONFIG_PAYPAL_ADAPTIVE_ENDPOINT = "star.configuration.paypal.adaptive.endpoint";
    public static String CONFIG_PAYPAL_ADAPTIVE_APPID = "star.configuration.paypal.adaptive.app";
    public static String CONFIG_PAYPAL_ADAPTIVE_URL = "star.configuration.paypal.adaptive.url";
    public static String CONFIG_PAYPAL_ADAPTIVE_OPTIONS_URL = "star.configuration.paypal.adaptive.options.url";
    public static String CONFIG_PAYPAL_ADAPTIVE_DETAILS_URL = "star.configuration.paypal.adaptive.details.url";
    public static String CONFIG_PAYPAL_ADAPTIVE_REFUND_URL = "star.configuration.paypal.adaptive.refund.url";

    @Before
    public static void getRandomChannels()
    {
        List<Listing> listings = (List<Listing>) Cache.get("random");
        if (listings == null)
        {
            listings = Listing.getRandom(9);
            Cache.set("random", listings, "1h");
        }
        renderArgs.put("random", listings);
    }

    public static boolean changeLocale()
    {
        String locale = request.params.get("locale");
        if (locale != null)
            Lang.change(locale);
        return true;
    }

    public static void redirectTo(String url)
    {
        redirect(UriUtils.redirectStr(url));
    }

    public static String getBaseUrl()
    {
        return getProperty(CONFIG_BASE_URL);
    }

    public static Integer getTimezoneOffset()
    {
        if (getLoggedUser() != null && getLoggedUser().timezone != null)
            return getLoggedUser().timezone;
        return NumberUtils.parseInt(request.cookies.get("timezoneJs") != null ? request.cookies.get("timezoneJs").value : "0");
    }

    public static String getBaseUrlWithoutSlash()
    {
        final String url = getProperty(CONFIG_BASE_URL);
        return url.substring(0, url.length() - 1);
    }

    public static boolean flashErrorPut(String msg)
    {
        response.setCookie("error", msg, "1s");
        return true;
    }

    public static String flashErrorGet()
    {
        Http.Cookie c = request.cookies.get("error");
        if (c != null)
            return c.value;
        return null;
    }

    public static String flashErrorClear()
    {
        Http.Cookie c = request.cookies.get("error");
        if (c != null)
            request.cookies.remove("error");
        return null;
    }

    public static void redirectToLogin(String url)
    {
        flash.put("url", url);
        redirect("/login?url=" + UriUtils.urlEncode(url));
    }

    public static User getLoggedUser()
    {
        final String userLogin = getUserLogin();
        if (userLogin != null)
        {
            User u = (User) Cache.get(userLogin);
            if (u != null)
            {
                return u;
            } else
            {
                u = User.getUserByLogin(userLogin);
                u.detach();
                u.userAbout = null;
                u.userEducation = null;
                u.userExperiences = null;
                u.linkedIn = null;
                u.facebook = null;
                u.googlePlus = null;
                u.skype = null;
                u.workingHourEnd = null;
                u.workingHourStart = null;
                u.hiddenDays = null;
                u.password = null;
                u.referrerToken = null;
                u.registrationToken = null;
                u.stylesheet = null;
                u.twitter = null;
                Cache.set(userLogin, u);
                return u;
            }
        }
        return null;
    }

    public static boolean clearUserFromCache()
    {
        final String userLogin = getUserLogin();
        if (userLogin != null)
            Cache.delete(userLogin);
        return true;
    }

    public static User getLoggedUserNotCache()
    {
        final String userLogin = getUserLogin();
        User u = User.getUserByLogin(userLogin);
        return u;
    }

    private static String getUserLogin()
    {
        return Secure.Security.connected();
    }

    public static String getProperty(String key)
    {
        return Play.configuration.getProperty(key);
    }

    public static boolean isProd()
    {
        return Play.mode.isProd();
    }

    public static boolean isUserLogged()
    {
        return Secure.Security.isConnected();
    }

    private static String getReferrerUrl(Map<String, Header> headers)
    {
        final String referer = headers.get("referer") != null ? headers.get("referer").value() : "";
        return referer;
    }

    public static String getReferrerParameter(Map<String, Header> headers, String param)
    {
        final String referer = getReferrerUrl(headers);
        return UriUtils.getSimpleParameter(referer, param);
    }

    static void checkAuthorizedAccess()
    {
        if (Secure.Security.isConnected())
        {
        } else
        {
            flash.put("url", request.url);
            Secure.login();
        }
    }

    static void checkAdminAccess()
    {
        if (Secure.Security.isConnected() && getLoggedUser().isAdmin())
        {
        } else
        {
            flash.put("url", request.url);
            notFound();
        }
    }

    protected static void checkPayment(Event e, String url)
    {
        final User user = getLoggedUser();
        final Attendance attendance = e.getInviteForCustomer(user);

        if (e.isOwner(user))
        {
            Logger.info("Owner, not paying");
            return;
        }

        if (e.isFree())
        {
            Logger.info("Free event, not need to pay");
            return;
        }

        if (!e.isFree() && getLoggedUser() == null)
        {
            flash.put("url", request.url);
            Secure.login();
            return;
        }

        if (!e.isFree() && getLoggedUser() != null)
        {
            // proceed to checkout
            if (attendance == null || attendance.paid == null || attendance.paid != true)
            {
                PaymentController.payment(e.uuid, url);
                return;
            }

            if (attendance != null && attendance.paid)
            {
                Logger.info("Payment is accepted, displaying room");
                return;
            }
        }

        // other payment methods
        error(400, "Bad request");
    }

    public static User getAdminUser()
    {
        User user = User.getUserByLogin(getProperty("admin.user"));
        return user;
    }

}