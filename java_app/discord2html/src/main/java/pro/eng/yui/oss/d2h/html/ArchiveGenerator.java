package pro.eng.yui.oss.d2h.html;

import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import pro.eng.yui.oss.d2h.botIF.DiscordJdaProvider;
import pro.eng.yui.oss.d2h.config.ApplicationConfig;
import pro.eng.yui.oss.d2h.config.Secrets;
import pro.eng.yui.oss.d2h.consts.DateTimeUtil;
import pro.eng.yui.oss.d2h.db.field.GuildId;
import pro.eng.yui.oss.d2h.github.GitConfig;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * Archive generation component: responsible for producing archive HTML pages
 * (daily channel pages and thread pages) and triggering related indices.
 */
@Component
public class ArchiveGenerator {
    private static final String TEMPLATE_NAME = "message";
    private static final String THREAD_TEMPLATE_NAME = "thread_message";

    private final ApplicationConfig appConfig;
    private final GitConfig gitConfig;
    private final TemplateEngine templateEngine;
    private final DiscordJdaProvider jdaProvider;
    private final FileGenerateUtil fileUtil;
    private final IndexGenerator indexGenerator;
    private final String botVersion;

    public ArchiveGenerator(ApplicationConfig appConfig, Secrets secrets, GitConfig gitConfig,
                            TemplateEngine templateEngine,
                            DiscordJdaProvider jdaProvider, FileGenerateUtil fileUtil,
                            IndexGenerator indexGenerator) {
        this.appConfig = appConfig;
        this.gitConfig = gitConfig;
        this.templateEngine = templateEngine;
        this.jdaProvider = jdaProvider;
        this.fileUtil = fileUtil;
        this.indexGenerator = indexGenerator;
        this.botVersion = secrets.getBotVersion();
    }

    /**
     * Generate archive(s) for the given channel and period.
     * Handles both regular channels (daily split) and thread channels (single page).
     */
    public Path generate(GuildId guildId, ChannelInfo channel, List<MessageInfo> messages,
                         Calendar begin, Calendar end, int seq) {
        // If target is a Thread, generate a single archive file (no daily split)
        if (channel.isThread()) {
            return generateThreadArchive(guildId, channel, messages, begin, end, seq);
        }

        // Normalize copies of begin/end to avoid mutating caller's calendars
        Calendar cur = (Calendar) begin.clone();
        Calendar until = (Calendar) end.clone();

        // Track affected date8 values for index regeneration
        Set<String> affectedDate8 = new HashSet<>();

        Path lastOutput = null;
        // Capture a per-execution timestamp directory name so each run creates a new archive folder
        String runTimestamp = DateTimeUtil.folder().format(Calendar.getInstance(DateTimeUtil.JST).getTime());

        while (!cur.after(until)) {
            // Segment end is end of current day (23:59:59) or the global end, whichever is earlier
            Calendar segmentEnd = (Calendar) cur.clone();
            segmentEnd.set(Calendar.HOUR_OF_DAY, 23);
            segmentEnd.set(Calendar.MINUTE, 59);
            segmentEnd.set(Calendar.SECOND, 59);
            segmentEnd.set(Calendar.MILLISECOND, 999);
            if (segmentEnd.after(until)) {
                segmentEnd = (Calendar) until.clone();
            }

            // Filter messages for [cur, segmentEnd]
            List<MessageInfo> segmentMessages = filterMessagesByRange(messages, cur, segmentEnd);

            // Always render the template, even if segmentMessages is empty.
            final String basePrefix = fileUtil.repoBaseWithPrefix();
            Context context = new Context();
            context.setVariable("channel", channel);
            context.setVariable("messages", segmentMessages);
            context.setVariable("begin", DateTimeUtil.time().format(cur.getTime()));
            context.setVariable("end", DateTimeUtil.time().format(segmentEnd.getTime()));
            context.setVariable("sequence", seq);
            context.setVariable("backToChannelHref", basePrefix+ "/archives/" + channel.getChannelId().toString() + ".html");
            context.setVariable("backToTopHref", basePrefix + "/index.html");
            context.setVariable("guildIconUrl", fileUtil.resolveGuildIconUrl(guildId));
            context.setVariable("botVersion", botVersion);
            context.setVariable("basePrefix", basePrefix);
            // Add active thread links for this channel at the top
            try {
                List<FileGenerateUtil.Link> activeThreadLinks = getActiveThreadLinks(channel);
                if (activeThreadLinks.size() > 0) {
                    context.setVariable("activeThreads", activeThreadLinks);
                }
            } catch (Exception ignore) {
                // best-effort; ignore failures
            }

            String htmlContent = templateEngine.process(TEMPLATE_NAME, context);

            Path output = appConfig.getOutputPath().resolve(runTimestamp).resolve(channel.getChannelId().toString() + ".html");
            fileUtil.writeIfChanged(output, htmlContent);

            lastOutput = output;

            // Mark affected date8 for indices only when there were messages
            if (!segmentMessages.isEmpty()) {
                affectedDate8.add(DateTimeUtil.date8().format(segmentEnd.getTime()));
            }

            // Advance to the start of the next day to avoid infinite loop
            Calendar nextDay = (Calendar) cur.clone();
            nextDay.set(Calendar.HOUR_OF_DAY, 0);
            nextDay.set(Calendar.MINUTE, 0);
            nextDay.set(Calendar.SECOND, 0);
            nextDay.set(Calendar.MILLISECOND, 0);
            nextDay.add(Calendar.DAY_OF_MONTH, 1);
            cur = nextDay;
        }

        // If the run ends today and the execution time is not midnight (00:00), ensure today's daily index is regenerated.
        try {
            Calendar nowJst = Calendar.getInstance(DateTimeUtil.JST);
            String today8 = DateTimeUtil.date8().format(nowJst.getTime());
            String until8 = DateTimeUtil.date8().format(until.getTime());
            boolean isMidnight = until.get(Calendar.HOUR_OF_DAY) == 23
                    && until.get(Calendar.MINUTE) == 59 && until.get(Calendar.SECOND) == 59
                    && until.get(Calendar.MILLISECOND) == 999;
            if (today8.equals(until8) && !isMidnight) {
                affectedDate8.add(until8);
            }
        } catch (Exception ignore) {
            // best-effort: do not block generation
        }

        // Update per-day (date8) index for all affected dates
        try {
            for (String d8 : affectedDate8) {
                // Use a calendar set to the date for regenerateDailyIndex
                Calendar any = Calendar.getInstance(DateTimeUtil.JST);
                any.set(Integer.parseInt(d8.substring(0,4)),
                        Integer.parseInt(d8.substring(4,6)) - 1,
                        Integer.parseInt(d8.substring(6,8)),
                        0,0,0);
                any.set(Calendar.MILLISECOND, 0);
                indexGenerator.regenerateDailyIndex(guildId, channel.getChannelId(), any);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to regenerate daily index page(s)", e);
        }

        // After generating archive page(s), update listing pages by prepending new links without scanning directories
        try {
            // Prepend per-channel archive links for each affected date
            for (String d8 : affectedDate8) {
                indexGenerator.prependChannelArchiveEntry(guildId, channel.getChannelId(), d8);
            }
        } catch (IOException e) {
            // Raise for visibility
            throw new RuntimeException("Failed to regenerate archives/index pages", e);
        }

        return lastOutput;
    }

    private Path generateThreadArchive(GuildId guildId, ChannelInfo channel, List<MessageInfo> messages, Calendar begin, Calendar end, int seq) {
        final String basePrefix = fileUtil.repoBaseWithPrefix();
        Context ctx = new Context();
        ctx.setVariable("channel", channel);
        ctx.setVariable("messages", messages);
        ctx.setVariable("begin", DateTimeUtil.time().format(begin.getTime()));
        ctx.setVariable("end", DateTimeUtil.time().format(end.getTime()));
        // Provide machine-readable epoch millis to template
        try {
            ctx.setVariable("beginEpochMillis", begin.getTimeInMillis());
            ctx.setVariable("endEpochMillis", end.getTimeInMillis());
        } catch (Exception ignore) { /* best-effort */ }
        ctx.setVariable("sequence", seq);
        if (channel.getParentChannelName() != null) {
            // Links for thread page navigation
            ctx.setVariable("backToParentThreadsHref", String.format(basePrefix + "/archives/%s/threads/index.html", channel.getParentChannelId().toString()));
            ctx.setVariable("backToParentArchiveHref", String.format(basePrefix + "/archives/%s.html", channel.getParentChannelId().toString()));
        } else {
            ctx.setVariable("backToParentThreadsHref", basePrefix + "/index.html");
            ctx.setVariable("backToParentArchiveHref", basePrefix + "/index.html");
        }
        ctx.setVariable("backToTopHref", basePrefix + "/index.html");
        ctx.setVariable("guildIconUrl", fileUtil.resolveGuildIconUrl(guildId));
        ctx.setVariable("botVersion", botVersion);
        ctx.setVariable("basePrefix", basePrefix);
        // Standardized nav variables
        ctx.setVariable("isThread", true);
        if (channel.getParentChannelId() != null) {
            ctx.setVariable("threadIndexHref", String.format(basePrefix+ "/archives/%s/threads/index.html", channel.getParentChannelId().toString()));
        }
        String html = templateEngine.process(THREAD_TEMPLATE_NAME, ctx);
        Path outPath = appConfig.getOutputPath()
                .resolve("archives")
                .resolve(channel.getParentChannelId() == null ? "unknown" : channel.getParentChannelId().toString())
                .resolve("threads")
                .resolve("t-" + channel.getChannelId().toString() + ".html");
        fileUtil.writeIfChanged(outPath, html);
        try {
            if (channel.getParentChannelId() != null) {
                indexGenerator.regenerateThreadIndex(guildId, channel.getParentChannelId());
                // Also ensure the parent channel's archive list page exists/updated
                indexGenerator.regenerateChannelArchives(guildId, channel.getParentChannelId());
            }
        } catch (IOException ioe) {
            throw new RuntimeException("Failed to regenerate thread index", ioe);
        }
        return outPath;
    }

    private List<MessageInfo> filterMessagesByRange(List<MessageInfo> messages, Calendar start, Calendar end) {
        List<MessageInfo> result = new ArrayList<>();
        for (MessageInfo m : messages) {
            try {
                Date d = DateTimeUtil.time().parse(m.getCreatedTimestamp());
                if (!d.before(start.getTime()) && !d.after(end.getTime())) {
                    result.add(m);
                }
            } catch (Exception ignore) {
                // Skip messages with unparsable timestamps
            }
        }
        return result;
    }

    private List<FileGenerateUtil.Link> getActiveThreadLinks(ChannelInfo channel) {
        try {
            net.dv8tion.jda.api.entities.channel.middleman.GuildChannel raw = jdaProvider.getJda().getChannelById(net.dv8tion.jda.api.entities.channel.middleman.GuildChannel.class, channel.getChannelId().getValue());
            if (!(raw instanceof net.dv8tion.jda.api.entities.channel.attribute.IThreadContainer container)) {
                return List.of();
            }
            List<net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel> threads = container.getThreadChannels();
            List<FileGenerateUtil.Link> links = new ArrayList<>();
            for (net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel t : threads) {
                if (!t.isArchived()) {
                    String href = fileUtil.repoBaseWithPrefix() + "/archives/" + channel.getChannelId() + "/threads/t-" + t.getId() + ".html";
                    String label = t.getName();
                    links.add(new FileGenerateUtil.Link(href, label, null, null, label, t.getId()));
                }
            }
            // sort by id(t-href)
            links.sort(Comparator.comparing(FileGenerateUtil.Link::getHref));
            return links;
        } catch (Throwable e) {
            return List.of();
        }
    }
}
