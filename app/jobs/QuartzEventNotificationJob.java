package jobs;

import java.util.List;

import models.Attendance;
import models.User;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import play.db.jpa.JPAPlugin;
import play.i18n.Messages;
import utils.Constants;
import utils.DateTimeUtils;
import email.EmailNotificationBuilder;

public class QuartzEventNotificationJob extends play.jobs.Job implements Job
{
    private String attendanceKey = null;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException
    {
        attendanceKey = context.getJobDetail().getKey().getName();
        now();

        //            new Invoker.Invocation()
        //            {
        //                @Override
        //                public InvocationContext getInvocationContext()
        //                {
        //                    return new InvocationContext(invocationType);
        //                }
        //
        //                @Override
        //                public void execute() throws Exception
        //                {
        //                    (Attendance.getById(attendanceKey)).getClass();
        //
        //                }
        //            }.run();

    }

    @Override
    public void doJob() throws Exception
    {
        JPAPlugin.startTx(false);
        final List<Attendance> attendances = Attendance.getById(attendanceKey);
        if (attendances == null || attendances.size() == 0)
            return;

        final Attendance a = attendances.get(0);
        final User recipient = a.isForUser ? a.user : a.customer;
        final String locale = recipient.locale != null ? recipient.locale : "en";
        final DateTimeUtils dt = new DateTimeUtils(DateTimeUtils.TYPE_DEFAULT);

        final String baseUrl = Constants.getBaseUrl();
        final String subject = Messages.getMessage(locale, "event-reminder-subject", a.event.listing.title);
        final String body = Messages.getMessage(locale, "event-reminder-message",
                a.event.listing.title,
                a.event.listing.title,
                baseUrl,
                a.event.uuid,
                dt.formatDate(dt.applyOffset(a.event.eventStart, recipient.timezone))
                );

        final EmailNotificationBuilder eb = new EmailNotificationBuilder();
        eb.setWidgrFrom()
                .setTo(recipient)
                .setFrom(a.user)
                .setEvent(a.event)
                .setSubject(subject)
                .setMessageWiki(body)
                .sendInvitation();

        super.doJob();
    }
}
