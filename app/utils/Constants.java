package utils;

import play.Play;
import controllers.BaseController;

public class Constants
{
    public static final String CONFIG_BASE_URL = "star.configuration.baseurl";

    public static final String MAIL_PROTOCOL_SMTP = "smtp";
    public static final String MAIL_PROTOCOL_SMTPS = "smtps";

    public static final String MAIL_HOST = BaseController.getProperty(BaseController.CONFIG_EMAIL_HOST);
    public static final String MAIL_PORT = BaseController.getProperty(BaseController.CONFIG_EMAIL_PORT);
    public static final String MAIL_ACCOUNT = BaseController.getProperty(BaseController.CONFIG_EMAIL_ACCOUNT);
    public static final String MAIL_PASSWORD = BaseController.getProperty(BaseController.CONFIG_EMAIL_PASSWORD);

    public static String getProperty(String key)
    {
        return Play.configuration.getProperty(key);
    }

    public static String getBaseUrl()
    {
        return getProperty(CONFIG_BASE_URL);
    }

}
