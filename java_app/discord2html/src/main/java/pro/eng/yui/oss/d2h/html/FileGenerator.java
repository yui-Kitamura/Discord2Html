package pro.eng.yui.oss.d2h.html;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.List;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.Channel;

@Service
public class FileGenerator {

    private static final String TEMPLATE_NAME = "templates/template.html";

    @Value("${d2h.output.path}")
    private String outputPath;

    private final TemplateEngine templateEngine;

    public FileGenerator(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public void generate(
            Channel channel, List<Message> messages, Calendar begin, Calendar end,
            int seq
    ) {
        Context context = new Context();
        context.setVariable("channel", channel);
        context.setVariable("messages", messages);
        context.setVariable("begin", begin);
        context.setVariable("end", end);
        context.setVariable("sequence", seq);

        String htmlContent = templateEngine.process(TEMPLATE_NAME, context);

        try (FileWriter writer = new FileWriter(Path.of(outputPath, channel.getName() + ".html").toString())) {
            writer.write(htmlContent);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate HTML file", e);
        }
    }
}
