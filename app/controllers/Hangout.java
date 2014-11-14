package controllers;

import models.Account;
import models.Event;
import models.User;
import play.mvc.Before;
import utils.NetUtils;

public class Hangout extends BaseController
{
    @Before(unless = { "events", "eventNew" })
    static void checkAccess()
    {
        checkAuthorizedAccess();
    }

    public static void room(String id, String transactionId) throws Throwable
    {
        final User user = getLoggedUser();
        final Account account = user.account;
        final Event event = Event.get(id);
        final Event e = event;

        checkPayPalPayment(e, transactionId, request.url);

        final String name = user.getFullName();
        final String room = id;
        String serverIp = NetUtils.getIp();

        if (isProd() || serverIp == null)
            serverIp = getProperty(CONFIG_SERVER_DOMAIN);

        serverIp = "localhost";
        serverIp = "192.168.1.100";
        //serverIp = "192.168.2.81";
        render(user, name, room, serverIp, account, event, e);
    }

    public static void joinRoom(String id)
    {
        User user = getLoggedUser();
        String roomName = id;
        render(user, roomName);
    }

    public static void createRoomView()
    {
        User user = getLoggedUser();
        render(user);
    }

}