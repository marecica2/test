package controllers;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import models.Account;
import models.AccountPlan;
import models.Attendance;
import models.Event;
import models.Message;
import models.User;

import org.apache.http.client.utils.URIBuilder;

import payments.Paypal;
import payments.Paypal.AccessToken;
import play.Logger;
import play.i18n.Messages;
import play.libs.Crypto;
import play.mvc.With;
import utils.NumberUtils;
import utils.RandomUtil;
import utils.StringUtils;
import email.EmailNotificationBuilder;

@With(controllers.Secure.class)
public class PaymentController extends BaseController
{
    public static void payment(String event, String url)
    {
        final User user = getLoggedUser();
        final Event e = Event.get(event);

        if (BaseController.flashErrorGet() != null)
            flash.error(BaseController.flashErrorGet());

        flash.keep();
        render(user, e, url);
    }

    public static void paymentPost(String event, String url, String payment, String transactionId) throws Exception
    {
        final User user = getLoggedUser();
        final Event e = Event.get(event);
        Attendance attendance = e.getInviteForCustomer(user);

        if (url == null || StringUtils.getStringOrNull(url) == null)
        {
            flash.put("warning", Messages.get("invalid-parameters-try-again"));
            Events.event(null, null, event, null, null, null);
        }

        // in case of public event and missing attendance create it for the logged customer on the fly
        if (attendance == null)
            attendance = createAttendanceForCustomerEvent(e, user);

        //TODO add here other payment methods
        if (payment.equals(Attendance.ATTENDANCE_PAYMENT_PAYPAL))
            processWithPaypalAdaptive(attendance, e, user, url, transactionId);
    }

    public static void processWithPaypalAdaptiveResponse(String event, String url, String transactionId) throws Exception
    {
        final User user = getLoggedUser();
        final Event e = Event.get(event);
        Attendance attendance = e.getInviteForCustomer(user);
        processWithPaypalAdaptive(attendance, e, user, url, transactionId);
    }

    private static void processWithPaypalAdaptive(Attendance attendance, Event e, User user, String url, String transactionId) throws Exception
    {
        final String baseUrl = getBaseUrl();
        final String paypalCancelUrl = baseUrl + "event/" + e.uuid;
        final String paypalRedirectUrl = new URIBuilder(baseUrl + "/paypal/adaptive/" + e.uuid)
                .addParameter("transactionId", Crypto.encryptAES(user.uuid + e.roomSecret))
                .addParameter("url", url)
                .build()
                .toString();

        final AccountPlan currentPlan = e.user.account.currentPlan();
        Boolean dual = true;
        if (currentPlan != null && !currentPlan.type.equals(Account.TYPE_STANDARD) && currentPlan.profile != null)
            dual = false;

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

        // payment transaction completed
        if (transactionId != null && Crypto.decryptAES(transactionId).equals(user.uuid + e.roomSecret))
        {
            Map<String, String> resp = pp.getAdaptiveCheckoutDetails(e, dual, attendance.paypalAdaptivePayKey);
            final String respCode = resp.get("responseEnvelope.ack");
            if (respCode != null && respCode.equals("Success"))
            {
                // update attendance
                attendance.paid = true;
                attendance.paypalTransactionDate = new Date();
                attendance.paymentMethod = Attendance.ATTENDANCE_PAYMENT_PAYPAL;
                attendance.currency = resp.get("currencyCode");
                attendance.price = e.getTotalPrice();
                attendance.providerPrice = NumberUtils.parseDecimal(resp.get("paymentInfoList.paymentInfo(0).receiver.amount"));
                attendance.fee = NumberUtils.parseDecimal(resp.get("paymentInfoList.paymentInfo(1).receiver.amount"));
                attendance.paypalTransactionId = resp.get("paymentInfoList.paymentInfo(0).transactionId");
                attendance.paypalTransactionIdProvider = resp.get("paymentInfoList.paymentInfo(1).transactionId");
                attendance.save();

                // notification
                final String subject = Messages.getMessage(e.user.locale, "event-payment-completed-subject", e.listing.title);
                final String body = Messages.getMessage(e.user.locale, "event-payment-completed-message", e.listing.title, user.getFullName(), e.listing.title, getBaseUrl() + "event/"
                        + e.uuid);
                if (e.customer != null)
                    Message.createAdminNotification(e.user, subject, body);

                if (e.customer != null && e.customer.emailNotification)
                {
                    EmailNotificationBuilder eb = new EmailNotificationBuilder();
                    eb.setTo(e.user);
                    eb.setFrom(user)
                            .setSubject(subject)
                            .setMessageWiki(body)
                            .send();
                }

                redirect(url);
            }

            if (attendance.paypalTransactionId == null)
            {
                flash.error(Messages.get("paypal-error"));
                Events.event(null, null, e.uuid, null, null, null);
            }

        } else
        {
            Map<String, String> resp = pp.setAdaptiveCheckout(e, dual);
            final String respCode = resp.get("responseEnvelope.ack");

            if (respCode != null && respCode.equals("Success"))
            {
                String payKeyResp = resp.get("payKey");
                attendance.paypalAdaptivePayKey = payKeyResp;
                attendance.save();
                redirect(BaseController.getProperty(BaseController.CONFIG_PAYPAL_ADAPTIVE_URL) + payKeyResp);
            } else
            {
                final String errorCode = resp.get("error(0).message");
                flash.error(Messages.get("paypal-error", errorCode));
                Events.event(null, null, e.uuid, null, null, null);
            }
        }
    }

    public static void paypalRefundRequest(String id, String url, String reason)
    {
        checkAuthenticity();
        User user = getLoggedUser();
        Attendance attendance = Attendance.get(id);
        validation.required(reason);
        validation.required(attendance);

        if (user == null)
            forbidden();
        if (attendance == null)
            notFound();
        if (!attendance.customer.equals(user))
            forbidden();

        if (!validation.hasErrors())
        {
            if (user.equals(attendance.customer))
            {
                attendance.refundReason = reason;
                attendance.refundRequested = true;
                attendance.save();

                // notification
                final String subject = Messages.getMessage(attendance.event.user.locale, "event-refund-request-subject", user.getFullName());
                final String body = Messages.getMessage(attendance.event.user.locale, "event-refund-request-message",
                        user.getFullName(),
                        attendance.event.listing.title,
                        getBaseUrl() + "event/" + attendance.event.uuid,
                        getBaseUrl() + "payments",
                        attendance.refundReason
                        );

                if (attendance.event.user != null)
                    Message.createAdminNotification(attendance.event.user, subject, body);

                if (attendance.event.user != null && attendance.event.user.emailNotification)
                {
                    EmailNotificationBuilder eb = new EmailNotificationBuilder();
                    eb.setTo(attendance.event.user);
                    eb.setFrom(user)
                            .setSubject(subject)
                            .setMessageWiki(body)
                            .send();
                }
            }
        } else
        {
            flash.error(Messages.get("reason-required"));
            Events.event(null, null, attendance.event.uuid, null, null, null);
        }
        redirectTo(url);
    }

    public static void paypalRefund(String id, String url)
    {
        checkAuthenticity();
        User user = getLoggedUser();
        Attendance attendance = Attendance.get(id);
        if (user == null)
            forbidden();

        System.err.println(attendance.event.user.login + " " + user.login);
        if (!attendance.event.user.equals(user) && !user.isAdmin())
            forbidden();

        final Paypal pp = new Paypal(attendance.event, null, null,
                getProperty(CONFIG_PAYPAL_PROVIDER_ACCOUNT),
                getProperty(CONFIG_PAYPAL_PROVIDER_ACCOUNT_MICROPAYMENT),
                getProperty(CONFIG_PAYPAL_USER),
                getProperty(CONFIG_PAYPAL_PWD),
                getProperty(CONFIG_PAYPAL_SIGNATURE),
                getProperty(CONFIG_PAYPAL_ENDPOINT),
                getProperty(CONFIG_PAYPAL_URL),
                getProperty(CONFIG_PAYPAL_PERCENTAGE)
                );

        try
        {
            Map<String, String> resp = pp.doAdaptiveCheckoutRefund(attendance.paypalTransactionId);
            if (resp.get("responseEnvelope.ack").equals("Success"))
            {
                attendance.refunded = true;
                attendance.save();

                // notification
                final String subject = Messages.getMessage(attendance.event.user.locale, "event-refunded-request-subject", attendance.event.listing.title);
                final String body = Messages.getMessage(attendance.event.user.locale, "event-refunded-request-message",
                        attendance.event.listing.title,
                        attendance.event.listing.title,
                        getBaseUrl() + "event/" + attendance.event.uuid
                        );

                if (attendance.customer != null)
                    Message.createAdminNotification(attendance.customer, subject, body);

                if (attendance.customer != null && attendance.customer.emailNotification)
                {
                    EmailNotificationBuilder eb = new EmailNotificationBuilder();
                    eb.setTo(attendance.customer);
                    eb.setFrom(user)
                            .setSubject(subject)
                            .setMessageWiki(body)
                            .send();
                }
            } else
            {
                flashErrorPut(Messages.get("error-default-message"));
                Logger.error("PaypalController.paypalRefund failed: \n" + resp.toString());
            }

        } catch (Exception e)
        {
            Logger.error(e, "paypalRefund ERROR");
            e.printStackTrace();
        }
        redirectTo(url);
    }

    public static void subscription(String id)
    {
        checkAuthenticity();
        final User user = getLoggedUserNotCache();
        final String redirectUrl = getBaseUrl() + "settings/subscription/response?id=" + id;
        final String cancelUrl = getBaseUrl() + "settings";
        final AccountPlan plan = AccountPlan.getById(user.account, Long.parseLong(id));
        final Paypal pp = new Paypal(redirectUrl, cancelUrl,
                getProperty(CONFIG_PAYPAL_USER),
                getProperty(CONFIG_PAYPAL_PWD),
                getProperty(CONFIG_PAYPAL_SIGNATURE),
                plan.price,
                Account.PRICE_PLAN_CURRENCY);
        try
        {
            AccessToken token = pp.setRecurring(user, plan);
            redirect(pp.getPaymentUrl(token.getToken()));
        } catch (Exception e)
        {
            e.printStackTrace();
            flash.error(e.getMessage());
            Accounts.account();
        }

    }

    public static void subscriptionResponse(String token, String id)
    {
        final User user = getLoggedUserNotCache();
        final AccountPlan plan = AccountPlan.getById(user.account, Long.parseLong(id));
        final String redirectUrl = getBaseUrl() + "settings";
        final String cancelUrl = getBaseUrl() + "settings";
        final Paypal pp = new Paypal(redirectUrl, cancelUrl,
                getProperty(CONFIG_PAYPAL_USER),
                getProperty(CONFIG_PAYPAL_PWD),
                getProperty(CONFIG_PAYPAL_SIGNATURE),
                plan.price,
                Account.PRICE_PLAN_CURRENCY);
        try
        {
            // set up recurring payment
            final String profile = pp.doRecurring(user, token, plan);

            // update date from validity from paypal
            final Map<String, String> respMap = pp.getRecurringPayments(profile);
            final String startDate = respMap.get("PROFILESTARTDATE").substring(0, respMap.get("PROFILESTARTDATE").length() - 1);
            final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
            Date from = sdf.parse(startDate);

            plan.validFrom = from;
            plan.profile = profile;
            plan.paid = true;
            plan.save();

            // update previous plan
            AccountPlan previous = plan.getPrevious();
            if (previous != null)
            {
                previous.validTo = from;
                previous.save();
            }

            flash.success("Subscription set up successfully");
            flash.keep();
            Accounts.account();
        } catch (Exception e)
        {
            e.printStackTrace();
            flash.error("Something went wrong, try again. " + e.getMessage());
            Accounts.account();
        }
    }

    public static void paypalCancelRecurring() throws Exception
    {
        checkAuthenticity();
        final User user = getLoggedUserNotCache();
        final String cancelUrl = null;
        final String redirectUrl = null;
        final Paypal pp = new Paypal(redirectUrl, cancelUrl,
                getProperty(CONFIG_PAYPAL_USER),
                getProperty(CONFIG_PAYPAL_PWD),
                getProperty(CONFIG_PAYPAL_SIGNATURE),
                user.account.getPlanPrice(),
                Account.PRICE_PLAN_CURRENCY);

        // cancel current plan but keep valid until last payment
        final AccountPlan current = AccountPlan.getCurrentPlan(user.account);
        if (current != null && current.profile != null)
        {
            Date validTo = current.validTo;
            if (current.canceled == null)
            {
                validTo = getValidUntil(current);
                pp.cancelRecurringPayments(user, current);
                current.validTo = validTo;
                current.canceled = true;
                current.save();
            }

            // if there is already paid upgrade plan, discard it
            AccountPlan last = AccountPlan.getLast(user.account);
            if (!last.id.equals(current.id) && last.profile != null)
            {
                pp.cancelRecurringPayments(user, last);
                last.delete();
            }
            // create standard plan for user
            final AccountPlan standard = new AccountPlan();
            standard.account = user.account;
            standard.type = Account.PLAN_STANDARD;
            standard.price = new BigDecimal(0);
            standard.validFrom = validTo;
            standard.save();
        } else
        {
            final AccountPlan standard = new AccountPlan();
            standard.account = user.account;
            standard.type = Account.PLAN_STANDARD;
            standard.price = new BigDecimal(0);
            standard.validFrom = new Date();
            standard.save();
        }

        redirectTo("/settings");
    }

    public static void paypalUpgradeRecurring(String type)
    {
        checkAuthenticity();
        final User user = getLoggedUserNotCache();
        final AccountPlan currentPlan = user.account.currentPlan();
        final String cancelUrl = null;
        final String redirectUrl = null;
        final Paypal pp = new Paypal(redirectUrl, cancelUrl,
                getProperty(CONFIG_PAYPAL_USER),
                getProperty(CONFIG_PAYPAL_PWD),
                getProperty(CONFIG_PAYPAL_SIGNATURE),
                user.account.getPlanPrice(),
                Account.PRICE_PLAN_CURRENCY);

        // get next payment of current plan
        Date validFrom = new Date();
        if (currentPlan != null && currentPlan.profile != null)
        {
            validFrom = getValidUntil(currentPlan);

            // cancel current recurring payment
            pp.cancelRecurringPayments(user, currentPlan);
        }

        upgradeCurrentPlan(currentPlan, type, user.account, validFrom);
        redirectTo("/settings");
    }

    private static AccountPlan upgradeCurrentPlan(AccountPlan current, String type, Account account, Date from)
    {
        if (current != null)
        {
            current.validTo = from;
            current.canceled = true;
            current.save();
        }

        AccountPlan newPlan = new AccountPlan();
        newPlan.type = type;
        newPlan.price = new BigDecimal("0");
        if (type.equals(Account.PLAN_MONTH_PREMIUM))
            newPlan.price = Account.PRICE_PLAN_PREMIUM;
        if (type.equals(Account.PLAN_MONTH_PRO))
            newPlan.price = Account.PRICE_PLAN_PRO;
        newPlan.validFrom = from;
        newPlan.account = account;
        newPlan.save();
        return newPlan;
    }

    public static void paypalPayments(String id) throws Exception
    {
        User user = getLoggedUser();
        if (!user.isAdmin())
            forbidden();
        User usr = User.getUserByUUID(id);
        Map<String, String> respMap = getPayPalRecurringDetail(usr.account);
        render(respMap, user);
    }

    public static Map<String, String> getPayPalRecurringDetail(Account account)
    {
        String cancelUrl = null;
        String redirectUrl = null;
        final Paypal pp = new Paypal(redirectUrl, cancelUrl,
                getProperty(CONFIG_PAYPAL_USER),
                getProperty(CONFIG_PAYPAL_PWD),
                getProperty(CONFIG_PAYPAL_SIGNATURE),
                account.getPlanPrice(),
                Account.PRICE_PLAN_CURRENCY);
        AccountPlan plan = AccountPlan.getCurrentPlan(account);
        Map<String, String> respMap = pp.getRecurringPayments(plan.profile);
        return respMap;
    }

    private static Date getValidUntil(final AccountPlan currentPlan)
    {
        Date valid = new Date();
        final String cancelUrl = null;
        final String redirectUrl = null;
        final Paypal pp = new Paypal(redirectUrl, cancelUrl,
                getProperty(CONFIG_PAYPAL_USER),
                getProperty(CONFIG_PAYPAL_PWD),
                getProperty(CONFIG_PAYPAL_SIGNATURE),
                currentPlan.account.getPlanPrice(),
                Account.PRICE_PLAN_CURRENCY);
        final Map<String, String> respMap = pp.getRecurringPayments(currentPlan.profile);
        final String date = respMap.get("NEXTBILLINGDATE").substring(0, respMap.get("NEXTBILLINGDATE").length() - 1);
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
        try
        {
            valid = sdf.parse(date);
        } catch (ParseException e)
        {
            e.printStackTrace();
        }
        return valid;
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