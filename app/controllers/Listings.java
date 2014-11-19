package controllers;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import models.Account;
import models.Comment;
import models.Event;
import models.FileUpload;
import models.Followers;
import models.Listing;
import models.ListingFilter;
import models.Rating;
import models.User;
import play.i18n.Messages;
import play.mvc.Before;
import utils.RandomUtil;
import utils.StringUtils;
import dto.ListingDTO;

public class Listings extends BaseController
{
    @Before(unless = { "listingNew", "listings" })
    static void checkAccess()
    {
        checkAuthorizedAccess();
    }

    public static void listings()
    {
        final User user = getLoggedUser();
        final Integer first = request.params.get("first") != null ? Integer.parseInt(request.params.get("first")) : null;
        final Integer count = request.params.get("count") != null ? Integer.parseInt(request.params.get("count")) : null;

        final ListingFilter filterListing = new ListingFilter();
        filterListing.search = StringUtils.getStringOrNull(request.params.get("search"));
        filterListing.sort = StringUtils.getStringOrNull(request.params.get("sort"));
        filterListing.category = StringUtils.getStringOrNull(request.params.get("category"));

        List<ListingDTO> ListingsDto = new ArrayList<ListingDTO>();
        List<Listing> listings = Listing.getFiltered(first, count, filterListing);
        for (Listing l : listings)
        {
            ListingDTO lDto = ListingDTO.convert(l, user);
            ListingsDto.add(lDto);
        }
        renderJSON(ListingsDto);
    }

    public static void listingNew(String action, String uuid, String url, String type)
    {
        final Boolean edit = action != null && action.equals("edit") ? true : false;
        final Boolean isPublic = false;
        final User user = getLoggedUser();
        final Account account = user != null ? user.account : null;
        final Listing listing = Listing.get(uuid);
        final Boolean isOwner = listing != null ? listing.user.equals(user) : false;
        final Followers follow = listing != null && user != null ? Followers.get(user, listing.user) : null;
        final String temp = RandomUtil.getDoubleUUID();
        final String commentTemp = RandomUtil.getDoubleUUID();
        final List<Comment> comments = Comment.getByObject(uuid);
        final Boolean fromEvent = false;

        final List<Rating> ratings = listing != null ? Rating.getByObject(uuid) : null;
        final Map<String, Object> stats = listing != null ? Rating.calculateStats(ratings) : null;

        if (listing != null)
        {
            params.put("title", listing.title);
            params.put("category", listing.category);
            params.put("tags", listing.tags);
            params.put("currency", listing.currency);
            params.put("description", listing.description);
            params.put("charging", listing.charging);
            params.put("type", listing.type);
            params.put("privacy", listing.privacy);
            if (listing.chatEnabled != null && listing.chatEnabled)
                params.put("chatEnabled", "true");
            params.put("price", listing.price);
            params.put("color", listing.color);
            params.put("image", listing.imageUrl);
        } else
        {
            params.put("privacy", Event.EVENT_VISIBILITY_PRIVATE);
            params.put("type", Event.EVENT_TYPE_P2P_CALL);
            params.put("charging", Event.EVENT_CHARGING_FREE);
            params.put("image", FileuploadController.PATH_TO_LISTING_AVATARS + "ava_" + RandomUtil.getRandomInteger(22) + ".png_thumb");
            params.put("currency", user.account.currency);
        }
        params.put("temp", temp);
        params.flash();

        Map<String, String> errs = new HashMap<String, String>();
        render(user, account, isPublic, isOwner, edit, listing, url, errs, type, follow, temp, commentTemp, comments, ratings, stats, fromEvent);
    }

    public static void listingNewPost(
        String action,
        String title,
        String description,
        String privacy,
        String charging,
        String type,
        String category,
        String tags,
        String price,
        String currency,
        String color,
        String url,
        String offset,
        String uuid,
        String image,
        String imageId,
        String imageTempUrl,
        String imageUrl,
        String chatEnabled,
        String temp
        )
    {
        Listing listing = Listing.get(uuid);
        boolean edit = action != null && action.equals("edit") ? true : false;
        final Boolean isPublic = false;
        final User user = getLoggedUser();
        final Account account = user.account;
        final Map<String, String> errs = new HashMap<String, String>();

        if (type == null)
            errs.put("type", Messages.get("validation.required"));
        if (charging == null)
            errs.put("charging", Messages.get("validation.required"));
        if (privacy == null)
            errs.put("privacy", Messages.get("validation.required"));
        if (Event.EVENT_CHARGING_BEFORE.equals(charging))
        {
            validation.required(price);
            validation.required(currency);
            validation.equals(charging, charging).message("validation.charging");
        }
        validation.required(title);
        validation.required(description);
        if (!validation.hasErrors() && errs.size() == 0)
        {
            if (listing == null)
            {
                listing = new Listing();
                listing.uuid = temp;
                listing.roomSecret = RandomUtil.getDoubleUUID();
                listing.created = new Date();
                listing.user = user;
                listing.imageUrl = FileuploadController.PATH_TO_LISTING_AVATARS + "ava_" + RandomUtil.getRandomInteger(22) + ".png";
                listing.account = account;
                listing.state = Event.EVENT_STATE_USER_CREATED;
            }
            listing.chatEnabled = chatEnabled != null ? true : null;
            listing.title = title;
            listing.account = account;
            listing.description = StringUtils.htmlEscape(description);
            listing.charging = charging;
            listing.price = price;
            listing.currency = currency;
            listing.color = color;
            listing.type = type;
            listing.privacy = privacy;
            listing.category = category;
            listing.tags = tags;
            if (imageUrl != null)
            {
                listing.imageUrl = imageUrl;
                listing.imageId = imageId;
            }
            listing.chatEnabled = chatEnabled != null ? true : null;
            listing.lastModified = new Date();
            listing.save();

            // mark file upload as stored
            if (imageId != null)
            {
                List<FileUpload> fu = FileUpload.getByObject(listing.uuid);
                for (FileUpload fileUpload : fu)
                {
                    fileUpload.stored = true;
                    fileUpload.save();
                }
            }
            redirect("/listing-new?action=view&uuid=" + listing.uuid);
        }
        params.flash();
        render("Listings/listingNew.html", user, account, isPublic, edit, listing, errs);
    }

    public void deleteListing(String id, String url)
    {
        Listing listing = Listing.get(id);
        listing.delete();
        redirectTo(url);
    }

    public static void resetImage(String id, String url)
    {
        final Listing e = Listing.get(id);
        e.imageUrl = FileuploadController.PATH_TO_LISTING_AVATARS + "ava_" + RandomUtil.getRandomInteger(22) + ".png_thumb";
        e.save();
        redirectTo(url);
    }
}