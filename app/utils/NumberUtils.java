package utils;

import java.math.BigDecimal;

public class NumberUtils
{

    public static Integer parseInt(String value)
    {
        try
        {
            Integer i = Integer.parseInt(value);
            return i;
        } catch (Exception e)
        {
            return null;
        }
    }

    public static Double parseDouble(String value)
    {
        try
        {
            Double i = Double.parseDouble(value);
            return i;
        } catch (Exception e)
        {
            return null;
        }
    }

    public static BigDecimal parseDecimal(String value)
    {
        try
        {
            BigDecimal i = new BigDecimal(value);
            return i;
        } catch (Exception e)
        {
            return new BigDecimal("0");
        }
    }

}
