package youtube;

import java.io.IOException;
import java.util.List;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.LiveBroadcast;
import com.google.api.services.youtube.model.LiveBroadcastListResponse;
import com.google.common.collect.Lists;

public class LiveStreams
{

    /**
     * Define a global instance of a Youtube object, which will be used
     * to make YouTube Data API requests.
     */
    private static YouTube youtube;

    /**
     * List streams for the user's channel.
     */
    public static String getStream(String user)
    {
        // This OAuth 2.0 access scope allows for read-only access to the
        // authenticated user's account, but not other types of account access.
        List<String> scopes = Lists.newArrayList("https://www.googleapis.com/auth/youtube.readonly");

        try
        {
            Credential credential = Auth.authorize(scopes, "listbroadcasts", user);
            youtube = new YouTube.Builder(Auth.HTTP_TRANSPORT, Auth.JSON_FACTORY, credential)
                    .setApplicationName("youtube-cmdline-listbroadcasts-sample").build();

            YouTube.LiveBroadcasts.List liveBroadcastRequest = youtube.liveBroadcasts().list("id,snippet");
            liveBroadcastRequest.setBroadcastStatus("active");
            LiveBroadcastListResponse returnedListResponse = liveBroadcastRequest.execute();
            List<LiveBroadcast> returnedList = returnedListResponse.getItems();

            for (LiveBroadcast broadcast : returnedList)
            {
                return broadcast.getId();
            }

        } catch (GoogleJsonResponseException e)
        {
            System.err.println("GoogleJsonResponseException code: " + e.getDetails().getCode() + " : "
                    + e.getDetails().getMessage());
            e.printStackTrace();

        } catch (IOException e)
        {
            System.err.println("IOException: " + e.getMessage());
            e.printStackTrace();
        } catch (Throwable t)
        {
            System.err.println("Throwable: " + t.getMessage());
            t.printStackTrace();
        }
        return null;
    }
}