package controllers;

import java.util.Date;

import models.Account;
import models.Attendance;
import models.Contact;
import models.User;

import org.apache.velocity.VelocityContext;

import play.cache.Cache;
import play.libs.Images;
import play.mvc.Before;
import templates.VelocityTemplate;
import utils.RandomUtil;
import utils.StringUtils;
import email.EmailProvider;
import email.Notification;

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
        String expiration = "5mn";
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
        validation.required(login);
        validation.required(captcha);
        final Object cap = Cache.get("captcha." + uuid);
        if (captcha != null && cap != null)
            validation.equals(captcha, cap).message("invalid.captcha");

        if (!validation.hasErrors())
        {
            User user = User.getUserByLogin(login);
            if (user != null)
            {
                user.password = StringUtils.getRandomPassword(8);
                user.save();

                final String from = user.login;
                final String baseUrl = getProperty(BaseController.CONFIG_BASE_URL);
                final EmailProvider emailProvider = new EmailProvider();
                final String title = "Widgr: Password reset";
                final String message = "<p>Your password has been reseted.<br/></p>New password: <strong>" + user.password
                        + "</strong> <p>It is highly recommended to change this password <a href='"
                        + baseUrl + "password'>here</a></p>";

                final VelocityContext ctx = VelocityTemplate.createBasicTemplate(null, baseUrl, title, message);
                final String body = VelocityTemplate.processTemplate(ctx, VelocityTemplate.getTemplateContent(VelocityTemplate.CONTACT_INVITE_TEMPLATE));
                new Notification(emailProvider, from, title, user.login, body).execute();
            }
            // send email
            redirect("/login");
        } else
        {
            params.flash();
            render("Registration/passwordReset.html");
        }
    }

    public static void registration()
    {
        String uuid = RandomUtil.getUUID();
        params.put("uuid", uuid);
        params.flash();
        render(uuid);
    }

    public static void registrationPost(
        String login,
        String password,
        String passwordRepeat,
        String firstName,
        String lastName,
        String captcha,
        String uuid,
        String invitation,
        String token,
        Integer offset)
    {
        validation.required(firstName);
        validation.required(lastName);
        validation.email(login).message("validation.login");
        validation.required(login);
        validation.required(password);
        validation.equals(password, passwordRepeat).message("validation.passwordMatch");
        validation.required(captcha);

        final Object cap = Cache.get("captcha." + uuid);
        if (captcha != null && cap != null)
            validation.equals(captcha, cap).message("invalid.captcha");

        final User checkUser = User.getUserByLogin(login);
        if (checkUser != null)
            validation.addError("login", "Login already used");

        if (!validation.hasErrors())
        {
            Account account = createDefaultAccount(firstName, lastName);
            account.save();

            User user = createDefaultUser(login, password, firstName, lastName, token, offset);
            user = user.save(account);

            // if user used invitation link
            if (token != null)
                createReferrerContacts(token, user);

            // if user was logged using email link, update his attendance
            if (invitation != null)
                createInvitation(invitation, user);

            // send email
            activationEmail(user);

            flash.success("Check out Your email for activating this account");
            flash.keep();
            redirect("/login");
        } else
        {
            params.flash();
            render("Registration/registration.html");
        }
    }

    public static void registrationFacebookPost(
        String login,
        String facebook,
        String facebookName,
        String firstName,
        String lastName,
        String uuid,
        String invitation,
        String token,
        Integer offset)
    {
        validation.required(firstName);
        validation.required(lastName);
        validation.email(login).message("validation.login");
        validation.required(login);

        final User checkUser = User.getUserByLogin(login);
        if (checkUser != null)
            validation.addError("login", "Login already used");

        if (!validation.hasErrors())
        {
            final Account account = createDefaultAccount(firstName, lastName);
            account.save();

            final String password = RandomUtil.getRandomString(10);
            User user = createDefaultUser(login, password, firstName, lastName, token, offset);
            user.activated = true;
            user.facebookId = facebook;
            user.facebookName = facebookName;
            user = user.save(account);

            // if user used invitation link
            if (token != null)
                createReferrerContacts(token, user);

            // if user was logged using email link, update his attendance
            if (invitation != null)
                createInvitation(invitation, user);

            redirectTo("/login");
        }
        params.flash();
        validation.keep();
        flash.keep();
        params.flash();
        registration();
    }

    public static void password()
    {
        final User user = getLoggedUser();
        render(user);
    }

    public static void passwordPost(String oldPassword, String password, String passwordRepeat)
    {
        validation.required(oldPassword);
        validation.required(password);
        validation.equals(password, passwordRepeat).message("validation.passwordMatch");

        final User user = getLoggedUser();
        final Account account = user.account;

        if (!oldPassword.equals(user.password))
            validation.addError("oldPassword", "validation.invalidPassword");

        if (!validation.hasErrors())
        {
            user.password = password;
            user.save(user.account);
            redirect("/login");
        } else
        {
            params.flash();
            render("Registration/password.html", user, account);
        }
    }

    private static void createInvitation(String invitation, User user)
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

    private static void activationEmail(User user)
    {
        final String from = user.login;
        final String baseUrl = getProperty(BaseController.CONFIG_BASE_URL);
        final EmailProvider emailProvider = new EmailProvider();
        final String title = "Widgr: Account activation";
        final String message = "<p>To activate your account click <a href='" + baseUrl + "account/activate?uuid=" + user.uuid
                + "'>here</a> or paste this url to the browser address bar <p>"
                + baseUrl + "account/activate?uuid=" + user.uuid + "</p>";

        final VelocityContext ctx = VelocityTemplate.createBasicTemplate(null, baseUrl, title, message);
        final String body = VelocityTemplate.processTemplate(ctx, VelocityTemplate.getTemplateContent(VelocityTemplate.CONTACT_INVITE_TEMPLATE));
        new Notification(emailProvider, from, title, user.login, body).execute();
    }

    private static User createDefaultUser(String login, String password, String firstName, String lastName, String token, Integer offset)
    {
        User user = new User();
        user.login = login;
        user.firstName = firstName;
        user.lastName = lastName;
        user.password = password;
        user.registrationToken = token;
        user.timezone = offset;

        user.uuid = RandomUtil.getUUID();
        user.referrerToken = RandomUtil.getUUID();
        user.role = User.ROLE_ADMIN;
        user.activated = false;

        user.workingHourStart = "8";
        user.workingHourEnd = "16";
        user.emailNotification = true;
        user.stylesheet = "purple";
        user.pattern = "pattern-4";
        user.layout = "wide";
        user.footer = "dark";
        return user;
    }

    private static Account createDefaultAccount(String firstName, String lastName)
    {
        Account account = new Account();
        account.key = RandomUtil.getUUID();
        account.smtpHost = "DEFAULT";
        account.name = firstName + " " + lastName;
        account.type = Account.TYPE_STANDARD;
        account.planCurrent = Account.TYPE_STANDARD;
        account.planRequest = Account.PLAN_TYPE_STANDARD;
        account.planRequestFrom = new Date();
        return account;
    }

    public static void passwordCustomer()
    {

    }
}