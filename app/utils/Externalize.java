package utils;

import java.io.File;
import java.io.FileInputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;

public class Externalize
{
    public final Map<String, String> map = new LinkedHashMap<String, String>();

    public void traverse(File file)
    {
        if (file.isDirectory())
        {
            String entries[] = file.list();

            if (entries != null)
            {
                for (String entry : entries)
                {
                    traverse(new File(file, entry));
                }
            }
        } else
        {
            //System.out.println(file);
            if (file.getName().contains(".js") || file.getName().contains(".java") || file.getName().contains(".html"))
            {
                String ext = file.getName().contains("js") ? "js" : file.getName().contains("java") ? "java" : "html";
                map.put("#" + file.getName(), "file");
                try
                {
                    String content = IOUtils.toString(new FileInputStream(file));
                    Matcher m = null;
                    if (ext.equals("js"))
                        m = Pattern.compile("i18n\\([\"'](.+?)[\"']\\)").matcher(content);
                    if (ext.equals("java"))
                        m = Pattern.compile("Messages.get\\(\"(.+?)\"").matcher(content);
                    if (ext.equals("html"))
                        m = Pattern.compile("\\&\\{'([^}]+)\\'}?").matcher(content);

                    while (m.find())
                    {
                        map.put(m.group(1), m.group(1));
                    }
                } catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String[] args)
    {
        Externalize rt = new Externalize();
        final Map<String, String> keys = rt.map;

        rt.traverse(new File("f:/workspace/widgr/app/"));
        rt.traverse(new File("f:/workspace/widgr/public/javascripts/"));

        for (String key : keys.keySet())
        {
            final String value = rt.map.get(key);
            if (!value.equals("file"))
            {
                String val = value.replaceAll("-", " ");
                String v = val.substring(1);
                String f = val.substring(0, 1).toUpperCase();
                System.err.println(key + "=" + f + v);
            } else
            {
                System.err.println("");
                System.err.println(key);
            }
        }
    }
}
