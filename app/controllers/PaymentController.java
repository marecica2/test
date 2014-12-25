package controllers;

import java.util.Date;
import java.util.List;

import models.Account;
import models.Attendance;
import models.Event;
import models.User;

import org.apache.http.client.utils.URIBuilder;

import payments.Paypal;
import payments.Paypal.AccessToken;
import payments.Paypal.DoExpressCheckoutResponse;
import play.Logger;
import play.libs.Crypto;
import utils.RandomUtil;

public class PaymentController extends BaseController
{
    public static void payments(String id)
    {
        final Boolean isPublic = true;
        final User user = User.getUserByUUID(id);
        final Account account = user != null ? user.account : null;
        final User customer = getLoggedUser();
        final User cust = customer;
        final List<Attendance> attendances = Attendance.getByCustomer(customer);
        final Boolean showAttendances = true;
        render("Public/customerProfile.html", customer, cust, account, user, attendances, isPublic, id, showAttendances);
    }

    public static void payment(String event, String url)
    {
        final User user = getLoggedUser();
        final Event e = Event.get(event);
        Attendance attendance = e.getInviteForCustomer(user);
        // in case of public event and missing attendance create it for the logged customer on the fly
        if (attendance == null)
            attendance = createAttendanceForCustomerEvent(e, user);

        if (BaseController.flashErrorGet() != null)
            flash.error(BaseController.flashErrorGet());

        flash.keep();
        render(user, e, url);
    }

    public static void paymentPost(String event, String url, String payment) throws Exception
    {
        if (payment.equals(Attendance.ATTENDANCE_PAYMENT_PAYPAL))
            processWithPaypal(event, url, null);
    }

    public static void processWithPaypal(String event, String url, String transactionId) throws Exception
    {
        final User user = getLoggedUser();
        final Event e = Event.get(event);
        final Attendance attendance = e.getInviteForCustomer(user);
        final String payerId = request.params.get("PayerID");
        final String baseUrl = getProperty(BaseController.CONFIG_BASE_URL);
        final String paypalCancelUrl = baseUrl + url.substring(1);
        final String paypalRedirectUrl = new URIBuilder(baseUrl + "/paypal/" + e.uuid)
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

        // handle response from paypal
        if (transactionId != null && Crypto.decryptAES(transactionId).equals(user.uuid) && payerId != null)
        {
            attendance.paypalPayerId = payerId;
            final DoExpressCheckoutResponse resp = pp.doExpressCheckoutDual(attendance.paypalAccessToken, attendance.paypalPayerId, e, false);

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

                System.err.println("xxxx");
                System.err.println("xxxx");
                System.err.println("redirecting");
                System.err.println(request.params.get("url"));
                BaseController.flashErrorPut(resp.errorMessage);
                redirectTo(request.params.get("url"));
            }
        }

        // redirect to paypal
        if (attendance.paid == null || !attendance.paid)
        {
            AccessToken token = pp.setExpressCheckoutDual(e, false);
            attendance.paypalAccessToken = token.getToken();
            attendance.paypalAccessTokenValidity = token.getValidity();
            attendance.paymentMethod = "paypal";
            attendance.save();
            redirect(pp.getPaymentUrl(token.getToken()));
        }

    }

    private static Attendance createAttendanceForCustomerEvent(Event e, User customer)
    {
        Attendance attendance;
        attendance = new Attendance();
        attendance.event = e;
        attendance.uuid = RandomUtil.getUUID();
        attendance.customer = customer;
        attendance.created = new Date();
        attendance.name = customer.getFullName();
        attendance.email = customer.login;
        attendance.isForUser = false;
        attendance.result = Attendance.ATTENDANCE_RESULT_ACCEPTED;
        attendance.save();
        return attendance;
    }
}