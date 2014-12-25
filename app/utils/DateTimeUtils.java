package utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import play.Logger;

public class DateTimeUtils
{
    public static final String TYPE_DEFAULT = "dd/MM/yyyy HH:mm";
    public static final String TYPE_EUROPE = "dd.MM.yyyy HH:mm";
    public static final String TYPE_OTHER = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    public static final String TYPE_PAYPAL = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    // 2014-12-02T11:59:01+01:00
    public static final String TYPE_GOOGLE = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    public static final String TYPE_DATE_ONLY = "dd/MM/yyyy";
    public static final String TYPE_TIME_ONLY = "HH:mm";

    private final SimpleDateFormat defaultDateFormat;

    public DateTimeUtils()
    {
        defaultDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    }

    public DateTimeUtils(String type)
    {
        // "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
        defaultDateFormat = new SimpleDateFormat(type);
    }

    public Date fromJson(String jsonDate)
    {
        try
        {
            if (jsonDate == null)
                return null;
            if (jsonDate != null && jsonDate.equals(""))
                return null;
            return defaultDateFormat.parse(jsonDate);
        } catch (ParseException e)
        {
            return null;
        }
    }

    public Date fromString(String date)
    {
        try
        {
            return defaultDateFormat.parse(date);
        } catch (ParseException e)
        {
            Logger.error(e, "");
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    public String convertJsonDate(String jsonDate)
    {
        Date date;
        try
        {
            date = defaultDateFormat.parse(jsonDate);
            return date.toGMTString();
        } catch (ParseException e)
        {
            Logger.error(e, "");
            return "";
        }
    }

    public String formatDate(Date date, SimpleDateFormat df)
    {
        return df.format(date);
    }

    public Date parseDate(String date, SimpleDateFormat df)
    {
        try
        {
            return df.parse(date);
        } catch (ParseException e)
        {
            return null;
        }
    }

    public String formatDate(Date date)
    {
        return defaultDateFormat.format(date);
    }

    public static Date applyOffset(Date date, Integer ofs)
    {
        int offset = 0;
        offset = ofs * 1000 * 60;
        Long time = date.getTime() - offset;
        return new Date(time);
    }

}
