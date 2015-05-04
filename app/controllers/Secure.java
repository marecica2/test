package controllers;

import java.util.Date;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import models.User;

import org.apache.commons.codec.binary.Base64;

import play.Logger;
import play.data.validation.Required;
import play.i18n.Messages;
import play.libs.Crypto;
import play.mvc.Before;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Http.Cookie;
import play.utils.Java;
import utils.NumberUtils;

public class Secure extends BaseController
{

    @Before(unless = { "login", "authenticate", "logout", "authenticateFacebook" })
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

        User user = getLoggedUser();
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

        if (flash.get("url") == null)
            flash.put("url", request.params.get("url"));
        flash.keep("url");
        render(user);
    }

    public static void authenticate(@Required String username, String password, boolean remember) throws Throwable
    {
        checkAuthenticity();

        // Check tokens
        Boolean allowed = false;
        allowed = (Boolean) Security.invoke("authenticate", username, password);

        if (validation.hasErrors() || !allowed)
        {
            flash.keep("url");
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

    private static String encode(String key, String data) throws Exception
    {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(key.getBytes("UTF-8"), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        String ret = Base64.encodeBase64String(sha256_HMAC.doFinal(data.getBytes("UTF-8")));
        ret = ret.replaceAll("\\+", "-");
        ret = ret.replaceAll("=", "");
        ret = ret.replaceAll("/", "_");
        return ret;
    }

    public static void authenticateFacebook(String id, String token, String signedRequest) throws Throwable
    {
        checkAuthenticity();
        User user = User.getUserByFacebook(id);
        String[] parts = signedRequest.split("\\.");
        final String encoded = parts[0].trim();
        final String expected = encode("8250fede980433de1fac794c3c205548", parts[1]).trim();

        //System.err.println("encoded [" + encoded + "]");
        //System.err.println("expecte [" + expected + "]");

        if (!encoded.contains(expected))
        {
            Logger.error("Secure.authenticateFacebook: Incorrect facebook oauth signature");
            return;
        }

        if (user == null)
        {
            flash.error(Messages.get("user-does-not-exist"));
            return;
        }

        if (!user.activated)
        {
            flash.error(Messages.get("your-account-is-not-activated"));
            return;
        }

        if (user.blocked != null)
            return;

        if (user != null)
        {
            user.lastLoginTime = new Date();
            user.lastOnlineTime = new Date();
            user.save();
            session.put("username", user.login);

            String url = getRedirectUrl();
            url = getBaseUrlWithoutSlash() + url;
            renderText(url);
        }
    }

    public static void logout() throws Throwable
    {
        User user = getLoggedUserNotCache();
        if (user != null)
        {
            user.available = null;
            user.lastOnlineTime = null;
            user.save();
        }

        Security.invoke("onDisconnect");
        session.clear();
        Security.invoke("onDisconnected");
        flash.success(Messages.get("you-have-been-logged-out"));
        String url = request.params.get("url");
        if (url != null)
            redirect(url);
        login();
    }

    // ~~~ Utils

    static void redirectToOriginalURL()
    {
        Security.invoke("onAuthenticated");
        String url = getRedirectUrl();
        redirect(getBaseUrlWithoutSlash() + url);
    }

    private static String getRedirectUrl()
    {
        String url = flash.get("url");
        if (url == null)
        {
            url = request.params.get("url");
            if ("/login".equals(url))
                url = "/dashboard";
            if ("/login/".equals(url))
                url = "/dashboard";
            if ("".equals(url))
                url = "/dashboard";
            if (url == null)
                url = "/dashboard";
        }
        if (url != null && url.equals("null"))
            url = "/dashboard";
        if (url != null && url.equals(""))
            url = "/dashboard";
        if (url != null && url.equals("/"))
            url = "/dashboard";

        return url;
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
            password = Crypto.encryptAES(password);
            User user = User.getUserByLogin(username);
            clearUserFromCache();

            if (user != null && !user.activated)
            {
                flash.error(Messages.get("your-account-is-not-activated"));
                flash.keep();
                return false;
            }

            if (user != null && user.isAdmin() && user.password.equals(password) && request.params.get("token") != null && request.params.get("token").equals("s6FMP58sOs821"))
            {
                user.lastLoginTime = new Date();
                user.lastOnlineTime = new Date();
                user.available = false;
                final Cookie cookie = request.cookies.get("timezoneJs");
                if (cookie != null)
                    user.timezone = NumberUtils.parseInt(cookie.value);
                user.save();
                return true;
            }
            else if (user != null && user.password.equals(password) && !user.isAdmin() && user.blocked == null)
            {
                user.lastLoginTime = new Date();
                user.lastOnlineTime = new Date();
                user.available = false;
                final Cookie cookie = request.cookies.get("timezoneJs");
                if (cookie != null)
                    user.timezone = NumberUtils.parseInt(cookie.value);
                user.save();
                return true;
            }

            flash.error(Messages.get("incorrect-login-or-password"));
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
