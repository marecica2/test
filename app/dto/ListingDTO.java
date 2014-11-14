package dto;

import models.Listing;
import models.User;

public class ListingDTO
{
    public String uuid;

    public String title;

    public long created;

    public String createdBy;

    public String createdByName;

    public String createdByLogin;

    public String createdByAvatarUrl;

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

    public String category;

    public static ListingDTO convert(Listing listing, User user)
    {
        ListingDTO l = new ListingDTO();
        l.color = listing.color;
        l.description = listing.description;
        l.title = listing.title;
        if (user != null)
            l.isOwner = false;
        else
            l.isOwner = listing.user.equals(user) ? true : false;
        l.type = listing.type;
        l.price = listing.price;
        l.currency = listing.currency;
        l.category = listing.category;
        l.state = listing.state;
        l.charging = listing.charging;
        l.imageUrl = listing.imageUrl;
        l.type = listing.type;
        l.privacy = listing.privacy;
        l.uuid = listing.uuid;
        l.isInvited = l.isOwner ? false : true;
        l.createdByUser = false;
        if (listing.created != null)
            l.created = listing.created.getTime();
        if (listing.user != null)
        {
            l.createdByName = listing.user.getFullName();
            l.createdByLogin = listing.user.login;
            l.createdBy = listing.user.uuid;
            l.createdByAvatarUrl = listing.user.avatarUrl;
        }
        return l;
    }

}
