package controllers;

import java.util.Date;

import models.User;
import play.data.validation.Required;
import play.i18n.Messages;
import play.libs.Crypto;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Http;
import play.utils.Java;

public class Secure extends Controller
{

    @Before(unless = { "login", "authenticate", "logout" })
    static void checkAccess() throws Throwable
    {
        // Authent
        if (!session.contains("username"))
        {
            flash.put("url", "GET".equals(request.method) ? request.url : "/"); // seems a good default
            login();
        }

        // Checks
        Check check = getActionAnnotation(Check.class);
        if (check != null)
        {
            check(check);
        }
        check = getControllerInheritedAnnotation(Check.class);
        if (check != null)
        {
            check(check);
        }
    }

    private static void check(Check check) throws Throwable
    {
        for (String profile : check.value())
        {
            boolean hasProfile = (Boolean) Security.invoke("check", profile);
            if (!hasProfile)
            {
                Security.invoke("onCheckFailed", profile);
            }
        }
    }

    // ~~~ Login

    public static void login()
    {
        Http.Cookie remember = request.cookies.get("rememberme");
        if (remember != null && remember.value.indexOf("-") > 0)
        {
            String sign = remember.value.substring(0, remember.value.indexOf("-"));
            String username = remember.value.substring(remember.value.indexOf("-") + 1);
            if (Crypto.sign(username).equals(sign))
            {
                session.put("username", username);
                redirectToOriginalURL();
            }
        }
        flash.keep("url");
        render();
    }

    public static void authenticate(@Required String username, String password, boolean remember) throws Throwable
    {
        // Check tokens
        Boolean allowed = false;
        allowed = (Boolean) Security.invoke("authenticate", username, password);
        if (validation.hasErrors() || !allowed)
        {
            flash.keep("url");
            flash.error(Messages.get("Incorrect login or password"));
            params.flash();
            login();
        }
        // Mark user as connected
        session.put("username", username);
        // Remember if needed
        if (remember)
        {
            response.setCookie("rememberme", Crypto.sign(username) + "-" + username, "30d");
        }
        // Redirect to the original URL (or /)
        redirectToOriginalURL();
    }

    public static void logout() throws Throwable
    {
        Security.invoke("onDisconnect");
        session.clear();
        //response.removeCookie("rememberme");
        Security.invoke("onDisconnected");
        flash.success(Messages.get("You have been logged out"));
        String url = request.params.get("url");
        if (url != null)
            redirect(url);
        login();
    }

    // ~~~ Utils

    static void redirectToOriginalURL()
    {
        Security.invoke("onAuthenticated");
        String url = flash.get("url");
        if (url == null)
        {
            url = request.params.get("url");
            if ("/login".equals(url))
                url = "/home";
            if ("/login/".equals(url))
                url = "/home";
            if ("".equals(url))
                url = "/home";
            if (url == null)
                url = "/home";
        }
        redirect(url);
    }

    public static class Security extends Controller
    {

        /**
         * @Deprecated
         * 
         * @param username
         * @param password
         * @return
         */
        static boolean authentify(String username, String password)
        {
            throw new UnsupportedOperationException();
        }

        /**
         * This method is called during the authentication process. This is where you check if
         * the user is allowed to log in into the system. This is the actual authentication process
         * against a third party system (most of the time a DB).
         *
         * @param username
         * @param password
         * @return true if the authentication process succeeded
         */
        static boolean authenticate(String username, String password)
        {
            User user = User.getUserByLogin(username);

            if (user != null && !user.activated)
            {
                flash("securityError", Messages.get("Your account is not activated"));
                return false;
            }

            if (user != null && user.password.equals(password))
            {
                user.lastLoginTime = new Date();
                user.lastOnlineTime = new Date();
                user.save();
                return true;
            }

            flash("securityError", Messages.get("Incorrect login or password"));
            return false;
        }

        /**
         * This method checks that a profile is allowed to view this page/method. This method is called prior
         * to the method's controller annotated with the @Check method. 
         *
         * @param profile
         * @return true if you are allowed to execute this controller method.
         */
        static boolean check(String profile)
        {
            return true;
        }

        /**
         * This method returns the current connected username
         * @return
         */
        static String connected()
        {
            return session.get("username");
        }

        /**
         * Indicate if a user is currently connected
         * @return  true if the user is connected
         */
        static boolean isConnected()
        {
            return session.contains("username");
        }

        /**
         * This method is called after a successful authentication.
         * You need to override this method if you with to perform specific actions (eg. Record the time the user signed in)
         */
        static void onAuthenticated()
        {
        }

        /**
        * This method is called before a user tries to sign off.
        * You need to override this method if you wish to perform specific actions (eg. Record the name of the user who signed off)
        */
        static void onDisconnect()
        {
        }

        /**
        * This method is called after a successful sign off.
        * You need to override this method if you wish to perform specific actions (eg. Record the time the user signed off)
        */
        static void onDisconnected()
        {
        }

        /**
         * This method is called if a check does not succeed. By default it shows the not allowed page (the controller forbidden method).
         * @param profile
         */
        static void onCheckFailed(String profile)
        {
            forbidden();
        }

        private static Object invoke(String m, Object... args)
        {
            try
            {
                return Java.invokeChildOrStatic(Security.class, m, args);
            } catch (Exception e)
            {
                e.printStackTrace();
            }
            return null;
        }

    }

}
