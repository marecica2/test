package jobs;

import play.Logger;
import play.jobs.Every;
import play.jobs.Job;
import controllers.Admin;

@Every("1h")
public class ScheduletJob extends Job
{

    @Override
    public void doJob() throws Exception
    {
        Logger.info(Admin.refreshIndex());
    }

}
