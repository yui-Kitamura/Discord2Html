package pro.eng.yui.oss.d2h.html;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

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

    @Value("${d2h.output.path}")
    private String outputPath;

    private final SimpleDateFormat timeFormat;
    private final TemplateEngine templateEngine;

    public FileGenerator(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
        this.timeFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        this.timeFormat.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
    }

    public Path generate(
            ChannelInfo channel, List<MessageInfo> messages, Calendar begin, Calendar end,
            int seq
    ) {
        Context context = new Context();
        context.setVariable("channel", channel);
        context.setVariable("messages", messages);
        context.setVariable("begin", timeFormat.format(begin.getTime()));
        context.setVariable("end", timeFormat.format(end.getTime()));
        context.setVariable("sequence", seq);

        String htmlContent = templateEngine.process(TEMPLATE_NAME, context);

        Path output = Path.of(outputPath, 
                new SimpleDateFormat("yyyyMMddHHmmss").format(end.getTime()),
                channel.getName()+ ".html"
        );
        try {
            Files.createDirectories(output.getParent());
            output.toFile().createNewFile();
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
