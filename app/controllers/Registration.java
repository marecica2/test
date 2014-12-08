package controllers;

import java.util.Date;

import models.Account;
import models.Attendance;
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
        String expiration = "10mn";
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
                final String message = "<p>Your Widgr password has been reseted.<br/></p>New password: <strong>" + user.password
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

        if (!validation.hasErrors())
        {
            Account account = new Account();
            account.key = RandomUtil.getUUID();
            account.smtpHost = "DEFAULT";
            account.name = firstName + " " + lastName;
            account.type = Account.TYPE_STANDARD;
            account.planCurrent = Account.TYPE_STANDARD;
            account.planRequest = Account.PLAN_TYPE_STANDARD;
            account.planRequestFrom = new Date();
            account.save();

            User user = new User();
            user.timezone = offset;
            user.workingHourStart = "8";
            user.workingHourEnd = "16";
            user.agendaType = "agendaWeek";
            user.role = User.ROLE_ADMIN;
            user.uuid = RandomUtil.getUUID();
            user.login = login;
            user.emailNotification = true;
            user.activated = false;
            user.password = password;
            user.firstName = StringUtils.htmlEscape(firstName);
            user.lastName = StringUtils.htmlEscape(lastName);
            user.stylesheet = "purple";
            user.pattern = "pattern-4";
            user.layout = "wide";
            user.footer = "dark";
            user.registrationToken = token;
            user.referrerToken = RandomUtil.getUUID();
            user.save(account);

            // if user was logged using email link, update his attendance
            if (invitation != null)
            {
                Attendance a = Attendance.get(invitation);
                if (a != null)
                {
                    user = User.getUserByUUID(user.uuid);
                    a.email = user.login;
                    a.customer = user;
                    a.save();
                }
            }

            final String from = user.login;
            final String baseUrl = getProperty(BaseController.CONFIG_BASE_URL);
            final EmailProvider emailProvider = new EmailProvider();
            final String title = "Widgr: Account activation";
            final String message = "<p>To activate your Widgr account click <a href='" + baseUrl + "account/activate?uuid=" + user.uuid
                    + "'>here</a> or paste this url to the browser address bar <p>"
                    + baseUrl + "account/activate?uuid=" + user.uuid + "</p>";

            final VelocityContext ctx = VelocityTemplate.createBasicTemplate(null, baseUrl, title, message);
            final String body = VelocityTemplate.processTemplate(ctx, VelocityTemplate.getTemplateContent(VelocityTemplate.CONTACT_INVITE_TEMPLATE));
            new Notification(emailProvider, from, title, user.login, body).execute();

            flash.success("Check out Your email for activating this account");
            flash.keep();
            redirect("/login");
        } else
        {
            System.err.println("Invalid captcha");
            params.flash();
            render("Registration/registration.html");
        }
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
}