package utils;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.math.BigDecimal;

import org.apache.commons.io.IOUtils;

import play.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class JsonUtils
{
    public static JsonObject getJson(InputStream is)
    {
        try
        {
            String json = IOUtils.toString(is);
            final JsonElement js = new JsonParser().parse(json);
            return js.getAsJsonObject();
        } catch (Exception e)
        {
            Logger.error(e, "Error occured while parsing json");
        }
        return null;
    }

    public static JsonArray getJsonArray(InputStream is)
    {
        try
        {
            String json = IOUtils.toString(is);
            final JsonElement js = new JsonParser().parse(json);
            return js.getAsJsonArray();
        } catch (Exception e)
        {
            Logger.error(e, "Error occured while parsing json array");
        }
        return null;
    }

    public static JsonArray getJsonArray(String json)
    {
        try
        {
            final JsonElement js = new JsonParser().parse(json);
            return js.getAsJsonArray();
        } catch (Exception e)
        {
            Logger.error(e, "Error occured while parsing json array");
        }
        return null;
    }

    public static JsonObject getJson(String json)
    {
        try
        {
            final JsonElement js = new JsonParser().parse(json);
            return js.getAsJsonObject();
        } catch (Exception e)
        {
            Logger.error(e, "Error occured while parsing json");
        }
        return null;
    }

    public static String getJsonAttribute(JsonObject jo, String attr)
    {
        try
        {
            return jo.getAsJsonObject().get(attr).getAsString();
        } catch (Exception e)
        {
            Logger.error(e, "Error occured while parsing json");
        }
        return null;
    }

    public static JsonElement getJsonAttributeAsJson(JsonObject jo, String attr)
    {
        try
        {
            return jo.getAsJsonObject().get(attr);
        } catch (Exception e)
        {
            Logger.error(e, "Error occured while parsing json");
        }
        return null;
    }

    public static <T> T populateObject(JsonObject jo, Class<T> cls)
    {
        try
        {
            T obj = cls.newInstance();
            final Field[] declaredFields = cls.getDeclaredFields();
            for (int i = 0; i < declaredFields.length; i++)
            {
                final String field = declaredFields[i].getName();

                if (getJsonAttribute(jo, field) != null)
                {
                    if (cls.getField(field).getType() == Integer.class)
                    {
                        cls.getField(field).set(obj, Integer.valueOf(getJsonAttribute(jo, field)));
                    } else if (cls.getField(field).getType() == Double.class)
                    {
                        cls.getField(field).set(obj, Double.valueOf(getJsonAttribute(jo, field)));
                    } else if (cls.getField(field).getType() == Long.class)
                    {
                        cls.getField(field).set(obj, Long.valueOf(getJsonAttribute(jo, field)));
                    } else if (cls.getField(field).getType() == Boolean.class)
                    {
                        if (getJsonAttribute(jo, field) == null)
                            cls.getField(field).set(obj, Boolean.valueOf(false));
                        else
                            cls.getField(field).set(obj, Boolean.valueOf(getJsonAttribute(jo, field)));
                    } else if (cls.getField(field).getType() == BigDecimal.class)
                    {
                        cls.getField(field).set(obj, new BigDecimal(getJsonAttribute(jo, field)));
                    } else if (cls.getField(field).getType() == String.class)
                    {
                        cls.getField(field).set(obj, getJsonAttribute(jo, field));
                    }
                }
            }
            return obj;
        } catch (Exception e)
        {
        }
        return null;
    }
}
