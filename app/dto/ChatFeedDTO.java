package dto;

import models.ChatFeed;

public class ChatFeedDTO
{
    public String name;

    public String avatar;

    public String comment;

    public long created;

    public boolean isAnonymous;

    public boolean isCustomer;

    public static ChatFeedDTO convert(ChatFeed feed)
    {
        ChatFeedDTO e = new ChatFeedDTO();
        e.comment = feed.comment;
        e.created = feed.created.getTime();

        if (feed.user != null)
        {
            e.name = feed.user.getFullName();
            e.avatar = feed.user.avatarUrl;
            e.isCustomer = false;
            e.isAnonymous = false;
        } else if (feed.customer != null)
        {
            e.name = feed.customer.getFullName();
            e.avatar = feed.customer.avatarUrl;
            e.isCustomer = true;
            e.isAnonymous = false;
        } else
        {
            e.name = feed.name;
            e.isCustomer = true;
            e.isAnonymous = true;
        }
        return e;
    }
}
