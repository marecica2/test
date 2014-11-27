package dto;

import java.util.ArrayList;
import java.util.List;

import models.Event;
import models.User;

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

    public String currency;

    public String charging;

    public boolean isEditable;

    public boolean isOwner;

    public boolean isInvited;

    public boolean notifyInvited;

    public long eventStart;

    public long eventEnd;

    public Boolean createdByUser;

    public String state;

    private boolean invisible;

    private String createdByLogin;

    private Boolean archived;

    private String category;

    public List<CommentDTO> comments = new ArrayList<CommentDTO>();

    public static EventDTO convert(Event event, User user)
    {
        EventDTO e = new EventDTO();
        e.color = event.listing.color;
        e.description = event.listing.description;
        e.eventEnd = event.eventEnd.getTime();
        e.eventStart = event.eventStart.getTime();
        e.title = event.listing.title;
        if (user != null)
            e.isOwner = event.isOwner(user);
        else
            e.isOwner = false;
        e.type = event.listing.type;
        e.price = event.listing.price.toString();
        e.currency = event.listing.currency;
        e.category = event.listing.category;
        e.state = event.state;
        e.charging = event.listing.charging;
        e.imageUrl = event.listing.imageUrl;
        e.archived = event.archived;
        e.type = event.listing.type;
        e.privacy = event.listing.privacy;
        e.uuid = event.uuid;
        e.isInvited = e.isOwner ? false : true;
        e.createdByUser = event.createdByUser;
        if (event.customer != null)
        {
            e.customerAvatarUrl = event.customer.avatarUrl;
            e.customerLogin = event.customer.login;
            e.customer = event.customer.getFullName();
        }

        if (event.created != null)
            e.created = event.created.getTime();
        if (event.user != null)
        {
            e.createdByName = event.user.getFullName();
            e.createdByLogin = event.user.login;
            e.createdBy = event.user.uuid;
            e.createdByAvatarUrl = event.user.avatarUrl;
        }

        if (event.attendances != null && event.attendances.size() > 0)
            e.notifyInvited = true;
        else
            e.notifyInvited = false;
        return e;
    }

    public static EventDTO postProcessHiddenEvent(EventDTO e)
    {
        e.title = "";
        e.type = "";
        e.privacy = e.privacy;
        e.description = "";
        e.charging = "";
        e.price = "";
        e.color = "#FF0000";
        e.invisible = true;
        e.uuid = "";
        e.category = "";
        e.comments = null;
        e.currency = "";
        e.state = "";
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
        if (uuid == null)
        {
            if (other.uuid != null)
                return false;
        } else if (!uuid.equals(other.uuid))
            return false;
        return true;
    }

}
