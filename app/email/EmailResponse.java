package email;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Store;

public class EmailResponse
{
    private Folder folder;
    private Store store;
    private Message[] messages;
    private Long lastMessageUID;

    public Store getStore()
    {
        return store;
    }

    public void setStore(Store store)
    {
        this.store = store;
    }

    public Folder getFolder()
    {
        return folder;
    }

    public void setFolder(Folder folder)
    {
        this.folder = folder;
    }

    public Message[] getMessages()
    {
        return messages;
    }

    public void setMessages(Message[] messages)
    {
        this.messages = messages;
    }

    public Long getLastMessageUID()
    {
        return lastMessageUID;
    }

    public void setLastMessageUID(Long lastMessageUID)
    {
        this.lastMessageUID = lastMessageUID;
    }

    public void closeResources() throws MessagingException
    {
        if (folder != null && folder.isOpen())
        {
            folder.close(true);
        }
        if (store != null)
        {
            store.close();
        }
    }

}
