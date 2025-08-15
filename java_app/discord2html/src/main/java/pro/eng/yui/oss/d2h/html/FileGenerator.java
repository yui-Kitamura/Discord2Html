package pro.eng.yui.oss.d2h.html;

import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import pro.eng.yui.oss.d2h.config.ApplicationConfig;
import pro.eng.yui.oss.d2h.github.GitUtil;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

@Service
public class FileGenerator {

    private static final String TEMPLATE_NAME = "template";

    private final SimpleDateFormat timeFormat;
    private final SimpleDateFormat folderFormat;
    private final ApplicationConfig appConfig;
    private final TemplateEngine templateEngine;
    private final GitUtil gitUtil;
    
    public FileGenerator(ApplicationConfig config, TemplateEngine templateEngine, GitUtil gitUtil) {
        this.appConfig = config;
        this.templateEngine = templateEngine;
        this.gitUtil = gitUtil;
        this.timeFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        this.timeFormat.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
        this.folderFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        this.folderFormat.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
    }

    public static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    public Path generate(
            ChannelInfo channel, List<MessageInfo> messages, Calendar begin, Calendar end,
            int seq
    ) {
        AnonymizationUtil.clearCache();
        
        // Sync local repo to latest before reading/writing outputs
        try {
            gitUtil.ensureRepoInitialized();
            gitUtil.fetch();
            gitUtil.pullRebase();
        } catch (Exception e) {
            // Non-fatal: continue generation even if git operations fail
            System.out.println("[GitSync] Skip or failed: " + e.getMessage());
        }
        
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
        
        try {
            writeIfChanged(output, htmlContent);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate HTML file", e);
        }
        
        // After generating an archive page, refresh static listings for GitHub Pages
        try {
            regenerateChannelArchives(channel.getName());
            regenerateTopIndex();
        } catch (IOException e) {
            // Do not fail the main generation if index regeneration fails; log via RuntimeException to keep visibility
            throw new RuntimeException("Failed to regenerate archives/index pages", e);
        }
        
        return output;
    }

    private void regenerateChannelArchives(String channelName) throws IOException {
        Path base = Paths.get(appConfig.getOutputPath());
        if (!Files.exists(base)) {
            return;
        }
        // Find all timestamp directories and collect files for this channel
        List<Path> timestampDirs = listTimestampDirs(base);
        // Sort by directory name (timestamp) descending
        timestampDirs.sort(Comparator.comparing((Path p) -> p.getFileName().toString()).reversed());

        List<String> links = new ArrayList<>();
        for (Path tsDir : timestampDirs) {
            Path file = tsDir.resolve(channelName + ".html");
            if (Files.exists(file)) {
                String ts = tsDir.getFileName().toString();
                String href = ".." + "/" + ts + "/" + channelName + ".html";
                String label = channelName + " (" + ts + ")";
                links.add(String.format("<li><a href=\"%s\">%s</a></li>", href, escapeHtml(label)));
            }
        }
        Path archivesDir = base.resolve("archives");
        Files.createDirectories(archivesDir);
        Path channelArchive = archivesDir.resolve(channelName + ".html");
        String page = buildSimpleHtml(channelName + " のアーカイブ一覧", "以下のアーカイブから選択してください:", links);
        writeIfChanged(channelArchive, page);
    }

    private void regenerateTopIndex() throws IOException {
        Path base = Paths.get(appConfig.getOutputPath());
        if (!Files.exists(base)) {
            return;
        }
        Set<String> channelNames = new HashSet<>();
        for (Path tsDir : listTimestampDirs(base)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(tsDir, "*.html")) {
                for (Path p : stream) {
                    String fileName = p.getFileName().toString();
                    if (fileName.toLowerCase().endsWith(".html")) {
                        channelNames.add(fileName.substring(0, fileName.length() - 5));
                    }
                }
            }
        }
        // Build links to archives/channel.html
        List<String> links = channelNames.stream()
                .sorted()
                .map(name -> String.format("<li><a href=\"archives/%s.html\">%s</a></li>", name, escapeHtml(name)))
                .collect(Collectors.toList());
        Path index = base.resolve("index.html");
        String page = buildSimpleHtml("Discord アーカイブ一覧", "チャンネルを選択してください:", links);
        writeIfChanged(index, page);
    }

    private List<Path> listTimestampDirs(Path base) throws IOException {
        if (!Files.exists(base) || !Files.isDirectory(base)) {
            return Collections.emptyList();
        }
        List<Path> result = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(base)) {
            for (Path p : stream) {
                if (Files.isDirectory(p) && p.getFileName().toString().matches("\\d{14}")) {
                    result.add(p);
                }
            }
        }
        return result;
    }

    private void writeIfChanged(Path target, String newContent) throws IOException {
        String existing = null;
        if (Files.exists(target)) {
            existing = Files.readString(target, StandardCharsets.UTF_8);
        }
        if (existing == null || !existing.equals(newContent)) {
            try (FileWriter writer = new FileWriter(target.toString())) {
                writer.write(newContent);
            }
        }
    }

    private String buildSimpleHtml(String title, String description, List<String> listItems) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n");
        sb.append("<html lang=\"ja\">\n");
        sb.append("<head>\n");
        sb.append("  <meta charset=\"UTF-8\">\n");
        sb.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n");
        sb.append("  <title>").append(escapeHtml(title)).append("</title>\n");
        sb.append("  <style>body{font-family:Segoe UI,Meiryo,sans-serif;background:#f8f9fb;color:#222;margin:0;padding:1em} .container{max-width:850px;margin:auto;background:#fff;padding:2em;border-radius:8px;box-shadow:0 2px 6px rgba(0,0,0,.06)} h1{margin-top:0} a{color:#0b6efd;text-decoration:none} a:hover{text-decoration:underline}</style>\n");
        sb.append("</head>\n");
        sb.append("<body>\n");
        sb.append("<div class=\"container\">\n");
        sb.append("  <h1>").append(escapeHtml(title)).append("</h1>\n");
        if (description != null && !description.isEmpty()) {
            sb.append("  <p>").append(escapeHtml(description)).append("</p>\n");
        }
        sb.append("  <ul>\n");
        for (String li : listItems) {
            sb.append("    ").append(li).append("\n");
        }
        sb.append("  </ul>\n");
        sb.append("</div>\n");
        sb.append("</body>\n");
        sb.append("</html>\n");
        return sb.toString();
    }
    
}
