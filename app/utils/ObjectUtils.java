package utils;

import java.lang.reflect.Field;

public class ObjectUtils
{
    public static void copyProperties(Object from, Object to)
    {
        final Field[] declaredFields = from.getClass().getDeclaredFields();
        for (int i = 0; i < declaredFields.length; i++)
        {
            try
            {
                final String field = declaredFields[i].getName();
                to.getClass().getField(field).set(to, from.getClass().getField(field).get(from));
            } catch (Exception e)
            {
            }
        }

    }
}
