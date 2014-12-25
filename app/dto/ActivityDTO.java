package dto;

import models.Activity;
import play.db.jpa.Model;

public class ActivityDTO extends Model
{
    public String event;
    public String message;
    public Long created;

    public String user;
    public String userId;
    public String userLogin;
    public String userAvatar;

    public Boolean byCustomer;
    public Boolean forCustomer;
    public String customer;
    public String customerId;
    public String customerAvatar;
    public String customerLogin;
    public String login;

    public static ActivityDTO convert(Activity a)
    {

        ActivityDTO aDto = new ActivityDTO();
        aDto.created = a.created != null ? a.created.getTime() : null;
        aDto.event = a.eventName;
        aDto.login = a.login;

        aDto.user = a.user.getFullName();
        aDto.userId = a.user.uuid;
        aDto.userAvatar = a.user.avatarUrl;
        aDto.userLogin = a.user.login;

        aDto.customer = a.user.getFullName();
        aDto.customerId = a.user.uuid;
        aDto.customerAvatar = a.user.avatarUrl;
        aDto.customerLogin = a.user.login;
        aDto.forCustomer = a.forCustomer;

        if (a.type.indexOf("Cust") >= 0)
            aDto.byCustomer = true;
        if (Activity.ACTIVITY_EVENT_INVITED.equals(a.type))
        {
            if (a.customer != null)
                aDto.message = a.getText(a.customer.login, a.customer.getFullName(), a.event.uuid, a.eventName);
            else
                aDto.message = a.getText(a.login, a.login, a.event.uuid, a.eventName);
        }
        if (Activity.ACTIVITY_EVENT_INVITE_ACCEPTED.equals(a.type))
            aDto.message = a.getText(a.event.uuid, a.eventName);
        if (Activity.ACTIVITY_EVENT_INVITE_DECLINED.equals(a.type))
            aDto.message = a.getText(a.event.uuid, a.eventName);
        if (Activity.ACTIVITY_EVENT_DELETED.equals(a.type))
            aDto.message = a.getText(a.event.uuid, a.eventName);
        if (Activity.ACTIVITY_EVENT_DELETED_CUSTOMER.equals(a.type))
            aDto.message = a.getText(a.event.uuid, a.eventName);
        if (Activity.ACTIVITY_EVENT_CREATED_BY_USER.equals(a.type))
            aDto.message = a.getText(a.event.uuid, a.eventName);
        if (Activity.ACTIVITY_EVENT_PROPOSED_BY_CUSTOMER.equals(a.type))
            aDto.message = a.getText(a.event.uuid, a.eventName);
        if (Activity.ACTIVITY_EVENT_UPDATED_BY_USER.equals(a.type))
            aDto.message = a.getText(a.event.uuid, a.eventName);
        if (Activity.ACTIVITY_EVENT_MOVED.equals(a.type))
            aDto.message = a.getText(a.event.uuid, a.eventName);
        if (Activity.ACTIVITY_EVENT_UPDATED_BY_CUSTOMER.equals(a.type))
            aDto.message = a.getText(a.event.uuid, a.eventName);
        if (Activity.ACTIVITY_EVENT_APPROVED.equals(a.type))
            aDto.message = a.getText(a.event.uuid, a.eventName);
        if (Activity.ACTIVITY_EVENT_DECLINED.equals(a.type))
            aDto.message = a.getText(a.event.uuid, a.eventName);
        if (Activity.ACTIVITY_EVENT_COMMENTED.equals(a.type))
            aDto.message = a.getText(a.event.uuid, a.eventName);
        if (Activity.ACTIVITY_EVENT_COMMENTED_CUSTOMER.equals(a.type))
            aDto.message = a.getText(a.event.uuid, a.eventName);
        if (Activity.ACTIVITY_EVENT_STARTED.equals(a.type))
            aDto.message = a.getText(a.event.uuid, a.eventName);
        return aDto;
    }
}
