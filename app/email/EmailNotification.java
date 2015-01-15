package email;

import play.Logger;

public class EmailNotification implements Runnable
{

    private final EmailProvider emailProvider;
    private final String email;
    private final String subject;
    private final String htmlPart;

    public EmailNotification(EmailProvider ep, String subject, String email, String htmlPart)
    {
        super();
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
            emailProvider.sendEmail(subject, email, htmlPart);
            Logger.info("notification sent to " + email);
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
