package pro.eng.yui.oss.d2h.html;

import java.net.URL;

/**
 * Contains user information for display in messages.
 * Used to encapsulate either original or anonymized user data.
 */
public class MessageUserInfo {
    private final String username;
    private final URL avatarUrl;
    
    /**
     * Creates a new MessageUserInfo with the specified username and avatar URL.
     * 
     * @param username The username to display
     * @param avatarUrl The URL of the user's avatar
     */
    public MessageUserInfo(String username, URL avatarUrl) {
        this.username = username;
        this.avatarUrl = avatarUrl;
    }
    
    /**
     * Gets the username.
     * 
     * @return The username
     */
    public String getUsername() {
        return username;
    }
    
    /**
     * Gets the avatar URL.
     * 
     * @return The URL of the user's avatar
     */
    public URL getAvatarUrl() {
        return avatarUrl;
    }
}