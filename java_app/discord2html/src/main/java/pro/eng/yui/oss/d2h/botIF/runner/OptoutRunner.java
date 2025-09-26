package pro.eng.yui.oss.d2h.botIF.runner;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.eng.yui.oss.d2h.db.dao.OptoutDAO;
import pro.eng.yui.oss.d2h.db.field.*;
import pro.eng.yui.oss.d2h.db.model.Optout;

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

    private String afterMessage = "";

    @Autowired
    public OptoutRunner(OptoutDAO optoutDAO) {
        this.optoutDAO = optoutDAO;
    }

    public void run(Member member, List<OptionMapping> options) {
        // parse options
        OptionMapping chOpt = get(options, "channel");
        OptionMapping optinOpt = get(options, "opt-in");
        if (optinOpt == null) {
            afterMessage = "opt-in option is required (True/False)";
            return;
        }
        boolean optIn = optinOpt.getAsBoolean();

        UserId userId = new UserId(member.getUser());
        GuildId guildId = new GuildId(member.getGuild());
        ChannelId channelId = null;
        String scopeLabel;
        if (chOpt != null) {
            GuildChannel ch = chOpt.getAsChannel();
            channelId = new ChannelId(ch.getIdLong());
            scopeLabel = "channel:#" + ch.getName();
        } else {
            scopeLabel = "this guild (all channels)";
        }

        if (optIn) {
            // clear opt-out by setting optin timestamp
            optoutDAO.optin(userId, guildId, channelId, null);
            afterMessage = "Opt-in recorded for " + scopeLabel + ". Thank you.";
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
            afterMessage = "Opt-out recorded for " + scopeLabel + ".";
        }
    }

    @Override
    public String afterRunMessage() {
        return afterMessage == null || afterMessage.isBlank() ? "Done." : afterMessage;
    }

    @Override
    public boolean shouldDeferEphemeral() {
        // privacy-related command -> make ephemeral
        return true;
    }
}
