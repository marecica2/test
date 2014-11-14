package dto;

import models.User;

public class UserDTO
{
    public String login;
    public String name;
    public String avatarUrl;
    public String avatarUrlSmall;
    public String skype;

    public static UserDTO convert(User customer)
    {
        UserDTO e = new UserDTO();
        e.name = customer.getFullName();
        e.login = customer.login;
        e.skype = customer.skype;
        e.avatarUrl = customer.avatarUrl;
        return e;
    }

}
