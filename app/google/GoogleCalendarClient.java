package google;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import models.User;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;

import controllers.GoogleOAuth;
import dto.EventDTO;

public class GoogleCalendarClient
{
    public static String getPrimaryCalendar(User user)
    {
        try
        {
            Calendar service = initCalendar(user);
            final CalendarList calendars = service.calendarList().list().execute();
            final List<CalendarListEntry> items = calendars.getItems();
            for (CalendarListEntry cal : items)
            {
                if (cal.getPrimary() != null && cal.getPrimary())
                    return cal.getId();
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public static List<CalendarListEntry> getCalendars(User user)
    {
        try
        {
            Calendar service = initCalendar(user);
            final CalendarList calendars = service.calendarList().list().set("showHidden", true).execute();
            final List<CalendarListEntry> items = calendars.getItems();
            return items;
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

    private static Calendar initCalendar(User user) throws GeneralSecurityException, IOException
    {
        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        JacksonFactory jsonFactory = JacksonFactory.getDefaultInstance();

        Credential credential = new GoogleCredential.Builder()
                .setTransport(httpTransport)
                .setJsonFactory(jsonFactory)
                .setClientSecrets(GoogleOAuth.CLIENT_ID, GoogleOAuth.CLIENT_SECRET)
                .build()
                .setAccessToken(GoogleOAuth.getAccessToken())
                .setRefreshToken(user.googleRefreshToken)
                .setExpirationTimeMilliseconds(user.googleTokenExpires.getTime());

        Calendar service = new Calendar.Builder(httpTransport, jsonFactory, credential).setApplicationName("widgr").build();
        return service;
    }

    public static void updateEvent(User user, String id, Event content)
    {
        try
        {
            Calendar service = initCalendar(user);
            service.events().update(user.googleCalendarId, id, content).execute();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void insertEvent(User user, Event content)
    {
        try
        {
            Calendar service = initCalendar(user);
            service.events().insert(user.googleCalendarId, content).execute();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static Event getEvent(User user, String eventId)
    {
        try
        {
            Calendar service = initCalendar(user);
            Event e = service.events().get(user.googleCalendarId, eventId).setEventId(eventId).execute();
            return e;
        } catch (Exception e)
        {
        }
        return null;
    }

    public static com.google.api.services.calendar.model.Event getGoogleEvent(User user, models.Event event)
    {
        com.google.api.services.calendar.model.Event existing = null;
        if (event.googleId != null)
            existing = GoogleCalendarClient.getEvent(user, event.googleId);
        if (existing == null)
            existing = GoogleCalendarClient.getEvent(user, event.uuid);
        return existing;
    }

    public static void upsertGoogleEvent(final User user, models.Event event, String baseUrl)
    {
        com.google.api.services.calendar.model.Event existing = GoogleCalendarClient.getGoogleEvent(user, event);

        if (existing != null)
        {
            com.google.api.services.calendar.model.Event update = new com.google.api.services.calendar.model.Event();
            update.setStart(new EventDateTime().setDateTime(new DateTime(event.eventStart.getTime())));
            update.setEnd(new EventDateTime().setDateTime(new DateTime(event.eventEnd.getTime())));
            update.setSummary(event.listing.title);

            update.setDescription(event.listing.description + "\n" + baseUrl + "event/" + event.uuid);
            update.set("link", baseUrl + "event/" + event.uuid);
            update.set("url", baseUrl + "event/" + event.uuid);
            update.set("location", baseUrl + "event/" + event.uuid);
            update.set("source.url", baseUrl + "event/" + event.uuid);
            update.set("source.title", baseUrl + "event/" + event.uuid);
            GoogleCalendarClient.updateEvent(user, existing.getId(), update);
        }

        if (existing == null)
        {
            com.google.api.services.calendar.model.Event insert = new com.google.api.services.calendar.model.Event();
            insert.setId(event.uuid);
            insert.setStart(new EventDateTime().setDateTime(new DateTime(event.eventStart.getTime())));
            insert.setEnd(new EventDateTime().setDateTime(new DateTime(event.eventEnd.getTime())));
            insert.setSummary(event.listing.title);
            insert.setDescription(event.listing.description + "\n\n" + baseUrl + "event/" + event.uuid + "");
            //insert.set("location", baseUrl + "event/" + event.uuid);
            GoogleCalendarClient.insertEvent(user, insert);
        }
    }

    public static void deleteGoogleEvent(User user, models.Event event)
    {
        try
        {
            com.google.api.services.calendar.model.Event existing = GoogleCalendarClient.getGoogleEvent(user, event);
            System.err.println("deleting  " + existing.getId());
            if (existing != null)
            {
                Calendar service = initCalendar(user);
                service.events().delete(user.googleCalendarId, existing.getId()).execute();
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static List<EventDTO> getEvents(User user, Date from, Date to)
    {
        List<EventDTO> eventsDto = new ArrayList<EventDTO>();
        try
        {
            Calendar service = initCalendar(user);
            Events events = service.events().list(user.googleCalendarId)
                    .set("timeMin", new DateTime(from.getTime()))
                    .set("timeMax", new DateTime(to.getTime()))
                    .execute();
            List<Event> items = events.getItems();
            for (Event event : items)
            {
                eventsDto.add(EventDTO.convertGoogleEvent(event, user));
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return eventsDto;
    }
}
