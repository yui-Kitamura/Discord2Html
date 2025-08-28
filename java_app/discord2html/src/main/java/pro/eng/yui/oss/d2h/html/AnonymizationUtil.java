package pro.eng.yui.oss.d2h.html;

import pro.eng.yui.oss.d2h.db.field.UserId;
import pro.eng.yui.oss.d2h.db.model.Users;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Utility class for anonymizing user information in HTML output.
 * Generates anonymous usernames and colored circle avatars based on UUIDs.
 */
public class AnonymizationUtil {

    // Global (legacy) cache without scoping
    private static final Map<UserId, String> userIdToAnonId = new HashMap<>();
    private static final Map<UserId, String> userIdToAnonAvatar = new HashMap<>();
    // Scoped caches: key = scopeKey (e.g., guild-date-cycle), value = per-user mapping
    private static final Map<String, Map<UserId, String>> scopedAnonId = new HashMap<>();
    private static final Map<String, Map<UserId, String>> scopedAnonAvatar = new HashMap<>();

    /**
     * Backward compatible anonymization without scope.
     */
    public static MessageUserInfo anonymizeUser(Users user) {
        if (user.getAnonStats() != null && user.getAnonStats().get().isAnon()) {
            UserId userId = user.getUserId();
            
            String anonId = userIdToAnonId.computeIfAbsent(userId, id -> {
                // Generate a UUID and use the last 12 characters
                String uuid = UUID.randomUUID().toString().replace("-", "");
                return uuid.substring(uuid.length() - 12);
            });
            
            String avatarUrl = userIdToAnonAvatar.computeIfAbsent(userId, id -> {
                String color = generateColorFromId(anonId); // #なし
                // Create a URL to a colored circle SVG
                String svg = 
                        "<svg xmlns='http://www.w3.org/2000/svg' width='44' height='44'>" +
                        "<circle cx='22' cy='22' r='22' fill='%23" + color + "'/>" +
                        "</svg>";
                return "data:image/svg+xml;charset=UTF-8," + svg;
            });

            return new MessageUserInfo(anonId, avatarUrl);
        } else {
            // Non-anonymous: prefer nickname, but fallback to username when nickname is blank
            String nick = (user.getNickname() == null) ? null : user.getNickname().getValue();
            if (nick == null || nick.isBlank()) {
                nick = (user.getUserName() == null) ? "" : user.getUserName().getValue();
            }
            return new MessageUserInfo(
                    nick,
                    user.getAvatar().getImgPath(user.getUserId())
            );
        }
    }

    /**
     * Scoped anonymization to rotate IDs per guild/day/cycle.
     * The mapping remains stable within the same scopeKey and changes when scopeKey changes.
     *
     * @param user The user to anonymize
     * @param scopeKey A key representing anonymization cycle (e.g., guildId-yyyyMMdd-cX-nN)
     * @return Display info
     */
    public static MessageUserInfo anonymizeUser(Users user, String scopeKey) {
        if (user.getAnonStats() != null && user.getAnonStats().get().isAnon()) {
            UserId userId = user.getUserId();

            Map<UserId, String> idMap = scopedAnonId.computeIfAbsent(scopeKey, k -> new HashMap<>());
            Map<UserId, String> avatarMap = scopedAnonAvatar.computeIfAbsent(scopeKey, k -> new HashMap<>());

            String anonId = idMap.computeIfAbsent(userId, id -> {
                String uuid = UUID.randomUUID().toString().replace("-", "");
                return uuid.substring(uuid.length() - 12);
            });

            String avatarUrl = avatarMap.computeIfAbsent(userId, id -> {
                String color = generateColorFromId(anonId);
                String svg = 
                        "<svg xmlns='http://www.w3.org/2000/svg' width='44' height='44'>" +
                        "<circle cx='22' cy='22' r='22' fill='%23" + color + "'/>" +
                        "</svg>";
                return "data:image/svg+xml;charset=UTF-8," + svg;
            });
            
            return new MessageUserInfo(anonId, avatarUrl);
        } else {
            // User is not anonymous: prefer nickname; fallback to username when nickname is blank
            String nick = (user.getNickname() == null) ? null : user.getNickname().getValue();
            if (nick == null || nick.isBlank()) {
                nick = (user.getUserName() == null) ? "" : user.getUserName().getValue();
            }
            return new MessageUserInfo(
                nick,
                user.getAvatar().getImgPath(user.getUserId())
            );
        }
    }
    
    /**
     * Generates an RGB color from the given ID string.
     */
    private static String generateColorFromId(String id) {
        String colorHex = id.substring(0, Math.min(6, id.length()));
        while (colorHex.length() < 6) {
            colorHex += "0";
        }
        return colorHex;
    }
    
    /**
     * Clears the anonymization cache.
     */
    public static void clearCache() {
        userIdToAnonId.clear();
        userIdToAnonAvatar.clear();
        scopedAnonId.clear();
        scopedAnonAvatar.clear();
    }
}