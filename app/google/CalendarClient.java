package google;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import models.User;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import play.libs.WS;
import play.libs.WS.HttpResponse;
import play.libs.WS.WSRequest;
import utils.DateTimeUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import controllers.GoogleOAuth;
import dto.EventDTO;

public class CalendarClient
{
    private static WSRequest authorized(final String url, final String token)
    {
        return WS.url(url).setHeader("Authorization", "Bearer " + token);
    }

    public static String getPrimaryCalendar(User user)
    {
        HttpResponse response = authorized("https://www.googleapis.com/calendar/v3/users/me/calendarList", GoogleOAuth.getAccessToken()).get();
        JsonObject jo = response.getJson().getAsJsonObject();
        JsonArray ja = jo.get("items").getAsJsonArray();
        for (JsonElement je : ja)
        {
            JsonObject jo1 = je.getAsJsonObject();
            if (jo1.get("primary") != null && jo1.get("primary").getAsBoolean())
                return jo1.get("id").getAsString();
        }
        return null;
    }

    public static List<EventDTO> getEvents(User user, String calendarId, Date from, Date to)
    {
        List<EventDTO> events = new ArrayList<EventDTO>();
        try
        {
            DateTimeUtils dt = new DateTimeUtils(DateTimeUtils.TYPE_GOOGLE);
            HttpResponse response = authorized("https://www.googleapis.com/calendar/v3/calendars/" + calendarId + "/events", GoogleOAuth.getAccessToken())
                    .setParameter("timeMin", dt.formatDate(from))
                    .setParameter("timeMax", dt.formatDate(to))
                    .get();
            JsonObject jo = response.getJson().getAsJsonObject();
            JsonArray ja = jo.get("items").getAsJsonArray();
            for (JsonElement je : ja)
            {
                JsonObject jo1 = je.getAsJsonObject();
                events.add(EventDTO.convertGoogle(jo1, user));
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return events;
    }

    public static void updateEvent(User user, String calendarId, String googleId, Date from, Date to)
    {
        try
        {
            final DateTimeFormatter dtf = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZZ");
            JsonObject joReq = new JsonObject();

            JsonObject start = new JsonObject();
            start.addProperty("dateTime", dtf.print(from.getTime()));
            joReq.add("start", start);

            JsonObject end = new JsonObject();
            end.addProperty("dateTime", dtf.print(to.getTime()));
            joReq.add("end", end);

            HttpResponse response = authorized("https://www.googleapis.com/calendar/v3/calendars/" + calendarId + "/events/" + googleId, GoogleOAuth.getAccessToken())
                    .setHeader("Content-type", "application/json")
                    .body(joReq)
                    .put();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
