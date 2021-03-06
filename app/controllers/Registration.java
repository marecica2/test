package controllers;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import models.Account;
import models.AccountPlan;
import models.Attendance;
import models.Contact;
import models.Message;
import models.User;
import play.cache.Cache;
import play.i18n.Lang;
import play.i18n.Messages;
import play.libs.Crypto;
import play.libs.Images;
import play.mvc.Before;
import utils.RandomUtil;
import utils.StringUtils;
import email.EmailNotificationBuilder;

public class Registration extends BaseController
{
    @Before(only = { "password", "passwordPost" })
    static void checkAccess()
    {
        checkAuthorizedAccess();
    }

    public static void captcha(String uuid, String exp)
    {
        Images.Captcha captcha = Images.captcha();
        String code = captcha.getText(5);
        String expiration = "3mn";
        if (exp != null)
            expiration = exp + "s";
        Cache.set("captcha." + uuid, code, expiration);
        renderBinary(captcha);
    }

    public static void passwordReset()
    {
        boolean isPublic = false;
        String uuid = RandomUtil.getUUID();
        params.put("uuid", uuid);
        params.flash();
        render(isPublic, uuid);
    }

    public static void passwordResetPost(String login, String captcha, String uuid) throws Exception
    {
        checkAuthenticity();
        validation.required(login);
        validation.required(captcha);
        final Object cap = Cache.get("captcha." + uuid);
        if (captcha != null && cap != null)
        {
            validation.equals(captcha, cap).message("invalid-captcha");
        }

        User user = User.getUserByLogin(login);
        if (user == null || (user != null && user.isAdmin()))
            validation.addError("login", "user-not-exists");

        if (!validation.hasErrors())
        {
            if (user != null)
            {
                final String password = StringUtils.getRandomPassword(8);
                user.password = password;
                user.password = Crypto.encryptAES(password);
                user.save();

                final String baseUrl = getProperty(BaseController.CONFIG_BASE_URL);
                final String title = Messages.get("password-reset-subject");
                final String message = Messages.get("password-reset-message", password, baseUrl + "password");

                new EmailNotificationBuilder()
                        .setWidgrFrom()
                        .setTo(user)
                        .setSubject(title)
                        .setMessageWiki(message)
                        .send();
                // send email
                flash.success(Messages.get("password-message"));
                Secure.login();
            }
        } else
        {
            params.flash();
            render("Registration/passwordReset.html");
        }
    }

    public static void registration()
    {
        String uuid = RandomUtil.getUUID();
        String url = request.params.get("url");
        params.put("uuid", uuid);
        if (url != null)
            params.put("url", url);
        params.flash();
        render(uuid);
    }

    public static void registrationFacebook()
    {
        User user = new User();
        render(user);
    }

    public static void registrationPost(
        String login,
        String url,
        String account,
        String password,
        String passwordRepeat,
        String firstName,
        String lastName,
        String captcha,
        String uuid,
        String invitation,
        String accPlan,
        String type,
        String token,
        Integer offset)
    {
        checkAuthenticity();
        validation.required(login);
        validation.email(login).message("validation.login");
        validation.required(captcha);

        // password
        validation.required(password);
        validation.minSize(password, 8).message(Messages.get("password-error"));
        Pattern pattern = Pattern.compile(".*\\d.*");
        Matcher matcher = pattern.matcher(password);
        if (!matcher.matches())
            validation.addError("password", Messages.get("password-error"));
        pattern = Pattern.compile(".*[A-Z].*[A-Z].*");
        matcher = pattern.matcher(password);
        if (!matcher.matches())
            validation.addError("password", Messages.get("password-error"));
        validation.equals(passwordRepeat, password).message("validation.passwordMatch");

        // first last name
        validation.required(firstName);
        validation.required(lastName);
        pattern = Pattern.compile("\\w+", Pattern.UNICODE_CHARACTER_CLASS);
        matcher = pattern.matcher(firstName);
        if (!matcher.matches())
            validation.addError("firstName", Messages.get("bad-characters"));
        matcher = pattern.matcher(lastName);
        if (!matcher.matches())
            validation.addError("lastName", Messages.get("bad-characters"));

        final Object cap = Cache.get("captcha." + uuid);
        if (captcha != null && cap != null)
            validation.equals(captcha, cap).message("invalid.captcha");

        if (!accPlan.equals(Account.PLAN_STANDARD) && !accPlan.equals(Account.PLAN_MONTH_PREMIUM) && !accPlan.equals(Account.PLAN_MONTH_PRO))
            validation.addError("accPlan", Messages.get("invalid-value"));

        final User checkUser = User.getUserByLogin(login);
        if (checkUser != null)
            validation.addError("login", Messages.get("login-already-used", login));

        if (!validation.hasErrors())
        {
            User user = createDefaultUser(login, password, firstName, lastName, token, offset);
            Account acc = null;
            if (account != null)
                acc = Account.get(account);
            if (acc != null)
                user.role = User.ROLE_USER;
            if (acc == null)
                acc = createDefaultAccount(firstName, lastName, accPlan, type);
            user = user.save(acc);

            // if user used invitation link
            if (token != null)
                createReferrerContacts(token, user);

            // if user was logged using email link, update his attendance
            if (invitation != null)
                updateInvitation(invitation, user);

            // send activation email
            final String baseUrl = getProperty(BaseController.CONFIG_BASE_URL);
            final String title = Messages.get("account-activation-subject");
            final String message = Messages.get("account-activation-message", baseUrl, user.uuid);

            new EmailNotificationBuilder()
                    .setWidgrFrom()
                    .setTo(user)
                    .setSubject(title)
                    .setMessageWiki(message)
                    .send();

            flash.success(Messages.get("check-out-your-email-for-activating"));
            flash.keep();

            if (url != null)
                redirect("/login?url=" + url);
            redirect("/login");
        } else
        {
            params.flash();
            render("Registration/registration.html");
        }
    }

    public static void registrationFacebookPost(
        String login,
        String account,
        String facebook,
        String facebookName,
        String firstName,
        String lastName,
        String uuid,
        String type,
        String invitation,
        String token,
        Integer offset,
        Boolean iframe)
    {
        checkAuthenticity();
        validation.required(firstName);
        validation.required(lastName);
        validation.email(login).message("validation.login");
        validation.required(login);

        // if existing user and from iframe - autosign and refresh
        User checkUser = User.getUserByFacebook(facebook);
        if (checkUser != null)
            facebookAutoSignIn();

        checkUser = User.getUserByLogin(login);
        if (checkUser != null)
        {
            validation.addError("login", Messages.get("login-already-used", login));
            boolean fbError = true;
            System.err.println("login already used but not connected with facebook");
            render("Registration/registrationFacebookRefresh.html", fbError);
        }

        if (!validation.hasErrors())
        {
            final String password = RandomUtil.getRandomString(10);
            User user = createDefaultUser(login, password, firstName, lastName, token, offset);
            user.activated = true;
            user.facebookId = facebook;
            user.facebookName = facebookName;

            Account acc = null;
            if (account != null)
                acc = Account.get(account);
            if (acc != null)
                user.role = User.ROLE_USER;
            if (acc == null)
                acc = createDefaultAccount(firstName, lastName, Account.TYPE_STANDARD, type);
            user = user.save(acc);

            // if user used invitation link
            if (token != null)
                createReferrerContacts(token, user);

            // if user was logged using email link, update his attendance
            if (invitation != null)
                updateInvitation(invitation, user);

            final String baseUrl = getBaseUrl();
            final String subject = Messages.getMessage(user.locale, "account.activated-subject");
            final String body = Messages.getMessage(user.locale, "account.activated-message", user.login, baseUrl, user.login, baseUrl, baseUrl, baseUrl, baseUrl, baseUrl, baseUrl);

            Message.createAdminNotification(user, subject, body);
            new EmailNotificationBuilder()
                    .setWidgrFrom()
                    .setTo(user)
                    .setSubject(subject)
                    .setMessageWiki(body)
                    .send();

            session.put("username", user.login);

            // if existing user and from iframe - autosign and refresh
            if (iframe != null)
                facebookAutoSignIn();
            else
                redirectTo("/dashboard");
        }
        params.flash();
        validation.keep();
        flash.keep();
        params.flash();
        registration();
    }

    private static void facebookAutoSignIn()
    {
        try
        {
            boolean success = Secure.authenticateFacebookMethod(request.params.get("id"), request.params.get("signedRequest"));
            if (success)
            {
                render("Registration/registrationFacebookRefresh.html");
            } else
            {
                forbidden();
            }
        } catch (Exception e)
        {
            e.printStackTrace();
            error();
        }
    }

    public static void password()
    {
        final User user = getLoggedUser();
        render(user);
    }

    public static void passwordPost(String oldPassword, String password, String passwordRepeat)
    {
        final User user = getLoggedUserNotCache();

        checkAuthenticity();
        validation.required(oldPassword);
        validation.required(password);

        if (!Crypto.encryptAES(oldPassword).equals(user.password))
            validation.addError("oldPassword", "validation-invalidPassword");

        validation.required(password);
        validation.minSize(password, 8).message(Messages.get("password-error"));
        Pattern pattern = Pattern.compile(".*\\d.*");
        Matcher matcher = pattern.matcher(password);
        if (!matcher.matches())
            validation.addError("password", Messages.get("password-error"));
        pattern = Pattern.compile(".*[A-Z].*[A-Z].*");
        matcher = pattern.matcher(password);
        if (!matcher.matches())
            validation.addError("password", Messages.get("password-error"));
        validation.equals(passwordRepeat, password).message("validation.passwordMatch");

        if (!validation.hasErrors())
        {
            user.password = Crypto.encryptAES(password);
            user.save(user.account);
            redirect("/login");
        } else
        {
            params.flash();
            render("Registration/password.html", user);
        }
    }

    public static void activate(String uuid)
    {
        User user = User.getUserByUUID(uuid);
        user.activated = true;
        user.save();

        final String baseUrl = getBaseUrl();
        final String subject = Messages.getMessage(user.locale, "account.activated-subject");
        final String body = Messages.getMessage(user.locale, "account.activated-message", user.login, baseUrl, user.login, baseUrl, baseUrl, baseUrl, baseUrl, baseUrl, baseUrl);

        Message.createAdminNotification(user, subject, body);
        new EmailNotificationBuilder()
                .setWidgrFrom()
                .setTo(user)
                .setSubject(subject)
                .setMessageWiki(body)
                .send();

        flash.success(Messages.get("account-successfully-activated"));
        flash.keep();
        Secure.login();
    }

    private static void updateInvitation(String invitation, User user)
    {
        Attendance a = Attendance.get(invitation);
        if (a != null)
        {
            a.email = user.login;
            a.customer = user;
            a.save();
        }
    }

    private static void createReferrerContacts(String token, User user)
    {
        User referrerUser = User.getUserByToken(token);
        if (referrerUser != null)
        {

            Contact c = new Contact();
            c.user = user;
            c.contact = referrerUser;
            c.following = true;
            c.saveContact();

            Contact c1 = new Contact();
            c1.user = referrerUser;
            c1.contact = user;
            c1.following = true;
            c1.saveContact();
        }
    }

    private static User createDefaultUser(String login, String password, String firstName, String lastName, String token, Integer offset)
    {
        User user = new User();
        user.login = login;
        user.firstName = firstName;
        user.lastName = lastName;
        user.password = Crypto.encryptAES(password);
        user.registrationToken = token;
        user.timezone = offset;
        user.locale = Lang.get();
        user.reminder = true;
        user.reminderMinutes = 30;
        user.created = new Date();

        user.uuid = RandomUtil.getUUID();
        user.referrerToken = RandomUtil.getUUID();
        user.role = User.ROLE_ADMIN;
        user.activated = false;

        user.workingHourStart = "8";
        user.workingHourEnd = "16";
        user.emailNotification = true;
        user.reminder = false;
        return user;
    }

    private static Account createDefaultAccount(String firstName, String lastName, String planType, String type)
    {
        Account account = new Account();
        account.key = RandomUtil.getUUID();
        account.name = null;
        account.type = Account.TYPE_STANDARD;
        account.created = new Date();
        account.save();

        AccountPlan plan = new AccountPlan();
        plan.type = planType;
        plan.validFrom = new Date();
        plan.account = account;
        plan.save();
        return account;
    }

}