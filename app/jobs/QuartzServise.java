package jobs;

import static org.quartz.TriggerBuilder.newTrigger;

import java.util.Date;
import java.util.List;

import models.Attendance;
import models.Event;
import models.User;

import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;

import play.Logger;
import play.jobs.Job;
import play.jobs.OnApplicationStart;

@OnApplicationStart
public class QuartzServise extends Job
{
    public static final String EVENT_NOTIFICATION_GROUP = "eventNotification";
    private static Scheduler scheduler = null;

    @Override
    public void doJob() throws Exception
    {
        try
        {
            scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();

            for (String group : scheduler.getJobGroupNames())
            {
                // enumerate each job in group
                for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(group)))
                {
                    Logger.info("Found persistent scheduled job: " + jobKey);
                }
            }
            Logger.info("Quartz has been initialized");
        } catch (SchedulerException se)
        {
            Logger.error(se, "Error in Quartz");
        }
    }

    public static void scheduleAttendance(Attendance attendance)
    {
        // ignore events from the past
        boolean isAfter = attendance.event.eventStart.after(new Date()) ? true : false;
        if (!isAfter)
            return;

        try
        {
            // define job
            JobDetail job = JobBuilder.newJob(QuartzEventNotificationJob.class).withIdentity(attendance.uuid, EVENT_NOTIFICATION_GROUP).build();

            User recipient = attendance.isForUser ? attendance.user : attendance.customer;
            if (recipient != null && recipient.reminder != null && recipient.reminder && recipient.reminderMinutes != null)
            {
                Date scheduledTime = new Date(attendance.event.eventStart.getTime() - (1000 * 60 * recipient.reminderMinutes));
                Logger.info("Notification is scheduled for event " + attendance.event.listing.title + " for " + recipient.login + " " + scheduledTime);

                Trigger oldTrigger = scheduler.getTrigger(new TriggerKey(attendance.uuid, EVENT_NOTIFICATION_GROUP));

                // define trigger
                SimpleTrigger trigger = (SimpleTrigger) newTrigger()
                        .withIdentity(attendance.uuid, EVENT_NOTIFICATION_GROUP)
                        .startAt(scheduledTime)
                        .forJob(job.getKey())
                        .build();

                // schedule or reschedule
                if (oldTrigger != null)
                    scheduler.rescheduleJob(oldTrigger.getKey(), trigger);
                else
                    scheduler.scheduleJob(job, trigger);
            }
        } catch (SchedulerException se)
        {
            Logger.error(se, "Error in Quartz");
        }
    }

    public static void scheduleEvent(Event event)
    {
        final List<Attendance> attendances = event.attendances;
        for (Attendance attendance : attendances)
            scheduleAttendance(attendance);
    }

    public static void unScheduleAttendance(Attendance attendance)
    {
        final JobKey jobKey = new JobKey(attendance.uuid, EVENT_NOTIFICATION_GROUP);
        try
        {
            if (scheduler.getJobDetail(jobKey) != null)
            {
                User recipient = attendance.isForUser ? attendance.user : attendance.customer;
                Logger.info("Notification is unscheduled for " + recipient.login);
                scheduler.deleteJob(jobKey);
            }
        } catch (SchedulerException se)
        {
            Logger.error(se, "Error in Quartz");
        }
    }

    public static void unScheduleEvent(Event event)
    {
        final List<Attendance> attendances = event.attendances;
        for (Attendance attendance : attendances)
            unScheduleAttendance(attendance);
    }
}
