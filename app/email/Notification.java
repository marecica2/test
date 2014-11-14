package email;

import play.Logger;

public class Notification implements Runnable
{

    private final EmailProvider emailProvider;
    private final String email;
    private final String subject;
    private final String htmlPart;
    private final String from;

    public Notification(EmailProvider ep, String from, String subject, String email, String htmlPart)
    {
        super();
        this.from = from;
        this.htmlPart = htmlPart;
        this.subject = subject;
        this.email = email;
        this.emailProvider = ep;
    }

    @Override
    public void run()
    {
        try
        {
            emailProvider.sendInvitation(from, subject, email, htmlPart);
            Logger.info("from thread Notification sent to " + email);
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void execute()
    {
        new Thread(this).start();
    }
}
