package dto;

import models.FileUpload;

public class FileUploadDTO
{
    public String name;
    public String uuid;
    public String url;
    public String contentType;
    public Long size;
    public Long created;

    public static FileUploadDTO convert(FileUpload file)
    {
        FileUploadDTO c = new FileUploadDTO();
        c.name = file.name;
        c.contentType = file.contentType;
        c.size = file.size;
        c.uuid = file.uuid;
        c.url = file.url;
        c.created = file.created.getTime();
        return c;
    }
}
