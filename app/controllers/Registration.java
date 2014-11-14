package controllers;

import models.Account;
import models.Attendance;
import models.User;
import play.cache.Cache;
import play.i18n.Messages;
import play.libs.Images;
import utils.RandomUtil;
import utils.StringUtils;
import email.EmailProvider;

public class Registration extends BaseController
{
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
            System.err.println(user);
            if (user != null)
            {
                user.password = StringUtils.getRandomPassword(8);
                user.save();
                EmailProvider em = new EmailProvider(user.account.smtpHost, user.account.smtpPort, user.account.smtpAccount, user.account.smtpPassword, "10000", user.account.smtpProtocol,
                        true);
                em.sendMessage(user.login, Messages.get("email.test.subject"), Messages.get("email.test.body") + user.password);
            }
            // send email
            redirect("/");
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
        String registration,
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
            account.key = RandomUtil.getDoubleUUID();
            account.smtpAccount = "DEFAULT";
            account.name = firstName + " " + lastName;
            account.save();

            User user = new User();
            user.timezone = offset;
            user.workingHourStart = "8";
            user.workingHourEnd = "16";
            user.agendaType = "agendaWeek";
            user.role = User.ROLE_ADMIN;
            user.uuid = RandomUtil.getDoubleUUID();
            user.login = login;
            user.emailNotification = true;
            user.password = password;
            user.firstName = StringUtils.htmlEscape(firstName);
            user.lastName = StringUtils.htmlEscape(lastName);
            user.save(account);

            // if user was logged using email link, update his attendance
            if (registration != null)
            {
                Attendance a = Attendance.get(registration);
                if (a != null)
                {
                    user = User.getUserByUUID(user.uuid);
                    a.email = user.login;
                    a.customer = user;
                    a.save();
                    System.err.println("Attedance updated");
                }
            }
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
        final Account account = user.account;
        boolean isPublic = false;
        render(isPublic, account, user);
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