package pro.eng.yui.oss.d2h.db.model;

import pro.eng.yui.oss.d2h.consts.StringConsts;
import pro.eng.yui.oss.d2h.db.field.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Guilds {
    
    /** DiscordサーバID */
    private GuildId guild_id;
    public void setGuildId(GuildId newValue){
        this.guild_id = newValue;
    }
    public GuildId getGuildId(){
        return guild_id;
    }
    
    /** Discordサーバ名 */
    private GuildName guild_name;
    public void setGuildName(GuildName newValue){
        this.guild_name = newValue;
    }
    public GuildName getGuildName() {
        return guild_name;
    }

    private JoinedTime joined_time;
    public void setJoinedTime(JoinedTime newValue){
        this.joined_time = newValue;
    }
    public JoinedTime getJoinedTime() {
        return joined_time;
    }
    
    /** 匿名更新サイクル */
    private AnonCycle anon_cycle;
    public void setAnonCycle(AnonCycle newValue){
        this.anon_cycle = newValue;
    }
    public AnonCycle getAnonCycle() {
        return anon_cycle;
    }
    
    private LastAnonChanged last_anon_changed;
    public void setLastAnonChanged(LastAnonChanged newValue){
        this.last_anon_changed = newValue;
    }
    public LastAnonChanged getLastAnonChanged(){
        return last_anon_changed;
    }
    
    public RunsOn runs_on;
    public void setRunsOn(RunsOn newValue) {
        this.runs_on = newValue;
    }
    public RunsOn getRunsOn() {
        return runs_on;
    }
    
    /** 実行時のメッセージ表示設定（"on"/"off"） */
    private OnRunMessage on_run_message;
    public void setOnRunMessage(OnRunMessage newValue) {
        this.on_run_message = newValue;
    }
    public OnRunMessage getOnRunMessage() {
        return on_run_message;
    }
    
    /** 実行後のURL共有設定（"share"/"deny"） */
    private OnRunUrl on_run_url;
    public void setOnRunUrl(OnRunUrl newValue) {
        this.on_run_url = newValue;
    }
    public OnRunUrl getOnRunUrl() {
        return on_run_url;
    }
    
    public List<RunsOn> getRunsOnList(){
        List<RunsOn> result = new ArrayList<>();
        if (runs_on == null || runs_on.getValue() <= 0 || 24 <= runs_on.getValue()) {
            result.add(new RunsOn(0){});
            return result;
        }
        for (int hour = 0; hour < 24; hour += runs_on.getValue()) {
            result.add(new RunsOn(hour));
        }
        result.sort(RunsOn.getListComparator());
        return result;
    }
    
    public Guilds(){
        // nothing to do
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = hash * 31 + (guild_id == null ? 0 : guild_id.hashCode());
        hash = hash * 31 + (guild_name == null ? 0 : guild_name.hashCode());
        hash = hash * 31 + (joined_time == null ? 0 : joined_time.hashCode());
        hash = hash * 31 + (anon_cycle == null ? 0 : anon_cycle.hashCode());
        hash = hash * 31 + (last_anon_changed == null ? 0 : last_anon_changed.hashCode());
        hash = hash * 31 + (runs_on == null ? 0 : runs_on.hashCode());
        hash = hash * 31 + (on_run_message == null ? 0 : on_run_message.hashCode());
        hash = hash * 31 + (on_run_url == null ? 0 : on_run_url.hashCode());
        return hash;
    }
    
    @Override
    public boolean equals(Object obj) {
        if(this == obj){ return true; }
        if(obj == null){ return false; }
        if(!(obj.getClass().equals(this.getClass()))){ return false; }
        Guilds other = (Guilds) obj;
        if(!Objects.equals(guild_id, other.guild_id)){
            return false;
        }
        if(!Objects.equals(guild_name, other.guild_name)){
            return false;
        }
        if(!Objects.equals(joined_time, other.joined_time)){
            return false;
        }
        if(!Objects.equals(anon_cycle, other.anon_cycle)){
            return false;
        }
        if(!Objects.equals(last_anon_changed, other.last_anon_changed)){
            return false;
        }
        if(!Objects.equals(runs_on, other.runs_on)){
            return false;
        }
        if(!Objects.equals(on_run_message, other.on_run_message)){
            return false;
        }
        if(!Objects.equals(on_run_url, other.on_run_url)){
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return StringConsts.gson.toJson(this);
    }

}
