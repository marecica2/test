//package controllers;
//
//import java.util.Date;
//import java.util.List;
//
//import models.Account;
//import models.Attendance;
//import models.Event;
//import models.User;
//import payments.Paypal;
//import payments.Paypal.AccessToken;
//import payments.Paypal.DoExpressCheckoutResponse;
//import play.Logger;
//import play.i18n.Lang;
//import utils.NetUtils;
//
//public class Customers extends BaseController
//{
//    public static void about()
//    {
//        final Boolean isPublic = true;
//        final Boolean isCustomerLogged = isUserLogged();
//        final User customer = getLoggedUser();
//        render(customer, isCustomerLogged, isPublic);
//    }
//
//    public static void payments(String id)
//    {
//        final Boolean isPublic = true;
//        final User user = User.getUserByUUID(id);
//        final Account account = user != null ? user.account : null;
//        final User customer = getLoggedUser();
//        final User cust = customer;
//        final List<Attendance> attendances = Attendance.getByCustomer(customer);
//        final Boolean showAttendances = true;
//        render("Public/customerProfile.html", customer, cust, account, user, attendances, isPublic, id, showAttendances);
//    }
//
//    public static void account(String id)
//    {
//        final Boolean isPublic = true;
//        final boolean edit = request.params.get("edit") == null ? false : true;
//        final User user = User.getUserByUUID(id);
//        final User customer = getLoggedUser();
//        final Account account = user != null ? user.account : null;
//        final Boolean isCustomerLogged = isUserLogged();
//        final String baseUrl = request.getBase();
//        if (edit)
//        {
//            params.put("firstName", customer.firstName);
//            params.put("lastName", customer.lastName);
//            params.put("locale", customer.locale);
//            params.put("skype", customer.skype);
//            params.flash();
//        }
//        render(user, baseUrl, edit, customer, account, id, isCustomerLogged, isPublic);
//    }
//
//    public static void accountPost(String id, String firstName, String lastName, String locale, String skype, String avatarUrl, String avatarUrlSmall)
//    {
//        validation.required(firstName);
//        validation.required(lastName);
//
//        final Boolean isPublic = true;
//        final boolean edit = request.params.get("edit") == null ? false : true;
//        final User user = User.getUserByUUID(id);
//        final User customer = getLoggedUser();
//        final Account account = user != null ? user.account : null;
//        final Boolean isCustomerLogged = isUserLogged();
//        final String baseUrl = request.getBase();
//
//        if (!validation.hasErrors())
//        {
//            customer.locale = locale;
//            customer.firstName = firstName;
//            customer.lastName = lastName;
//            customer.skype = skype;
//            customer.avatarUrl = avatarUrl;
//            customer.save();
//            Lang.change(locale);
//            redirect("/public/account?id=" + id);
//        } else
//        {
//            params.flash();
//        }
//        render("Customers/account.html", user, baseUrl, account, edit, id, isCustomerLogged, isPublic);
//    }
//
//    public static void livestream(String event, String id, String secret) throws Throwable
//    {
//        final Event e = Event.get(event);
//        if (e == null)
//            notFound();
//
//        final User customer = getLoggedUser();
//        final String name = customer != null ? customer.getFullName() : session.get("customername");
//        final String room = event;
//        final Boolean anonymous = true;
//        final Account account = Account.getAccountByEvent(event);
//        final String rmtp = getProperty(CONFIG_RMTP_PATH);
//        final boolean isPublic = true;
//        final boolean isCustomerLogged = isUserLogged();
//
//        String serverIp = NetUtils.getIp();
//        if (isProd() || serverIp == null)
//            serverIp = getProperty("star.configuration.domain");
//
//        checkAccessRights(e, event, customer, id);
//        checkPayPalPayment(e, id, secret, "livestream");
//        render(name, serverIp, room, anonymous, rmtp, e, account, isPublic, isCustomerLogged, customer, id);
//    }
//
//    public static void room(String event, String id, String secret) throws Throwable
//    {
//        final Event e = Event.get(event);
//        if (e == null)
//            notFound();
//
//        final User customer = getLoggedUser();
//        final String name = customer != null ? customer.getFullName() : session.get("customername");
//        final String room = event;
//        final Account account = Account.getAccountByEvent(event);
//        final Boolean isPublic = true;
//        final Boolean isCustomerLogged = isUserLogged();
//        final String user = id;
//        String serverIp = NetUtils.getIp();
//        if (isProd() || serverIp == null)
//            serverIp = getProperty("star.configuration.domain");
//
//        checkAccessRights(e, event, customer, id);
//        checkPayPalPayment(e, id, secret, "room");
//        renderTemplate("Application/room.html", name, room, serverIp, isPublic, isCustomerLogged, user, account, e, customer, id);
//    }
//
//    public static void joinRoom(String retUrl, String id, String eventId)
//    {
//        final Event e = Event.get(eventId);
//        final String user = id;
//        final User usr = User.getUserByUUID(user);
//        final Account account = usr.account;
//        final Boolean isPublic = true;
//        final Boolean isCustomerLogged = getLoggedUser() == null ? false : true;
//        final User customer = getLoggedUser();
//
//        if (request.method.equals("GET"))
//        {
//            String returnUrl = retUrl;
//            render(returnUrl, account, e, isPublic, isCustomerLogged, customer, id, retUrl);
//        }
//        if (request.method.equals("POST"))
//        {
//            final String postedName = request.params.get("name");
//            System.err.println("posted name " + postedName);
//            session.put("customername", postedName);
//            redirect(request.params.get("url"));
//        }
//    }
//
//    private static void checkPayPalPayment(Event e, String id, String secret, String redirect) throws Throwable
//    {
//        if (e.listing.charging.equals(Event.EVENT_CHARGING_FREE))
//        {
//            Logger.info("Free event, not needed to log in");
//            return;
//        }
//
//        if (!e.listing.charging.equals(Event.EVENT_CHARGING_FREE) && (!isUserLogged() || getLoggedUser() == null))
//        {
//            Logger.info("Redirect to login");
//            flash.put("url", request.url);
//            Secure.login();
//        }
//
//        if (!e.listing.charging.equals(Event.EVENT_CHARGING_FREE) && isUserLogged() && getLoggedUser() != null)
//        {
//            final User customer = getLoggedUser();
//            Attendance attendance = e.getInviteForCustomer(customer);
//
//            // in case of public event and missing attendance create it for the logged customer programatically
//            if (attendance == null && e.listing.privacy.equals(Event.EVENT_VISIBILITY_PUBLIC))
//                attendance = createAttendanceForCustomerEvent(e, customer);
//
//            final String cancelUrl = getProperty(BaseController.CONFIG_BASE_URL) + "public/calendar?id=" + id;
//            final String returnUrl = getProperty(BaseController.CONFIG_BASE_URL) + "public/" + redirect + "?" + "event=" + e.uuid + "&id=" + id + "&secret=" + e.roomSecret;
//
//            final Paypal pp = new Paypal(
//                    e,
//                    returnUrl,
//                    cancelUrl,
//                    getProperty(CONFIG_PAYPAL_PROVIDER_ACCOUNT),
//                    getProperty(CONFIG_PAYPAL_PROVIDER_ACCOUNT_MICROPAYMENT),
//                    getProperty(CONFIG_PAYPAL_USER),
//                    getProperty(CONFIG_PAYPAL_PWD),
//                    getProperty(CONFIG_PAYPAL_SIGNATURE),
//                    getProperty(CONFIG_PAYPAL_ENDPOINT),
//                    getProperty(CONFIG_PAYPAL_URL),
//                    getProperty(CONFIG_PAYPAL_PERCENTAGE)
//                    );
//
//            // payment received
//            if (secret != null && e.roomSecret.equals(secret))
//            {
//                attendance.payerId = request.params.get("PayerID");
//                final DoExpressCheckoutResponse resp = pp.doExpressCheckoutDual(attendance.accessToken, attendance.payerId, e);
//
//                if (resp.success)
//                {
//                    attendance.paid = true;
//                    attendance.fee = resp.fee;
//                    attendance.transactionDate = new Date();
//                    attendance.price = resp.price;
//                    attendance.providerPrice = resp.providerPrice;
//                    attendance.currency = e.account.currency;
//                    attendance.paypalAccount = e.account.paypalAccount;
//                    attendance.paypalAccountProvider = resp.providerAccount;
//                    attendance.transactionId = resp.transactionIdProvider;
//                    attendance.transactionIdProvider = resp.transactionIdOur;
//                    attendance.save();
//                    String returnUrlParam = "event=" + e.uuid + "&id=" + id;
//                    redirect(getProperty(BaseController.CONFIG_BASE_URL) + "public/" + redirect + "?" + returnUrlParam);
//                } else
//                {
//                    Logger.error(resp.errorMessage);
//                    flash.put("paypalError", resp.errorMessage);
//                    redirect(getProperty(BaseController.CONFIG_BASE_URL) + "public/" + redirect + "?event=" + e.uuid + "&id=" + e.user.uuid);
//                }
//            }
//
//            // proceed to payment
//            if ((attendance.paid == null || attendance.paid != true))
//            {
//                if (request.method.equals("GET"))
//                {
//                    final Boolean isPublic = true;
//                    final Boolean isCustomerLogged = getLoggedUser() == null ? false : true;
//                    final Account account = e.account;
//                    renderTemplate("Customers/payment.html", e, isPublic, customer, isCustomerLogged, account);
//                }
//
//                if (request.method.equals("POST"))
//                {
//                    AccessToken token = pp.setExpressCheckoutDual(e);
//                    attendance.accessToken = token.getToken();
//                    attendance.accessTokenValidity = token.getValidity();
//                    attendance.save();
//                    redirect(pp.getPaymentUrl(token.getToken()));
//                }
//            } else
//            {
//                Logger.info("Payment is accepted, displaying room");
//                return;
//            }
//        }
//
//        response.status = 400;
//        renderText("Bad request");
//    }
//
//    private static void checkAccessRights(Event e, String event, User customer, String id) throws Throwable
//    {
//        // allow only public events for not registered customers
//        if (Event.EVENT_VISIBILITY_PUBLIC.equals(e.listing.privacy) && !isUserLogged())
//        {
//            joinRoom(request.url, id, event);
//
//        } else if (Event.EVENT_VISIBILITY_PUBLIC.equals(e.listing.privacy) && isUserLogged())
//        {
//            System.err.println("Access granted - public event " + e.listing.title);
//
//        } else if (!Event.EVENT_VISIBILITY_PUBLIC.equals(e.listing.privacy) && customer != null && e.hasInviteFor(customer))
//        {
//            System.err.println("Access for event " + e.listing.title + " granted to customer " + customer);
//
//        } else
//        {
//            flash.put("id", id);
//            flash.put("url", request.url);
//            Secure.login();
//        }
//    }
//
//    private static Attendance createAttendanceForCustomerEvent(Event e, User customer)
//    {
//        Attendance attendance;
//        attendance = new Attendance();
//        attendance.event = e;
//        attendance.account = Account.getAccountByEvent(e.uuid);
//        attendance.customer = customer;
//        attendance.created = new Date();
//        attendance.email = customer.login;
//        attendance.isForUser = false;
//        attendance.result = Attendance.ATTENDANCE_RESULT_ACCEPTED;
//        attendance.save();
//        return attendance;
//    }
//}