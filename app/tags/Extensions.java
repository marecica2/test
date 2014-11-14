package tags;

import play.templates.JavaExtensions;

public class Extensions extends JavaExtensions
{
    public static String wiki(String input)
    {
        input = input.replaceAll("\\*(.+?)\\*", "<strong>$1</strong>");
        input = input.replaceAll("\n", "<br/>");
        return input;
    }

    public static String trimDot(String input, int len)
    {
        if (input.length() > len && input.length() > 50)
        {
            input = input.substring(0, len - 3);
            input += " ...";
        }
        return input;
    }
}
