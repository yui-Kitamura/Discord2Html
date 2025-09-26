package pro.eng.yui.oss.d2h.db.model;

import pro.eng.yui.oss.d2h.db.field.*;

public class Optout {

    private SeqId seq_id;
    public void setSeqId(SeqId v){ this.seq_id = v; }
    public SeqId getSeqId(){ return seq_id; }

    private UserId user_id;
    public void setUserId(UserId v){ this.user_id = v; }
    public UserId getUserId(){ return user_id; }

    private GuildId guild_id;
    public void setGuildId(GuildId v){ this.guild_id = v; }
    public GuildId getGuildId(){ return guild_id; }

    private ChannelId channel_id;
    public void setChannelId(ChannelId v){ this.channel_id = v; }
    public ChannelId getChannelId(){ return channel_id; }

    private OptinTimestamp optin_timestamp;
    public void setOptinTimestamp(OptinTimestamp v){ this.optin_timestamp = v; }
    public OptinTimestamp getOptinTimestamp(){ return optin_timestamp; }
}
