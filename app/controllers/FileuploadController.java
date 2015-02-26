package controllers;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import javax.imageio.ImageIO;

import models.FileUpload;
import models.Listing;
import models.User;

import org.apache.commons.io.FileUtils;

import play.Play;
import play.cache.Cache;
import play.libs.Images;
import utils.NumberUtils;
import utils.RandomUtil;

import com.google.gson.JsonObject;

public class FileuploadController extends BaseController
{
    public static final String PATH_TO_UPLOADS_FILESYSTEM = Play.applicationPath + "/public/uploads/";
    public static final String PATH_TO_LISTING_AVATARS = "public/images/avatars/";
    public static final String PATH_TO_IMAGES = "public/images/";
    public static final String PATH_TO_UPLOADS = "public/uploads/";
    public static final Integer FILESIZE = 2500000;

    public static void removeFile(String uuid, String url) throws IOException
    {
        FileUpload fu = FileUpload.getByUuid(uuid);
        if (fu != null)
        {
            String filename = fu.url;
            File f = new File(PATH_TO_UPLOADS_FILESYSTEM + filename);
            f.delete();
            File ft = new File(PATH_TO_UPLOADS_FILESYSTEM + filename + "_thumb");
            ft.delete();
            fu.delete();
        }
        redirectTo(url);
    }

    public static void uploadFile(String item, String temp, File attachment, String contentType, String size, String name) throws IOException
    {
        checkAuthenticity();
        int fileSize = NumberUtils.parseInt(size);
        if (fileSize < FILESIZE)
        {
            FileUpload fu = createFile(item, temp, attachment, contentType, size, name);
            JsonObject jo = new JsonObject();
            jo.addProperty("url", fu.url);
            jo.addProperty("uuid", fu.uuid);
            jo.addProperty("name", name);
            jo.addProperty("size", fu.size);
            jo.addProperty("extension", fu.contentType);
            renderJSON(jo.toString());
        } else
        {
            response.status = 400;
            renderText("{\"exception\":\"Invalid file size or content type\"}");
        }
    }

    public static void cropImage(String imageId, String url, Integer x1, Integer y1, Integer x2, Integer y2, String type, String objectId)
    {
        FileUpload fu = FileUpload.getByUuid(imageId);
        final String filename = fu.getUrl();
        final File source = new File(PATH_TO_UPLOADS_FILESYSTEM + filename);

        // new id
        fu.uuid = RandomUtil.getUUID();
        fu.save();
        final File destination = new File(PATH_TO_UPLOADS_FILESYSTEM + fu.getUrl());
        Images.crop(source, destination, x1, y1, x2, y2);
        source.delete();

        if ("avatar".equals(type))
        {
            File destinationThumb1 = new File(PATH_TO_UPLOADS_FILESYSTEM + fu.getUrl() + "_64x64");
            Images.resize(destination, destinationThumb1, 64, 64, true);
            File destinationThumb2 = new File(PATH_TO_UPLOADS_FILESYSTEM + fu.getUrl() + "_32x32");
            Images.resize(destination, destinationThumb2, 32, 32, true);

            User user = getLoggedUserNotCache();
            user.avatarUrl = fu.getUrl();
            user.save();
            Cache.delete(user.login);
        }

        if ("listing".equals(type))
        {
            // aspect ration 2.5
            File destinationThumb1 = new File(PATH_TO_UPLOADS_FILESYSTEM + fu.getUrl() + "_128x128");
            Images.resize(destination, destinationThumb1, 320, 128, true);
            File destinationThumb3 = new File(PATH_TO_UPLOADS_FILESYSTEM + fu.getUrl() + "_64x64");
            Images.resize(destination, destinationThumb3, 160, 64, true);
            File destinationThumb2 = new File(PATH_TO_UPLOADS_FILESYSTEM + fu.getUrl() + "_32x32");
            Images.resize(destination, destinationThumb2, 80, 32, true);

            Listing l = Listing.get(objectId);
            if (l != null)
            {
                l.imageId = fu.uuid;
                l.imageUrl = PATH_TO_UPLOADS + fu.getUrl();
                l.save();
            }
            if (url.contains("imageUrl"))
                url = url.substring(0, url.indexOf("imageUrl"));
            url = url + "&imageUrl=" + PATH_TO_UPLOADS + fu.url + "&imageId=" + fu.uuid;
        }
        redirectTo(url);
    }

    private static FileUpload createFile(String item, String temp, File attachment, String contentType, String size, String name) throws IOException
    {
        final String uuid = RandomUtil.getUUID();
        long fileSize = attachment.length();

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        String folder = cal.get(Calendar.YEAR) + "" +
                ((cal.get(Calendar.MONTH) + 1) < 10 ? "0" : "") + (cal.get(Calendar.MONTH) + 1) + "" +
                (cal.get(Calendar.DAY_OF_MONTH) < 10 ? "0" : "") + cal.get(Calendar.DAY_OF_MONTH) + "";

        File copyTo = new File(PATH_TO_UPLOADS_FILESYSTEM + folder);
        if (!copyTo.exists())
            copyTo.mkdir();

        String filename = folder + "/" + uuid;
        String filenamePath = folder + "/";
        final File destination = new File(PATH_TO_UPLOADS_FILESYSTEM + filename);
        FileUtils.copyFile(attachment, destination);

        if (contentType.indexOf("image") != -1)
        {
            BufferedImage readImage = ImageIO.read(attachment);
            int h = readImage.getHeight();
            int w = readImage.getWidth();
            if (w > 1200 || h > 1200)
            {
                //System.err.println("resizing big image " + h + " px x " + w + " px");
                readImage = null;

                // resize big images
                File destinationMax = new File(PATH_TO_UPLOADS_FILESYSTEM + filename + "_bck");
                Images.resize(destination, destinationMax, 1170, 1170, true);
                destination.delete();
                destinationMax.renameTo(new File(PATH_TO_UPLOADS_FILESYSTEM + filename));
                fileSize = destinationMax.length();
            }

            File destinationThumb = new File(PATH_TO_UPLOADS_FILESYSTEM + filename + "_thumb");
            Images.resize(destination, destinationThumb, 200, 200, true);
        }

        FileUpload fu = new FileUpload();
        fu.url = filenamePath;
        fu.name = name;
        fu.size = fileSize;
        fu.contentType = contentType;
        fu.uuid = uuid;
        fu.created = new Date();

        // if editing new event set temporary id of the fileupload
        if (temp != null)
            fu.temp = temp;

        // if editing existing event, set the event of the fileupload
        if (item != null)
        {
            fu.objectUuid = item;
            fu.stored = true;
        }
        fu.save();
        return fu;
    }

    public static void deleteFile(String uuid)
    {
        FileUpload fu = FileUpload.find("byUuid", uuid).first();
        if (fu != null)
        {
            File f = new File(PATH_TO_UPLOADS_FILESYSTEM + fu.url);
            if (f.exists())
                f.delete();
            fu.delete();
        }
    }

    public static void deleteTmpFiles()
    {
        File[] listOfFiles = Play.tmpDir.listFiles();
        for (int i = 0; i < listOfFiles.length; i++)
        {
            final File file = listOfFiles[i];
            if (file.isFile())
            {
                file.delete();
            }
        }
    }
}
