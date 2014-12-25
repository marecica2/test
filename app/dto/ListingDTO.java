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

    public Long ratingAvg;

    public Integer ratingCount;

    public Boolean firstFree;

    public static ListingDTO convert(Listing listing, User user)
    {
        ListingDTO l = new ListingDTO();
        if (user == null)
            l.isOwner = false;
        else
            l.isOwner = user.isOwner(listing);
        l.color = listing.color;
        l.description = listing.description;
        l.title = listing.title;
        l.type = listing.type;
        l.ratingAvg = listing.ratingAvg;
        l.ratingCount = listing.ratingStars != null ? listing.ratingStars : 0;
        l.price = listing.price != null ? listing.price.toString() : null;
        l.currency = listing.currency;
        l.category = listing.category;
        l.state = listing.state;
        l.charging = listing.charging;
        l.imageUrl = listing.imageUrl;
        l.type = listing.type;
        l.privacy = listing.privacy;
        l.uuid = listing.uuid;
        l.firstFree = listing.firstFree;
        l.isInvited = l.isOwner ? false : true;
        l.createdBy = listing.user != null ? listing.user.uuid : null;
        l.createdByUser = false;
        l.createdByName = listing.user != null ? listing.user.getFullName() : null;
        l.createdByLogin = listing.user != null ? listing.user.login : null;
        l.createdByAvatarUrl = listing.user != null ? listing.user.avatarUrl : null;
        return l;
    }

}
