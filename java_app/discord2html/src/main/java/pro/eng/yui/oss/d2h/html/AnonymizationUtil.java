package pro.eng.yui.oss.d2h.html;

import pro.eng.yui.oss.d2h.db.field.UserId;
import pro.eng.yui.oss.d2h.db.model.Users;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Utility class for anonymizing user information in HTML output.
 * Generates anonymous usernames and colored circle avatars based on UUIDs.
 */
public class AnonymizationUtil {

    private static final Map<UserId, String> userIdToAnonId = new HashMap<>();
    private static final Map<UserId, URL> userIdToAnonAvatar = new HashMap<>();

    /**
     * Anonymizes a user's information if their AnonStats is set to ANONYMOUS.
     * Generates a consistent anonymous username and avatar for each user within a processing cycle.
     * 
     * @param user The user to anonymize
     * @return A MessageUserInfo object containing either the original or anonymized user information
     */
    public static MessageUserInfo anonymizeUser(Users user) {
        if (user.getAnonStats() != null && user.getAnonStats().get().isAnon()) {
            UserId userId = user.getUserId();
            
            String anonId = userIdToAnonId.computeIfAbsent(userId, id -> {
                // Generate a UUID and use the last 12 characters
                String uuid = UUID.randomUUID().toString().replace("-", "");
                return uuid.substring(uuid.length() - 12);
            });
            
            // Get or generate anonymous avatar URL for this user
            URL avatarUrl = userIdToAnonAvatar.computeIfAbsent(userId, id -> {
                // Generate a color based on the anonymous ID
                String color = generateColorFromId(anonId);
                try {
                    // Create a URL to a colored circle SVG
                    return new URL("data:image/svg+xml," + 
                        "<svg xmlns='http://www.w3.org/2000/svg' width='44' height='44'>" +
                        "<circle cx='22' cy='22' r='22' fill='" + color + "'/>" +
                        "</svg>");
                } catch (MalformedURLException e) {
                    throw new RuntimeException("Failed to create anonymous avatar URL", e);
                }
            });
            
            return new MessageUserInfo(anonId, avatarUrl);
        } else {
            // User is not anonymous, return original information
            return new MessageUserInfo(
                user.getNickname().getValue(),
                user.getAvatar().getImgPath(user.getUserId())
            );
        }
    }
    
    /**
     * Generates an RGB color from the given ID string.
     * 
     * @param id The ID string to generate a color from
     * @return A hex color string (e.g., "#FF0000")
     */
    private static String generateColorFromId(String id) {
        // Use the first 6 characters of the ID as a hex color
        // If ID is shorter than 6 characters, pad with zeros
        String colorHex = id.substring(0, Math.min(6, id.length()));
        while (colorHex.length() < 6) {
            colorHex += "0";
        }
        return "#" + colorHex;
    }
    
    /**
     * Clears the anonymization cache.
     * Call this method when starting a new processing cycle.
     */
    public static void clearCache() {
        userIdToAnonId.clear();
        userIdToAnonAvatar.clear();
    }
}