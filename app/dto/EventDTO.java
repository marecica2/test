package dto;

import java.util.ArrayList;
import java.util.List;

import models.Event;
import models.User;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.google.gson.JsonObject;

public class EventDTO
{
    public String uuid;

    public String title;

    public long created;

    public String createdBy;

    public String createdByName;

    public String createdByAvatarUrl;

    public String customerAvatarUrl;
    public String customerLogin;
    public String customer;

    public String description;

    public String color;

    public String type;

    public String privacy;

    public String status;

    public String imageUrl;

    public String price;

    public String priceTotal;

    public String currency;

    public String charging;

    public String chargingTime;

    public boolean isEditable;

    public boolean firstFree;

    public boolean isOwner;

    public boolean isInvited;

    public boolean notifyInvited;

    public long eventStart;

    public long eventEnd;

    public Boolean createdByUser;

    public String state;

    public String googleId;

    public String createdByLogin;

    public Boolean archived;

    public Boolean invisible;

    public String category;

    public List<CommentDTO> comments = new ArrayList<CommentDTO>();

    public static EventDTO convert(Event event, User user)
    {
        EventDTO e = new EventDTO();
        e.color = event.listing.color;
        e.description = event.listing.description;
        e.firstFree = event.listing.firstFree;
        e.eventEnd = event.eventEnd.getTime();
        e.eventStart = event.eventStart.getTime();
        e.title = event.listing.title;
        e.category = event.listing.category;
        e.imageUrl = event.listing.imageUrl;
        e.state = event.state;
        e.privacy = event.privacy;
        e.type = event.type;
        e.currency = event.currency;
        e.price = event.price.toString();
        e.priceTotal = event.getTotalPrice().toString();
        e.charging = event.charging;
        e.uuid = event.uuid;
        e.archived = event.archived;
        e.createdByUser = event.createdByUser;
        e.googleId = event.googleId;
        if (event.chargingTime != null)
            e.chargingTime = event.chargingTime.toString();
        if (user != null)
            e.isOwner = event.isOwner(user);
        else
            e.isOwner = false;
        if (user != null)
            e.isInvited = event.hasInviteFor(user);
        else
            e.isInvited = false;
        if (event.customer != null)
        {
            e.customerAvatarUrl = event.customer.avatarUrl;
            e.customerLogin = event.customer.login;
            e.customer = event.customer.getFullName();
        }
        if (event.user != null)
        {
            e.createdByName = event.user.getFullName();
            e.createdByLogin = event.user.login;
            e.createdBy = event.user.uuid;
            e.createdByAvatarUrl = event.user.avatarUrl;
        }
        if (event.created != null)
            e.created = event.created.getTime();
        if (event.attendances != null && event.attendances.size() > 0)
            e.notifyInvited = true;
        else
            e.notifyInvited = false;
        return e;
    }

    public static EventDTO postProcessHiddenEvent(EventDTO e)
    {
        e.title = null;
        e.type = null;
        e.privacy = e.privacy;
        e.description = null;
        e.charging = null;
        e.price = null;
        e.color = "#FF0000";
        e.invisible = true;
        e.uuid = null;
        e.category = null;
        e.imageUrl = null;
        e.comments = null;
        e.currency = null;
        e.state = null;
        e.created = 0;
        return e;
    }

    @Override
    public String toString()
    {
        return "EventDTO [uuid=" + uuid + ", title=" + title + ", description=" + description + ", color=" + color + ", type=" + type + ", privacy=" + privacy + ", status=" + status
                + ", isEditable=" + isEditable + ", notifyInvited=" + notifyInvited + ", eventStart=" + eventStart + ", eventEnd=" + eventEnd + ", createdByUser=" + createdByUser + "]";
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;

        EventDTO other = (EventDTO) obj;
        if (googleId != null && other.googleId != null && googleId.equals(other.googleId))
            return true;

        if (uuid == null)
        {
            if (other.uuid != null)
                return false;
        } else if (!uuid.equals(other.uuid))
            return false;
        return true;
    }

    public static EventDTO convertGoogle(JsonObject event, User user)
    {
        final DateTimeFormatter dtf = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZZ");
        final String start = event.get("start").getAsJsonObject().get("dateTime").getAsString();
        final String end = event.get("end").getAsJsonObject().get("dateTime").getAsString();

        EventDTO e = new EventDTO();
        e.color = "#000000";
        e.googleId = event.get("id").getAsString();
        e.eventStart = dtf.parseDateTime(start).getMillis();
        e.eventEnd = dtf.parseDateTime(end).getMillis();
        e.title = event.get("summary").getAsString();
        if (user != null)
        {
            e.createdByName = user.getFullName();
            e.createdByLogin = user.login;
            e.createdBy = user.uuid;
            e.createdByAvatarUrl = user.avatarUrl;
        }
        return e;
    }
}
