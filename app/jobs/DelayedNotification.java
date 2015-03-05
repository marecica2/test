package jobs;

import models.Message;
import models.User;
import play.jobs.Job;

public class DelayedNotification extends Job
{
    private final String subject;
    private final String message;
    private User user;

    public DelayedNotification(User user, String subject, String message)
    {
        this.user = user;
        this.subject = subject;
        this.message = message;
    }

    public void scheduleNotification()
    {
        user = User.getUserByUUID(user.uuid);
        Message.createAdminNotification(user, subject, message);
    }

    @Override
    public void doJob() throws Exception
    {
        scheduleNotification();
    }

}
