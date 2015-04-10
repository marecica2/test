package models;

import java.io.File;
import java.util.Date;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;

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

    @ManyToOne
    public User owner;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "comment_upload", joinColumns = @JoinColumn(name = "fileupload_id"), inverseJoinColumns = @JoinColumn(name = "comment_id"))
    public List<Comment> comments;

    public String type;

    public String getUrl()
    {
        return this.url;
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

    public static List<FileUpload> getByOwner(User owner)
    {
        if (owner != null)
        {
            return FileUpload.find("from FileUpload where owner = ? order by created desc", owner).fetch();
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

    @Override
    public String toString()
    {
        return "FileUpload [name=" + name + ", size=" + size + ", objectUuid=" + objectUuid + ", contentType=" + contentType + ", url=" + url + ", created=" + created + ", stored=" + stored
                + ", uuid=" + uuid + ", temp=" + temp + ", owner=" + owner + "]";
    }
}
