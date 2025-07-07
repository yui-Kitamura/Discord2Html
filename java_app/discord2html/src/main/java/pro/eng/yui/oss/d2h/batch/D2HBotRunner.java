package pro.eng.yui.oss.d2h.batch;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pro.eng.yui.oss.d2h.botIF.runner.RunArchiveRunner;

@Service
@EnableScheduling
public class D2HBotRunner {

    private final RunArchiveRunner archiveRunner;
    
    public D2HBotRunner(RunArchiveRunner archiveRunner) {
        this.archiveRunner = archiveRunner;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void runTask() {
        archiveRunner.run();
    }
}
