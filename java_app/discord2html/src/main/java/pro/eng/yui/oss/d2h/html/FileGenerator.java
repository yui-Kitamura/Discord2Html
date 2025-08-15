package pro.eng.yui.oss.d2h.html;

import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import pro.eng.yui.oss.d2h.config.ApplicationConfig;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

@Service
public class FileGenerator {

    private static final String TEMPLATE_NAME = "template";

    private final SimpleDateFormat timeFormat;
    private final SimpleDateFormat folderFormat;
    private final ApplicationConfig appConfig;
    private final TemplateEngine templateEngine;
    
    public FileGenerator(ApplicationConfig config, TemplateEngine templateEngine) {
        this.appConfig = config;
        this.templateEngine = templateEngine;
        this.timeFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        this.timeFormat.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
        this.folderFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        this.folderFormat.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
    }

    public Path generate(
            ChannelInfo channel, List<MessageInfo> messages, Calendar begin, Calendar end,
            int seq
    ) {
        AnonymizationUtil.clearCache();
        
        Context context = new Context();
        context.setVariable("channel", channel);
        context.setVariable("messages", messages);
        context.setVariable("begin", timeFormat.format(begin.getTime()));
        context.setVariable("end", timeFormat.format(end.getTime()));
        context.setVariable("sequence", seq);

        String htmlContent = templateEngine.process(TEMPLATE_NAME, context);

        Path output = Path.of(
                appConfig.getOutputPath(),
                folderFormat.format(end.getTime()),
                channel.getName()+ ".html"
        );
        try {
            Files.createDirectories(output.getParent());
            if (!Files.exists(output)) {
                output.toFile().createNewFile();
            }
        }catch(IOException ioe) {
            throw new RuntimeException("Failed to generate HTML file", ioe);
        }
        
        try (FileWriter writer = new FileWriter(output.toString())) {
            writer.write(htmlContent);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate HTML file", e);
        }
        
        return output;
    }
}
