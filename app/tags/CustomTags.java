package tags;

import groovy.lang.Closure;

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import play.templates.GroovyTemplate.ExecutableTemplate;
import play.templates.JavaExtensions;

public class CustomTags extends play.templates.FastTags
{
    public static void _urlenc(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine)
    {
        out.println(JavaExtensions.urlEncode(JavaExtensions.toString(body)));
    }

    public static void _formatDate(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine)
    {
        SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy");
        Long l = Long.parseLong(JavaExtensions.toString(body));
        Date d = new Date(l);
        out.println(df.format(d));
    }

    public static void _formatTime(Map<?, ?> args, Closure body, PrintWriter out, ExecutableTemplate template, int fromLine)
    {
        SimpleDateFormat df = new SimpleDateFormat("kk:mm");
        Long l = Long.parseLong(JavaExtensions.toString(body));
        Date d = new Date(l);
        out.println(df.format(d));
    }
}
