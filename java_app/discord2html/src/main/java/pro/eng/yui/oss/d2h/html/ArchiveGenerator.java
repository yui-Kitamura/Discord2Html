package pro.eng.yui.oss.d2h.html;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.requests.restaction.pagination.PinnedMessagePaginationAction;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import pro.eng.yui.oss.d2h.botIF.DiscordJdaProvider;
import pro.eng.yui.oss.d2h.config.ApplicationConfig;
import pro.eng.yui.oss.d2h.config.Secrets;
import pro.eng.yui.oss.d2h.consts.DateTimeUtil;
import pro.eng.yui.oss.d2h.db.field.GuildId;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * Archive generation component: responsible for producing archive HTML pages
 * (daily channel pages and thread pages) and triggering related indices.
 */
@Component
public class ArchiveGenerator {
    private static final String TEMPLATE_NAME = "daily";
    private static final String THREAD_TEMPLATE_NAME = "thread_message";

    private final ApplicationConfig appConfig;
    private final TemplateEngine templateEngine;
    private final DiscordJdaProvider jdaProvider;
    private final FileGenerateUtil fileUtil;
    private final IndexGenerator indexGenerator;
    private final String botVersion;

    public ArchiveGenerator(ApplicationConfig appConfig, Secrets secrets,
                            TemplateEngine templateEngine,
                            DiscordJdaProvider jdaProvider, FileGenerateUtil fileUtil,
                            IndexGenerator indexGenerator) {
        this.appConfig = appConfig;
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
            Path p = generateThreadArchive(guildId, channel, messages, begin, end, seq);
            try { generateThreadPinnedPage(guildId, channel, messages); } catch (IOException ignore) {}
            return p;
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
            final String date8 = DateTimeUtil.formatDate8(segmentEnd);

            // Filter messages for [cur, segmentEnd]
            List<MessageInfo> segmentMessages = filterMessagesByRange(messages, cur, segmentEnd);

            // Always render the template, even if segmentMessages is empty.
            final String basePrefix = fileUtil.repoBaseWithPrefix();
            Context context = new Context();
            context.setVariable("channelName", channel.getName());
            context.setVariable("humanDate", DateTimeUtil.dateOnly().format(cur.getTime()));
            context.setVariable("endText", DateTimeUtil.full().format(segmentEnd.getTime()));
            context.setVariable("messages", segmentMessages);
            context.setVariable("backToTopHref", basePrefix + "/index.html");
            context.setVariable("backToChannelHref", basePrefix+ "/archives/" + channel.getChannelId().toString() + ".html#d-" + date8);
            context.setVariable("guildIconUrl", fileUtil.resolveGuildIconUrl(guildId));
            context.setVariable("botVersion", botVersion);
            context.setVariable("basePrefix", basePrefix);
            // Add navigation links for previous/next day (mechanically computed)
            try {
                Calendar prevCal = (Calendar) end.clone();
                prevCal.add(Calendar.DAY_OF_MONTH, -1);
                String prevDate8 = DateTimeUtil.date8().format(prevCal.getTime());
                String prevHref = basePrefix + "/archives/" + prevDate8 + "/" + channel.getChannelId().toString() + ".html";
                context.setVariable("prevHref", prevHref);
            } catch (Exception ignore) { /* best-effort */ }
            try {
                Calendar nextCal = (Calendar) end.clone();
                nextCal.add(Calendar.DAY_OF_MONTH, 1);
                String nextDate8 = DateTimeUtil.date8().format(nextCal.getTime());
                String nextHref = basePrefix + "/archives/" + nextDate8 + "/" + channel.getChannelId().toString() + ".html";
                context.setVariable("nextHref", nextHref);
            } catch (Exception ignore) { /* best-effort */ }

            String htmlContent = templateEngine.process(TEMPLATE_NAME, context);

            Path output = appConfig.getOutputPath().resolve("archives").resolve(date8).resolve(channel.getChannelId().toString() + ".html");
            fileUtil.writeIfChanged(output, htmlContent);

            lastOutput = output;

            // Mark affected date8 for indices only when there were messages
            if (!segmentMessages.isEmpty()) {
                affectedDate8.add(date8);
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

        // Generate/refresh pinned messages page for this channel
        try { generateChannelPinnedPage(guildId, channel); } catch (IOException ignore) { }

        return lastOutput;
    }

    private void generateChannelPinnedPage(GuildId guildId, ChannelInfo channel) throws IOException {
        List<PinnedMessagePaginationAction.PinnedMessage> pinnedMessages = jdaProvider.getJda()
                .getChannelById(GuildMessageChannel.class, channel.getChannelId().getValue())
                .retrievePinnedMessages().complete();

        List<MessageInfo> pins = new ArrayList<>();
        for (PinnedMessagePaginationAction.PinnedMessage msg : pinnedMessages) {
            try {
                pins.add(new MessageInfo(msg.getMessage()));
            } catch (Throwable ignore) { }
        }
        final String basePrefix = fileUtil.repoBaseWithPrefix();
        Context ctx = new Context();
        ctx.setVariable("title", channel.getName() + " のピン留め");
        ctx.setVariable("messages", pins);
        ctx.setVariable("guildIconUrl", fileUtil.resolveGuildIconUrl(guildId));
        ctx.setVariable("botVersion", botVersion);
        ctx.setVariable("basePrefix", basePrefix);
        ctx.setVariable("backToTopHref", basePrefix + "/index.html");
        ctx.setVariable("backToChannelArchivesHref", basePrefix + "/archives/" + channel.getChannelId().toString() + ".html");
        ctx.setVariable("lastUpdated", DateTimeUtil.full().format(Calendar.getInstance(DateTimeUtil.JST).getTime()));
        String html = templateEngine.process("pin", ctx);
        Path outDir = appConfig.getOutputPath().resolve("archives").resolve(channel.getChannelId().toString());
        java.nio.file.Files.createDirectories(outDir);
        Path out = outDir.resolve("pin.html");
        fileUtil.writeIfChanged(out, html);
    }

    private void generateThreadPinnedPage(GuildId guildId, ChannelInfo channel, List<MessageInfo> messages) throws IOException {
        // Retrieve pinned messages from the thread using the same approach as channels
        List<PinnedMessagePaginationAction.PinnedMessage> pinnedMessages = jdaProvider.getJda()
                .getThreadChannelById(channel.getChannelId().getValue())
                .retrievePinnedMessages().complete();
        
        List<MessageInfo> pins = new ArrayList<>();
        for (PinnedMessagePaginationAction.PinnedMessage msg : pinnedMessages) {
            try { 
                pins.add(new MessageInfo(msg.getMessage()));
            } catch (Throwable ignore) { }
        }
        final String basePrefix = fileUtil.repoBaseWithPrefix();
        Context ctx = new Context();
        ctx.setVariable("title", (channel.getParentChannelName() != null ? (channel.getParentChannelName() + " / ") : "") + channel.getName() + " のピン留め");
        ctx.setVariable("messages", pins);
        ctx.setVariable("guildIconUrl", fileUtil.resolveGuildIconUrl(guildId));
        ctx.setVariable("botVersion", botVersion);
        ctx.setVariable("basePrefix", basePrefix);
        ctx.setVariable("backToTopHref", basePrefix + "/index.html");
        if (channel.getParentChannelId() != null) {
            ctx.setVariable("backToParentThreadsHref", String.format(basePrefix + "/archives/%s/threads/index.html", channel.getParentChannelId().toString()));
            ctx.setVariable("backToChannelArchivesHref", String.format(basePrefix + "/archives/%s.html", channel.getParentChannelId().toString()));
            ctx.setVariable("backToThreadHref", String.format(basePrefix + "/archives/%s/threads/t-%s.html", channel.getParentChannelId().toString(), channel.getChannelId().toString()));
        }
        ctx.setVariable("lastUpdated", DateTimeUtil.full().format(Calendar.getInstance(DateTimeUtil.JST).getTime()));
        String html = templateEngine.process("pin", ctx);
        Path outDir = appConfig.getOutputPath().resolve("archives")
                .resolve(channel.getParentChannelId() == null ? "unknown" : channel.getParentChannelId().toString())
                .resolve("threads")
                .resolve("t-" + channel.getChannelId().toString());
        java.nio.file.Files.createDirectories(outDir);
        Path out = outDir.resolve("pin.html");
        fileUtil.writeIfChanged(out, html);
    }

    private Path generateThreadArchive(GuildId guildId, ChannelInfo channel, List<MessageInfo> messages, Calendar begin, Calendar end, int seq) {
        final String basePrefix = fileUtil.repoBaseWithPrefix();
        Context ctx = new Context();
        ctx.setVariable("channel", channel);
        ctx.setVariable("messages", messages);
        ctx.setVariable("begin", DateTimeUtil.full().format(begin.getTime()));
        ctx.setVariable("end", DateTimeUtil.full().format(end.getTime()));
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
            // Link to this thread's pin list page
            ctx.setVariable("threadPinListHref", String.format(basePrefix + "/archives/%s/threads/t-%s/pin.html", channel.getParentChannelId().toString(), channel.getChannelId().toString()));
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
                indexGenerator.regenerateChannelIndex(guildId, channel.getParentChannelId());
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
                Date d = DateTimeUtil.full().parse(m.getCreatedTimestamp());
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
