package utils;

public class PostParamsBuilder
{
    StringBuilder req;

    public PostParamsBuilder()
    {
        req = new StringBuilder();
    }

    public PostParamsBuilder addParameter(String param, String value)
    {
        req.append(param);
        req.append("=");
        req.append(value);
        req.append("&");
        return this;
    }

    public String build()
    {
        String resp = req.toString();
        resp = resp.substring(0, resp.length() - 1);
        return resp;
    }
}
