package controllers;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.imageio.ImageIO;

import models.Comment;
import models.Event;
import models.FileUpload;
import models.Listing;
import models.User;

import org.apache.commons.io.FileUtils;

import play.Logger;
import play.Play;
import play.cache.Cache;
import play.libs.Images;
import utils.NumberUtils;
import utils.RandomUtil;

import com.google.gson.JsonObject;

import dto.FileUploadDTO;

public class FileuploadController extends BaseController
{
    public static final String PATH_TO_UPLOADS_FILESYSTEM = Play.applicationPath + "/public/uploads/";
    public static final String PATH_TO_LISTING_AVATARS = "public/images/avatars/";
    public static final String PATH_TO_IMAGES = "public/images/";
    public static final String PATH_TO_UPLOADS = "public/uploads/";
    public static final Integer FILESIZE = 10485760;
    public static final Integer FILESIZE_LIMIT = 104857600;

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

    public static void uploadFile(String item, String temp, File attachment, String contentType, String size, String name, String type) throws IOException
    {
        long total = getOwnerStorage();
        int fileSize = NumberUtils.parseInt(size);
        if (total + fileSize > FILESIZE_LIMIT)
        {
            Logger.warn("storage limit reached for " + getLoggedUser().login);
            forbidden();
        }

        if (fileSize < FILESIZE)
        {
            FileUpload fu = createFile(item, temp, attachment, contentType, size, name, type);
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
        final File source = new File(PATH_TO_UPLOADS_FILESYSTEM + fu.getUrl());
        final File sourceThumb = new File(PATH_TO_UPLOADS_FILESYSTEM + fu.getUrl() + "_thumb");

        // new id
        fu.uuid = RandomUtil.getUUID();
        fu.url = fu.getUrl() + RandomUtil.getRandomString(5);
        fu.save();

        final File destination = new File(PATH_TO_UPLOADS_FILESYSTEM + fu.getUrl());
        Images.crop(source, destination, x1, y1, x2, y2);

        // source file delete
        source.delete();
        sourceThumb.delete();

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
            // aspect ration 1.8
            File destinationThumb1 = new File(PATH_TO_UPLOADS_FILESYSTEM + fu.getUrl() + "_256x256");
            Images.resize(destination, destinationThumb1, 460, 256, true);
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

    private static FileUpload createFile(String item, String temp, File attachment, String contentType, String size, String name, String type) throws IOException
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

        String filename = folder + "/" + getName(name) + "-" + uuid;

        // for other extensions than images keep extension name
        if (!contentType.contains("image"))
        {
            filename = folder + "/" + getName(name) + "-" + uuid + getExtension(name);
        }

        final File destination = new File(PATH_TO_UPLOADS_FILESYSTEM + filename);
        FileUtils.copyFile(attachment, destination);

        // for images only
        if (contentType.contains("image"))
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
        fu.url = filename;
        fu.name = name;
        fu.size = fileSize;
        fu.contentType = contentType;
        fu.uuid = uuid;
        fu.created = new Date();
        fu.type = type;
        fu.objectUuid = item;

        // if editing new event set temporary id of the fileupload
        if (temp != null)
            fu.temp = temp;

        // set owner of the file
        if ("listing".equals(type))
        {
            Listing l = Listing.get(item);
            fu.owner = l.user;
        }
        else if ("event".equals(type))
        {
            Event e = Event.get(item);
            fu.owner = e.user;
        }
        else
        {
            fu.owner = getLoggedUserNotCache();
        }

        fu.save();
        return fu;
    }

    public static void getOwnerFiles(String ownerId)
    {
        User user = getLoggedUser();
        List<FileUpload> files = FileUpload.getByOwner(user);
        List<FileUploadDTO> filesDto = new ArrayList<FileUploadDTO>();
        for (FileUpload fileUpload : files)
        {
            filesDto.add(FileUploadDTO.convert(fileUpload));
        }
        renderJSON(filesDto);
    }

    public static void deleteOwnerFile(String uuid)
    {
        User user = getLoggedUser();
        FileUpload fu = FileUpload.getByUuid(uuid);
        if (user.equals(fu.owner))
        {
            // delete file from comment
            Comment c = Comment.getByUuid(fu.temp);
            if (c != null)
            {
                for (int i = 0; i < c.files.size(); i++)
                {
                    FileUpload file = c.files.get(i);
                    if (fu.uuid.equals(uuid))
                        c.files.remove(i);
                }
                c.save();
            }
            deleteFile(uuid);
        }
        renderText("ok");
    }

    public static void deleteFile(String uuid)
    {
        FileUpload fu = FileUpload.find("byUuid", uuid).first();
        if (fu != null)
        {
            File f = new File(PATH_TO_UPLOADS_FILESYSTEM + fu.url);
            if (f.exists())
                f.delete();
            f = new File(PATH_TO_UPLOADS_FILESYSTEM + fu.url + "_thumb");
            if (f.exists())
                f.delete();
            f = new File(PATH_TO_UPLOADS_FILESYSTEM + fu.url + "_256x256");
            if (f.exists())
                f.delete();
            f = new File(PATH_TO_UPLOADS_FILESYSTEM + fu.url + "_128x128");
            if (f.exists())
                f.delete();
            f = new File(PATH_TO_UPLOADS_FILESYSTEM + fu.url + "_64x64");
            if (f.exists())
                f.delete();
            f = new File(PATH_TO_UPLOADS_FILESYSTEM + fu.url + "_32x32");
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

    private static long getOwnerStorage()
    {
        User user = getLoggedUser();
        List<FileUpload> files = FileUpload.getByOwner(user);
        long total = 0;
        for (FileUpload fileUpload : files)
        {
            total += fileUpload.size;
        }
        return total;
    }

    private static String getExtension(String filename)
    {
        if (filename.contains("."))
            return filename.substring(filename.lastIndexOf("."));
        return null;
    }

    private static String getName(String filename)
    {
        if (filename.contains("."))
            return filename.substring(0, filename.lastIndexOf("."));
        return null;
    }
}
