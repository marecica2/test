package utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;

import org.eclipse.mylyn.internal.wikitext.confluence.core.ConfluenceDocumentBuilder;
import org.eclipse.mylyn.internal.wikitext.core.parser.html.HtmlParser;
import org.eclipse.mylyn.wikitext.confluence.core.ConfluenceLanguage;
import org.eclipse.mylyn.wikitext.core.parser.MarkupParser;
import org.eclipse.mylyn.wikitext.core.parser.builder.HtmlDocumentBuilder;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class WikiUtils
{
    public static String parseToHtml(String content)
    {
        try
        {
            StringWriter writer = new StringWriter();
            HtmlDocumentBuilder builder = new HtmlDocumentBuilder(writer);
            // avoid the <html> and <body> tags 
            builder.setEmitAsDocument(false);

            MarkupParser parser = new MarkupParser(new ConfluenceLanguage());
            parser.setBuilder(builder);
            parser.parse(content);
            String htmlContent = writer.toString();
            htmlContent = htmlContent.replaceAll("\\[mailto:(.+?)\\]", "<a href=\"mailto:$1\">$1</a>");
            htmlContent = htmlContent.replaceAll("\\[(.+?)\\|(.+?)\\]", "<a href=\"$2\">$1</a>");
            htmlContent = htmlContent.replaceAll("\\>mailto:(.+?)\\</", ">$1</");
            htmlContent = htmlContent.replaceAll("\\{.+?\\}", "");
            return htmlContent;
        } catch (Exception e)
        {
            return content;
        }
    }

    public static String parseToWiki(String content) throws IOException, SAXException
    {
        try
        {
            content = content.replaceAll("<\\s*a\\s+.*?href\\s*=\\s*'(\\S*?)'.*?>(.*?)</a>", "[$2|$1]");
            content = content.replaceAll("<\\s*a\\s+.*?href\\s*=\\s*\"(\\S*?)\".*?>(.*?)</a>", "[$2|$1]");

            InputSource inputSource = new InputSource(new ByteArrayInputStream(content.getBytes("UTF-8")));
            StringWriter writer = new StringWriter();
            ConfluenceDocumentBuilder builder = new ConfluenceDocumentBuilder(writer);
            HtmlParser parser = new HtmlParser();
            parser.parse(inputSource, builder);
            content = writer.toString();
            content = content.replaceAll("\\\\", "\\\n");
            return content;
        } catch (Exception e)
        {
            return content;
        }
    }
}
