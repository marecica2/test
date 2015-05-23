package dto;

import models.ChatFeed;

public class ChatFeedDTO
{
    public String name;

    public String avatar;

    public String comment;

    public String senderName;

    public String sender;

    public String recipientName;

    public String recipient;

    public long created;

    public boolean isAnonymous;

    public boolean isCustomer;

    public static ChatFeedDTO convert(ChatFeed feed)
    {
        ChatFeedDTO e = new ChatFeedDTO();
        e.comment = feed.comment;
        e.created = feed.created.getTime();
        e.name = feed.name;
        e.recipientName = feed.recipientName;
        e.recipient = feed.recipient;
        e.senderName = feed.senderName;
        e.sender = feed.sender;
        return e;
    }
}
