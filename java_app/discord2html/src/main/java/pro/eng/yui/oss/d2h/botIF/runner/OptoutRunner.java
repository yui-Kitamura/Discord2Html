package pro.eng.yui.oss.d2h.botIF.runner;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.eng.yui.oss.d2h.db.dao.OptoutDAO;
import pro.eng.yui.oss.d2h.db.field.*;
import pro.eng.yui.oss.d2h.db.model.Optout;

import pro.eng.yui.oss.d2h.botIF.i.MessageKey;
import pro.eng.yui.oss.d2h.botIF.i.MessageKeys;
import pro.eng.yui.oss.d2h.botIF.i.MessageSeed;

import java.awt.*;
import java.util.List;

/**
 * Runner for `/d2h optout` command.
 * Syntax: d2h optout channel:OptionSelectedChannel opt-in:True/False
 * - channel: optional. When omitted => guild-wide setting (channel NULL)
 * - opt-in: required boolean. True = re-consent (opt-in). False = opt-out.
 */
@Component
public class OptoutRunner implements IRunner {

    private final OptoutDAO optoutDAO;

    private MessageKey lastMessageKey;
    private Object[] lastMessageArgs;
    private Color lastMessageColor;
    private boolean lastHasNote;

    @Autowired
    public OptoutRunner(OptoutDAO optoutDAO) {
        this.optoutDAO = optoutDAO;
        this.lastMessageKey = MessageKeys.RUNNER_OPTOUT_SUCCESS;
        this.lastMessageArgs = new Object[0];
        this.lastMessageColor = INFO;
        this.lastHasNote = false;
    }

    @Override
    public RequiredPermissionType requiredPermissionType(List<OptionMapping> options){
        return RequiredPermissionType.ANY;
    }
    
    public void run(Member member, List<OptionMapping> options) {
        // parse options
        OptionMapping chOpt = get(options, "channel");
        OptionMapping optinOpt = get(options, "opt-in");
        if (optinOpt == null) {
            this.lastMessageColor = ERROR;
            this.lastMessageKey = MessageKeys.RUNNER_OPTOUT_ERROR_OPTIN_REQUIRED;
            this.lastMessageArgs = new Object[0];
            return;
        }
        boolean optIn = optinOpt.getAsBoolean();

        UserId userId = new UserId(member.getUser());
        GuildId guildId = new GuildId(member.getGuild());
        ChannelId channelId = null;
        MessageKey scopeKey;
        Object[] scopeArgs;
        if (chOpt != null) {
            GuildChannel ch = chOpt.getAsChannel();
            channelId = new ChannelId(ch.getIdLong());
            scopeKey = MessageKeys.RUNNER_OPTOUT_SCOPE_CHANNEL;
            scopeArgs = new Object[]{ ch.getName() };
        } else {
            scopeKey = MessageKeys.RUNNER_OPTOUT_SCOPE_GUILD;
            scopeArgs = new Object[0];
        }

        this.lastMessageColor = SUCCESS;
        this.lastHasNote = false;
        if (optIn) {
            // clear opt-out by setting optin timestamp
            optoutDAO.optin(userId, guildId, channelId, null);
            this.lastMessageKey = MessageKeys.RUNNER_OPTOUT_OPTIN_SUCCESS;
            
            // look issue#127
            // though out-IN, warn that OUT channels still exclude
            if (channelId == null) {
                List<Optout> allRecs = optoutDAO.selectAllByUserGuild(userId, guildId);
                boolean hasSpecificOptout = allRecs.stream()
                        .anyMatch(r -> r.getChannelId() != null && r.getOptinTimestamp() == null);
                if (hasSpecificOptout) {
                    this.lastHasNote = true;
                }
            }
        } else {
            // set opt-out (insert if not exists; if exists, clear optin timestamp to null)
            Optout rec = new Optout();
            rec.setUserId(userId);
            rec.setGuildId(guildId);
            rec.setChannelId(channelId); // may be null for guild-wide
            rec.setOptinTimestamp(null); // explicit null for opt-out state
            try {
                optoutDAO.insert(rec);
            } catch (Throwable t) {
                // fallback to update in case of duplicate
                try {
                    optoutDAO.update(rec);
                } catch (Throwable ignored) { /* no-op */ }
            }
            this.lastMessageKey = MessageKeys.RUNNER_OPTOUT_OPTOUT_SUCCESS;
        }

        Object note = this.lastHasNote ? MessageKeys.RUNNER_OPTOUT_OPTIN_NOTE : "";
        this.lastMessageArgs = new Object[]{ scopeKey, note };
        if (scopeArgs.length > 0) {
            this.lastMessageArgs = new Object[]{ new MessageSeed(SUCCESS, scopeKey, scopeArgs[0]), note };
        }
    }

    @Override
    public MessageSeed afterRunMessage() {
        return new MessageSeed(lastMessageColor, lastMessageKey, lastMessageArgs);
    }

    @Override
    public boolean shouldDeferEphemeral() {
        // privacy-related command -> make ephemeral
        return true;
    }
}
