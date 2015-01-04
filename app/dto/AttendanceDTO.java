package dto;

import models.Attendance;
import play.db.jpa.Model;

public class AttendanceDTO extends Model
{
    public String uuid;
    public String result;
    public String created;
    public String email;
    public String name;
    public String avatar;
    public Boolean paid;
    public String customer;
    public Boolean isForUser;

    public static AttendanceDTO convert(Attendance a)
    {
        AttendanceDTO aDto = new AttendanceDTO();
        aDto.created = a.created != null ? a.created.getTime() + "" : "";
        aDto.result = a.result;
        aDto.uuid = a.uuid;
        aDto.email = a.email;
        aDto.name = a.name;
        aDto.paid = a.paid;
        aDto.isForUser = a.isForUser;
        if (a.customer != null && (a.isForUser == null || !a.isForUser))
        {
            aDto.customer = a.customer.uuid;
            aDto.avatar = a.customer != null ? a.customer.avatarUrl : "";
        } else if (a.isForUser == false)
        {
            aDto.avatar = "public/images/avatar";
        } else if (a.user != null)
        {
            aDto.customer = a.user.uuid;
            aDto.avatar = a.user.avatarUrl;
        }
        return aDto;
    }
}
