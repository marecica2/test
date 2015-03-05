package email;

import java.util.List;

import play.Logger;

public class EmailNotificationRunner implements Runnable
{
    private EmailProvider emailProvider = null;
    private String email = null;
    private String subject = null;
    private String htmlPart = null;

    List<EmailContainer> emails;

    public EmailNotificationRunner(EmailProvider ep, String subject, String email, String htmlPart)
    {
        this.htmlPart = htmlPart;
        this.subject = subject;
        this.email = email;
        this.emailProvider = ep;
    }

    public EmailNotificationRunner(EmailProvider ep, List<EmailContainer> emails)
    {
        this.emails = emails;
    }

    @Override
    public void run()
    {
        try
        {
            if (emails != null)
            {
                emailProvider.sendEmails(emails);
                Logger.info("multiple notifications sent");
            } else
            {
                emailProvider.sendEmail(subject, email, htmlPart);
                Logger.info("notification sent to " + email);
            }

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
