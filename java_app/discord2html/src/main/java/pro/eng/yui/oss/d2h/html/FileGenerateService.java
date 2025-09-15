package pro.eng.yui.oss.d2h.html;

import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import pro.eng.yui.oss.d2h.botIF.DiscordJdaProvider;
import pro.eng.yui.oss.d2h.config.ApplicationConfig;
import pro.eng.yui.oss.d2h.config.Secrets;
import pro.eng.yui.oss.d2h.db.field.GuildId;
import pro.eng.yui.oss.d2h.github.GitUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Orchestrator service for generating archives. Performs environment setup
 * (git sync, assets, emoji archiving) and delegates archive HTML generation to ArchiveGenerator.
 */
@Service
public class FileGenerateService {
    private final ApplicationConfig appConfig;
    private final TemplateEngine templateEngine;
    private final FileGenerateUtil fileUtil;
    private final GitUtil gitUtil;
    private final ArchiveGenerator archiveGenerator;
    private final IndexGenerator indexGenerator;
    private final String botVersion;

    public FileGenerateService(ApplicationConfig config, Secrets secrets,
                               FileGenerateUtil fileUtil, GitUtil gitUtil,
                               TemplateEngine templateEngine, 
                               ArchiveGenerator archiveGenerator, IndexGenerator indexGenerator) {
        this.appConfig = config;
        this.templateEngine = templateEngine;
        this.fileUtil = fileUtil;
        this.gitUtil = gitUtil;
        this.archiveGenerator = archiveGenerator;
        this.indexGenerator = indexGenerator;
        this.botVersion = secrets.getBotVersion();
    }

    public List<Path> generate(ChannelInfo channel, List<MessageInfo> messages, Calendar begin, Calendar end, int seq) {
        AnonymizationUtil.clearCache();

        // Sync local repo to the latest before reading/writing outputs
        try {
            gitUtil.ensureRepoInitialized();
            gitUtil.fetch();
            gitUtil.pullRebase();
        } catch (Exception e) {
            // Non-fatal: continue generation even if git operations fail
            System.out.println("[GitSync] Skip or failed: " + e.getMessage());
        }

        // Ensure static assets like CSS exist in an output directory
        try {
            ensureStaticAssets();
        } catch (IOException e) {
            throw new RuntimeException("Failed to prepare static assets", e);
        }

        // Archive custom emojis used in messages to gh_pages resources
        try {
            fileUtil.archiveCustomEmojis(appConfig.getOutputPath(), messages);
        } catch (IOException ioe) {
            // non-fatal: continue even if emoji archiving fails
            System.out.println("[EmojiArchive] failed: " + ioe.getMessage());
        }

        // Delegate the core archive generation and also generate related indexes.
        Path mainOut = archiveGenerator.generate(new GuildId(channel), channel, messages, begin, end, seq);
        List<Path> outs = new ArrayList<>();
        if (mainOut != null) outs.add(mainOut);
        // Generate indices that Runner previously handled directly
        try {
            GuildId gid = new GuildId(channel);
            indexGenerator.regenerateTopIndex(gid);
            Path top = appConfig.getOutputPath().resolve("index.html");
            if (!outs.contains(top) && Files.exists(top)){ outs.add(top); }
        } catch (IOException ignore) { }
        return outs;
    }

    public void regenerateHelpPage(GuildId guildId) throws IOException {
        Path base = appConfig.getOutputPath();
        if (!Files.exists(base)) {
            return;
        }
        Path help = base.resolve("help.html");
        Context ctx = new Context();
        ctx.setVariable("guildIconUrl", fileUtil.resolveGuildIconUrl(guildId));
        ctx.setVariable("botVersion", botVersion);
        String page = templateEngine.process("help", ctx);
        fileUtil.writeIfChanged(help, page);
    }

    /**
     * Ensure required static assets (like CSS) are present under the output root for GitHub Pages/local viewing.
     */
    private void ensureStaticAssets() throws IOException {
        Path base = appConfig.getOutputPath();
        if (!Files.exists(base)) {
            Files.createDirectories(base);
        }
        // Copy classpath:/static/css/style.css -> {output}/css/style.css
        Path cssDir = base.resolve("css");
        Files.createDirectories(cssDir);
        Path target = cssDir.resolve("style.css");
        byte[] data = readClasspathResource("/static/css/style.css");
        if (data != null) {
            fileUtil.writeIfChanged(target, new String(data, StandardCharsets.UTF_8));
        }
        // Copy classpath:/static/js/archive-date.js -> {output}/js/archive-date.js
        Path jsDir = base.resolve("js");
        Files.createDirectories(jsDir);
        Path jsTarget = jsDir.resolve("archive-date.js");
        byte[] jsData = readClasspathResource("/static/js/archive-date.js");
        if (jsData != null) {
            fileUtil.writeIfChanged(jsTarget, new String(jsData, StandardCharsets.UTF_8));
        }
        // Copy D2H_logo.png from classpath root to output root for favicon/header in help.html
        Path logoTarget = base.resolve("D2H_logo.png");
        byte[] logo = readClasspathResource("/D2H_logo.png");
        if (logo != null) {
            boolean shouldWrite = true;
            if (Files.exists(logoTarget)) {
                byte[] existing = Files.readAllBytes(logoTarget);
                shouldWrite = (existing.length != logo.length || !java.util.Arrays.equals(existing, logo));
            }
            if (shouldWrite) {
                Files.write(logoTarget, logo);
            }
        }
    }

    private byte[] readClasspathResource(String resourcePath) throws IOException {
        try (var in = FileGenerateService.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                return null;
            }
            return in.readAllBytes();
        }
    }
}
