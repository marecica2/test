package controllers;

import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;

import models.User;

import org.apache.http.client.utils.URIBuilder;

import play.libs.WS;
import play.libs.WS.HttpResponse;
import utils.PostParamsBuilder;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;

public class GoogleOAuth extends BaseController
{
    public static final String GOOGLE_OAUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/auth";
    public static final String GOOGLE_OAUTH_TOKEN = "https://accounts.google.com/o/oauth2/token";
    public static final String GOOGLE_OAUTH_REFRESH_TOKEN = "https://www.googleapis.com/oauth2/v3/token";

    public static final String CLIENT_ID = "647254293629-rfkfigbph07td6gl9h8lnstdjtebfng4.apps.googleusercontent.com";
    public static final String CLIENT_SECRET = "kHWp9OYZETjUjXvNN4-_HfZG";
    public static final String REDIRECT_URI = "https://localhost:10001/google-oauth";

    public static List<String> scopes = Lists.newArrayList("https://www.googleapis.com/auth/youtube.readonly", "https://www.googleapis.com/auth/calendar");

    // creates requestTokenUrl
    private static String getRequestTokenUrl(User user, String url)
    {
        try
        {
            String redirecUrl;
            redirecUrl = new URIBuilder()
                    .setPath(GOOGLE_OAUTH_ENDPOINT)
                    .addParameter("response_type", "code")
                    .addParameter("client_id", CLIENT_ID)
                    .addParameter("redirect_uri", getProperty(CONFIG_GOOGLE_OAUTH_CALLBACK))
                    .addParameter("scope", "https://www.googleapis.com/auth/youtube.readonly https://www.googleapis.com/auth/calendar")
                    .addParameter("state", url)
                    .addParameter("access_type", "offline")
                    .addParameter("approval_prompt", "auto")
                    .addParameter("login_hint", user.login).build().toString();
            return redirecUrl.toString();
        } catch (URISyntaxException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    // performs refresh token
    private static User refreshToken(User user)
    {
        try
        {
            String body = new PostParamsBuilder()
                    .addParameter("refresh_token", user.googleRefreshToken)
                    .addParameter("client_id", CLIENT_ID)
                    .addParameter("client_secret", CLIENT_SECRET)
                    .addParameter("grant_type", "refresh_token").build().toString();

            HttpResponse resp = WS.url(GOOGLE_OAUTH_REFRESH_TOKEN)
                    .setHeader("Content-Type", "application/x-www-form-urlencoded")
                    .body(body)
                    .post();

            // save response
            JsonObject json = resp.getJson().getAsJsonObject();
            String accessToken = json.get("access_token").getAsString();
            Long expires = json.get("expires_in").getAsLong();
            user = getLoggedUserNotCache();
            clearUserFromCache();

            user.googleAccessToken = accessToken;
            user.googleTokenExpires = new Date(System.currentTimeMillis() + (expires * 1000));
            user.save();
        } catch (Exception e)
        {
            // delete invalid tokens
            user.googleAccessToken = null;
            user.googleTokenExpires = null;
            user.googleRefreshToken = null;
            user.save();
        }
        return user;
    }

    public static void authorize(String url) throws Exception
    {
        User user = getLoggedUserNotCache();

        // token is valid
        if (user.googleTokenExpires != null && user.googleAccessToken != null && user.googleTokenExpires.getTime() > System.currentTimeMillis())
        {
            redirect(url);
        }

        // token is expired need to refresh
        else if (user.googleTokenExpires != null && user.googleAccessToken != null && user.googleTokenExpires.getTime() < System.currentTimeMillis())
        {
            refreshToken(user);
        }

        // need to create new token
        else
        {
            redirect(getRequestTokenUrl(user, url));
        }
    }

    public static String getAccessToken()
    {
        User user = getLoggedUser();
        String url = request.url;

        // token is valid
        if (user.googleTokenExpires != null && user.googleAccessToken != null && user.googleTokenExpires.getTime() > System.currentTimeMillis())
        {
            return user.googleAccessToken;
        }

        // token is expired need to refresh
        else if (user.googleTokenExpires != null && user.googleAccessToken != null && user.googleTokenExpires.getTime() < System.currentTimeMillis())
        {
            return refreshToken(user).googleRefreshToken;
        } else
        {
            redirect(getRequestTokenUrl(user, url));
        }
        return null;
    }

    // oauth callback
    public static void callback(String code, String state)
    {
        User user = getLoggedUserNotCache();
        try
        {
            String body = new PostParamsBuilder()
                    .addParameter("code", code)
                    .addParameter("client_id", CLIENT_ID)
                    .addParameter("client_secret", CLIENT_SECRET)
                    .addParameter("grant_type", "authorization_code")
                    .addParameter("redirect_uri", getProperty(CONFIG_GOOGLE_OAUTH_CALLBACK))
                    .build();

            // request for access token
            HttpResponse response = WS.url(GOOGLE_OAUTH_TOKEN)
                    .setHeader("Content-Type", "application/x-www-form-urlencoded")
                    .body(body)
                    .post();

            // save response
            JsonObject json = response.getJson().getAsJsonObject();
            String accessToken = json.get("access_token").getAsString();
            String refreshToken = json.get("refresh_token") != null ? json.get("refresh_token").getAsString() : null;
            Long expires = json.get("expires_in").getAsLong();

            clearUserFromCache();
            user.googleTokenExpires = new Date(System.currentTimeMillis() + (expires * 1000));
            user.googleAccessToken = accessToken;
            if (refreshToken != null)
                user.googleRefreshToken = refreshToken;
            user.save();

        } catch (Exception e)
        {
            // delete invalid tokens
            user.googleAccessToken = null;
            user.googleTokenExpires = null;
            user.googleRefreshToken = null;
            user.save();
        }

        flash.success("Oauth successfull");
        flash.keep();
        redirect(state);
    }
}
