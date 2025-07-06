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
    public AnonCycle getAnonCycle(){
        return anon_cycle;
    }        
        
    public RunsOn1 runs_on_1;
    public void setRunsOn1(RunsOn1 newValue) {
        this.runs_on_1 = newValue;
    }
    public RunsOn getRunsOn1() {
        return runs_on_1;
    }

    public RunsOn2 runs_on_2;
    public void setRunsOn2(RunsOn2 newValue) {
        this.runs_on_2 = newValue;
    }
    public RunsOn getRunsOn2() {
        return runs_on_2;
    }

    public RunsOn3 runs_on_3;
    public void setRunsOn3(RunsOn3 newValue) {
        this.runs_on_3 = newValue;
    }
    public RunsOn getRunsOn3() {
        return runs_on_3;
    }

    public RunsOn4 runs_on_4;
    public void setRunsOn4(RunsOn4 newValue) {
        this.runs_on_4 = newValue;
    }
    public RunsOn getRunsOn4() {
        return runs_on_4;
    }
    
    public List<RunsOn> getRunsOn(){
        List<RunsOn> result = new ArrayList<>();
        if(runs_on_1 != null){ result.add(runs_on_1); }
        if(runs_on_2 != null){ result.add(runs_on_2); }
        if(runs_on_3 != null){ result.add(runs_on_3); }
        if(runs_on_4 != null){ result.add(runs_on_4); }
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
        hash = hash * 31 + (runs_on_1 == null ? 0 : runs_on_1.hashCode());
        hash = hash * 31 + (runs_on_2 == null ? 0 : runs_on_2.hashCode());
        hash = hash * 31 + (runs_on_3 == null ? 0 : runs_on_3.hashCode());
        hash = hash * 31 + (runs_on_4 == null ? 0 : runs_on_4.hashCode());
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
        if(!Objects.equals(anon_cycle, other.anon_cycle)) {
            return false;
        }
        if(!Objects.equals(runs_on_1, other.runs_on_1)){
            return false;
        }
        if(!Objects.equals(runs_on_2, other.runs_on_2)){
            return false;
        }
        if(!Objects.equals(runs_on_3, other.runs_on_3)){
            return false;
        }
        if(!Objects.equals(runs_on_4, other.runs_on_4)){
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return StringConsts.gson.toJson(this);
    }

}
