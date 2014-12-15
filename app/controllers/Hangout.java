package controllers;

import models.Event;
import models.User;

public class Hangout extends BaseController
{
    //@Before(only = { "createRoom" })
    static void checkAccess()
    {
        checkAuthorizedAccess();
    }

    public static void room(String id, String transactionId, String tempName) throws Throwable
    {
        final User user = getLoggedUser();
        final Event event = Event.get(id);
        final Event e = event;

        if (user == null && tempName == null)
            joinRoom(id, request.url);

        if (!e.isFree())
            checkPayment(e, request.url);

        final String name = user != null ? user.getFullName() : tempName;
        final String room = id;
        final String socketIo = getProperty(CONFIG_SOCKET_IO);
        //final String socketIo = "https://192.168.1.100:10002";
        render(user, name, room, socketIo, event, e);
    }

    public static void joinRoom(String id, String url)
    {
        render(id, url);
    }

    public static void joinRoomPost(String name, String id, String url)
    {
        redirectTo(url + "&tempName=" + name);
    }

    public static void createRoom()
    {
        User user = getLoggedUser();
        render(user);
    }

}