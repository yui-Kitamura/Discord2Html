package pro.eng.yui.oss.d2h.action;

import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pro.eng.yui.oss.d2h.db.dao.ChannelsDAO;
import pro.eng.yui.oss.d2h.db.dao.GuildsDAO;
import pro.eng.yui.oss.d2h.db.dao.UsersDAO;
import pro.eng.yui.oss.d2h.github.GitUtil;

@Service
public class RunArchive {
    
    private final GuildsDAO guildDao;
    private final ChannelsDAO channelDao;
    private final UsersDAO userDao;
    private final GitUtil githubIO;
    
    @Autowired
    public RunArchive(
            GuildsDAO guildsDao, ChannelsDAO channelsDao, UsersDAO usersDao,
            GitUtil githubUtil
    ){
        this.guildDao = guildsDao;
        this.channelDao = channelsDao;
        this.userDao = usersDao;
        this.githubIO = githubUtil;
    }
    
    public void run(GuildMessageChannel channel){
        //対象チャンネルの情報収集
        // guildの周期情報
        // チャンネルの最終処理時刻
        
        
        //DiscordAPIをコールして前回処理時以後のチャット歴を取得
        // ユーザごとの匿名処理判断
        
        //ThymleafによるHTML生成
        
        //GitPush
    }
}
