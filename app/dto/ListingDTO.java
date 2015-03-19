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

    public boolean commentsEnabled;

    public long eventStart;

    public long eventEnd;

    public String state;

    public String category;

    public Double ratingAvg;

    public Integer ratingCount;

    public boolean firstFree;

    public boolean available;

    public static ListingDTO convert(Listing listing, User user)
    {
        ListingDTO l = new ListingDTO();
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
        l.available = listing.isAvailable();
        if (listing.firstFree != null && listing.firstFree)
            l.firstFree = listing.firstFree;
        else
            l.firstFree = false;
        if (listing.commentsEnabled != null && listing.commentsEnabled)
            l.commentsEnabled = true;
        else
            l.commentsEnabled = false;
        l.createdBy = listing.user != null ? listing.user.uuid : null;
        l.createdByName = listing.user != null ? listing.user.getFullName() : null;
        l.createdByLogin = listing.user != null ? listing.user.login : null;
        l.createdByAvatarUrl = listing.user != null ? listing.user.avatarUrl : null;
        return l;
    }

}
