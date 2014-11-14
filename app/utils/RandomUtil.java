package utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import java.util.UUID;

public class RandomUtil
{
    public static String getDoubleUUID()
    {
        final String uuid = (UUID.randomUUID().toString() + UUID.randomUUID().toString()).replaceAll("-", "");
        return uuid;
    }

    public static String getUUID()
    {
        final String uuid = (UUID.randomUUID().toString()).replaceAll("-", "");
        return uuid;
    }

    public static String getRandomDigits(int number)
    {
        return Math.round(Math.random() * (10 * number)) + "";
    }

    public static Integer getRandomInteger(int number)
    {
        Random r = new Random();
        return r.nextInt(number);
    }

    public static String getMD5Hex(final String inputString)
    {

        MessageDigest md;
        try
        {
            md = MessageDigest.getInstance("MD5");
            md.update(inputString.getBytes());
            byte[] digest = md.digest();
            return convertByteToHex(digest);
        } catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    private static String convertByteToHex(byte[] byteData)
    {

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < byteData.length; i++)
        {
            sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
        }

        return sb.toString();
    }
}
