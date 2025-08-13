package pro.eng.yui.oss.d2h.db.dao;

import net.dv8tion.jda.api.entities.Member;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pro.eng.yui.oss.d2h.consts.UserAnon;
import pro.eng.yui.oss.d2h.db.field.GuildId;
import pro.eng.yui.oss.d2h.db.field.UserId;
import pro.eng.yui.oss.d2h.db.model.Roles;
import pro.eng.yui.oss.d2h.db.model.Users;

import java.util.List;

@Service
public class AnonStatsDAO {

    private final UsersDAO usersDAO;
    private final RolesDAO rolesDAO;

    @Autowired
    public AnonStatsDAO(UsersDAO usersDAO, RolesDAO rolesDAO) {
        this.usersDAO = usersDAO;
        this.rolesDAO = rolesDAO;
    }

    /**
     * Extract anonymization statistics based on role+user criteria.
     * If any of them has Anon=OPEN, that will be used.
     * If the user is a bot, UserAnon.OPEN will be used regardless of other settings.
     *
     * @param member The Discord member
     * @return UserAnon.OPEN if any of role or user has Anon=OPEN or if user is a bot, otherwise UserAnon.ANONYMOUS
     */
    public UserAnon extractAnonStats(Member member) {
        // If user is a bot, always use OPEN anonymization
        if (member.getUser().isBot()) {
            return UserAnon.OPEN;
        }
        
        GuildId guildId = new GuildId(member.getGuild());
        UserId userId = new UserId(member.getUser());

        // Users
        boolean isAnon = true;
        try {
            Users user = usersDAO.select(guildId, userId);
            if (user.getAnonStats() != null && user.getAnonStats().get().isOpen()) {
                isAnon = false;
            }
        } catch (Exception ignore) { }

        // Roles
        if (isAnon) {
            try {
                List<Roles> userRoles = rolesDAO.selectUserRole(member);
                for (Roles role : userRoles) {
                    if (role.getAnonStats() != null && role.getAnonStats().get().isOpen()) {
                        isAnon = false;
                        break;
                    }
                }
            } catch (Exception ignore) { }
        }

        return UserAnon.get(isAnon);
    }
}