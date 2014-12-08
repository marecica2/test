package models;

import java.io.File;
import java.util.Date;
import java.util.List;

import javax.persistence.Entity;

import play.db.jpa.Model;
import controllers.FileuploadController;

@Entity
public class FileUpload extends Model
{
    public String name;

    public Long size;

    public String objectUuid;

    public String contentType;

    public String url;

    public Date created;

    public Boolean stored;

    public String uuid;

    public String temp;

    public String getUrl()
    {
        return this.url + uuid;
    }

    public static FileUpload getByUuid(String uuid)
    {
        if (uuid != null)
        {
            return FileUpload.find("byUuid", uuid).first();
        }
        return null;
    }

    public static List<FileUpload> getByTemp(String uuid)
    {
        if (uuid != null)
        {
            return FileUpload.find("byTemp", uuid).fetch();
        }
        return null;
    }

    public static List<FileUpload> getByObject(String uuid)
    {
        if (uuid != null)
        {
            return FileUpload.find("byObjectUuid", uuid).fetch();
        }
        return null;
    }

    public static boolean deleteOnDisc(FileUpload file)
    {
        File thumb = new File(FileuploadController.PATH_TO_UPLOADS_FILESYSTEM + file.url + "_thumb");
        if (thumb.exists())
            thumb.delete();

        File f = new File(FileuploadController.PATH_TO_UPLOADS_FILESYSTEM + file.url);
        if (f.exists())
            return f.delete();
        return false;
    }

    public boolean isImage()
    {
        if (this.contentType.contains("image"))
            return true;
        return false;
    }
}
