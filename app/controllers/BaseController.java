package controllers;

import java.util.Date;
import java.util.Map;

import models.Attendance;
import models.Event;
import models.User;

import org.apache.http.client.utils.URIBuilder;

import payments.Paypal;
import payments.Paypal.AccessToken;
import payments.Paypal.DoExpressCheckoutResponse;
import play.Logger;
import play.Play;
import play.cache.Cache;
import play.libs.Crypto;
import play.mvc.Controller;
import play.mvc.Http.Header;
import utils.RandomUtil;
import utils.UriUtils;

public class BaseController extends Controller
{
    public static final String CONFIG_BASE_URL = "star.configuration.baseurl";
    public static final String CONFIG_SOCKET_IO = "star.configuration.socketio";
    public static final String CONFIG_RMTP_PATH = "star.configuration.rmtp";
    public static final String CONFIG_STREAM_PATH = "star.configuration.stream";

    public static final String CONFIG_GOOGLE_OAUTH_CALLBACK = "star.google.oauth.callback";

    public static final String CONFIG_PAYPAL_PROVIDER_ACCOUNT = "star.configuration.paypal.provider.account";
    public static final String CONFIG_PAYPAL_PROVIDER_ACCOUNT_MICROPAYMENT = "star.configuration.paypal.provider.account.micropayment";
    public static final String CONFIG_PAYPAL_ENDPOINT = "star.configuration.paypal.endpoint";
    public static final String CONFIG_PAYPAL_URL = "star.configuration.paypal.payment.url";
    public static final String CONFIG_PAYPAL_USER = "star.configuration.paypal.user";
    public static final String CONFIG_PAYPAL_PWD = "star.configuration.paypal.pwd";
    public static final String CONFIG_PAYPAL_SIGNATURE = "star.configuration.paypal.signature";
    public static final String CONFIG_PAYPAL_PERCENTAGE = "star.configuration.paypal.percentage";

    public static void redirectTo(String url)
    {
        redirect(UriUtils.redirectStr(url));
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
                Cache.set(userLogin, u);
                return u;
            }
        }
        return null;
    }

    public static boolean clearUserFromCache()
    {
        final String userLogin = getUserLogin();
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

    protected static void checkPayment(Event e, String url) throws Throwable
    {
        final User user = getLoggedUser();
        final String transactionId = params.get("transactionId");
        Attendance attendance = e.getInviteForCustomer(user);

        if (e.isOwner(user))
        {
            Logger.info("Owner, not paying");
            return;
        }

        if (e.isFree())
        {
            Logger.info("Free event, not needed to log in");
            return;
        }

        if (!e.isFree() && getLoggedUser() == null)
        {
            Logger.info("Redirect to login");
            flash.put("url", request.url);
            Secure.login();
            return;
        }

        if (!e.isFree() && getLoggedUser() != null)
        {
            if (attendance == null)
                attendance = createAttendanceForCustomerEvent(e, user);

            // proceed to payment
            if ((attendance.paid == null || attendance.paid != true))
            {
                renderTemplate("PaymentController/payment.html", user, e, url);
            } else
            {
                Logger.info("Payment is accepted, displaying room");
                return;
            }
        }

        if (!e.isFree() && getLoggedUser() != null)
        {
            // in case of public event and missing attendance create it for the logged customer on the fly
            if (attendance == null)
                attendance = createAttendanceForCustomerEvent(e, user);

            String paypalCancelUrl = getProperty(BaseController.CONFIG_BASE_URL) + url.substring(1);
            String paypalRedirectUrl = new URIBuilder(paypalCancelUrl)
                    .addParameter("transactionId", Crypto.encryptAES(user.uuid))
                    .addParameter("url", url)
                    .build()
                    .toString();

            final Paypal pp = new Paypal(e, paypalRedirectUrl, paypalCancelUrl,
                    getProperty(CONFIG_PAYPAL_PROVIDER_ACCOUNT),
                    getProperty(CONFIG_PAYPAL_PROVIDER_ACCOUNT_MICROPAYMENT),
                    getProperty(CONFIG_PAYPAL_USER),
                    getProperty(CONFIG_PAYPAL_PWD),
                    getProperty(CONFIG_PAYPAL_SIGNATURE),
                    getProperty(CONFIG_PAYPAL_ENDPOINT),
                    getProperty(CONFIG_PAYPAL_URL),
                    getProperty(CONFIG_PAYPAL_PERCENTAGE)
                    );

            // payment received
            final String payerId = request.params.get("PayerID");
            if (transactionId != null && Crypto.decryptAES(transactionId).equals(user.uuid) && payerId != null)
            {
                attendance.paypalPayerId = payerId;
                final DoExpressCheckoutResponse resp = pp.doExpressCheckoutDual(attendance.paypalAccessToken, attendance.paypalPayerId, e);

                if (resp.success)
                {
                    attendance.paid = true;
                    attendance.paypalTransactionDate = new Date();
                    attendance.fee = resp.fee;
                    attendance.price = resp.price;
                    attendance.providerPrice = resp.providerPrice;
                    attendance.currency = e.user.account.currency;
                    attendance.paypalAccount = e.user.account.paypalAccount;
                    attendance.paypalAccountProvider = resp.providerAccount;
                    attendance.paypalTransactionId = resp.transactionIdProvider;
                    attendance.paypalTransactionIdProvider = resp.transactionIdOur;
                    attendance.save();
                    redirectTo(request.params.get("url"));
                } else
                {
                    Logger.error(resp.errorMessage);
                    flash.put("paypalError", resp.errorMessage);
                    redirect(request.params.get("url"));
                }
            }

            // proceed to payment
            if ((attendance.paid == null || attendance.paid != true))
            {
                // display payment form
                if (request.method.equals("GET"))
                    renderTemplate("PaymentController/payment.html", user, e, url);

                if (request.method.equals("POST"))
                {
                    AccessToken token = pp.setExpressCheckoutDual(e);
                    attendance.paypalAccessToken = token.getToken();
                    attendance.paypalAccessTokenValidity = token.getValidity();
                    attendance.paymentMethod = "paypal";
                    attendance.save();
                    redirect(pp.getPaymentUrl(token.getToken()));
                }
            } else
            {
                Logger.info("Payment is accepted, displaying room");
                return;
            }
        }

        // other payment methods

        error(400, "Bad request");
    }

    private static Attendance createAttendanceForCustomerEvent(Event e, User customer)
    {
        Attendance attendance;
        attendance = new Attendance();
        attendance.event = e;
        attendance.uuid = RandomUtil.getUUID();
        attendance.customer = customer;
        attendance.created = new Date();
        attendance.email = customer.login;
        attendance.isForUser = false;
        attendance.result = Attendance.ATTENDANCE_RESULT_ACCEPTED;
        attendance.save();
        return attendance;
    }

}