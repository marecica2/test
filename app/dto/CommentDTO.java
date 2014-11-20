package dto;

import models.Comment;
import play.db.jpa.Model;
import utils.WikiUtils;

public class CommentDTO extends Model
{
    public String uuid;
    public String result;
    public String avatar;
    public String created;
    public String createdBy;
    public String comment;
    public Boolean isForUser;
    public String type;
    public String url;
    public String fileName;
    public String fileSize;
    public String fileExtension;
    public String filePath;

    public static CommentDTO convert(Comment c)
    {
        CommentDTO cDto = new CommentDTO();
        cDto.created = c.created != null ? c.created.getTime() + "" : "";
        cDto.comment = WikiUtils.parseToHtml(c.comment);
        cDto.uuid = c.uuid;
        cDto.url = c.url;
        cDto.type = c.type;
        if (c.files != null)
        {
            //            cDto.fileName = c.files.name;
            //            cDto.fileExtension = c.files.contentType;
            //            cDto.filePath = c.files.url;
            //            cDto.fileSize = c.files.size + "";
        }
        //        if (c.customer != null)
        //        {
        //            cDto.createdBy = c.customer.getFullName();
        //            cDto.avatar = c.customer.avatarUrlSmall;
        //        }
        if (c.user != null)
        {
            cDto.createdBy = c.user.getFullName();
            cDto.avatar = c.user.avatarUrl;
        }
        return cDto;
    }
}
