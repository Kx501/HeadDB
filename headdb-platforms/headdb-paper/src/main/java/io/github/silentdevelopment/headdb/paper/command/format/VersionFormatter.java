package io.github.silentdevelopment.headdb.paper.command.format;

import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.message.Messages;
import io.github.silentdevelopment.headdb.paper.runtime.BuildInfo;
import io.github.silentdevelopment.headdb.paper.updater.GitHubRelease;
import io.github.silentdevelopment.headdb.paper.updater.UpdateCheckResult;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class VersionFormatter {

    private VersionFormatter() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static @NotNull Component startup(
            @NotNull HeadDBPlugin plugin,
            @Nullable UpdateCheckResult updateResult
    ) {
        Objects.requireNonNull(plugin, "plugin");

        BuildInfo buildInfo = BuildInfo.read(plugin);
        Messages messages = plugin.messages();
        CommandSender console = Bukkit.getConsoleSender();
        List<Component> lines = new ArrayList<>();

        lines.add(Component.empty());
        lines.add(Component.empty());

        lines.add(Component.empty());
        lines.add(runningLine(messages, console, plugin));
        lines.add(Component.empty());
        lines.add(versionLine(messages, console, buildInfo.version(), updateResult));

        if (plugin.config().isDebug()) {
            lines.add(field(text(messages, console, "field.build", "Build"), buildInfo.displayBuild()));
            lines.add(field(text(messages, console, "field.branch", "Branch"), value(messages, console, buildInfo.branch())));
            lines.add(field(text(messages, console, "field.commit", "Commit"), value(messages, console, buildInfo.commit())));
            lines.add(field(text(messages, console, "field.timestamp", "Timestamp"), value(messages, console, buildInfo.buildTime())));
        }

        lines.add(Component.empty());
        lines.add(Component.empty());

        return joinLines(lines);
    }

    public static @NotNull List<Component> command(@NotNull HeadDBPlugin plugin, @NotNull CommandSender sender) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(sender, "sender");

        BuildInfo buildInfo = BuildInfo.read(plugin);
        UpdateCheckResult updateResult = plugin.updater().lastResult();
        Messages messages = plugin.messages();
        List<Component> lines = new ArrayList<>();

        lines.add(Component.empty());
        lines.add(runningLine(messages, sender, plugin));
        lines.add(versionLine(messages, sender, buildInfo.version(), updateResult));
        lines.add(field(text(messages, sender, "field.build", "Build"), buildInfo.displayBuild()));
        lines.add(field(text(messages, sender, "field.branch", "Branch"), value(messages, sender, buildInfo.branch())));
        lines.add(field(text(messages, sender, "field.commit", "Commit"), value(messages, sender, buildInfo.commit())));
        lines.add(field(text(messages, sender, "field.timestamp", "Timestamp"), value(messages, sender, buildInfo.buildTime())));

        Component actions = updateActions(messages, sender, updateResult);

        if (actions != null) {
            lines.add(Component.empty());
            lines.add(actions);
        }

        lines.add(Component.empty());
        return List.copyOf(lines);
    }

    private static @NotNull Component runningLine(@NotNull Messages messages, @NotNull CommandSender sender, @NotNull HeadDBPlugin plugin) {
        List<String> authors = plugin.getPluginMeta().getAuthors();
        String authorText = authors.isEmpty() ? text(messages, sender, "value.unknown", "Unknown") : String.join(", ", authors);

        return Component.text(text(messages, sender, "running", "Running") + " ", NamedTextColor.GRAY)
                .append(Component.text(plugin.getPluginMeta().getName(), NamedTextColor.RED))
                .append(Component.text(" " + text(messages, sender, "by", "by") + " ", NamedTextColor.GRAY))
                .append(Component.text(authorText, NamedTextColor.GOLD));
    }

    private static @NotNull Component versionLine(
            @NotNull Messages messages,
            @NotNull CommandSender sender,
            @NotNull String version,
            @Nullable UpdateCheckResult updateResult
    ) {
        return Component.text(text(messages, sender, "label.version", "Version") + ": ", NamedTextColor.GRAY)
                .append(Component.text(version, NamedTextColor.GOLD))
                .append(Component.text(" (", NamedTextColor.GRAY))
                .append(status(messages, sender, updateResult))
                .append(Component.text(")", NamedTextColor.GRAY));
    }

    private static @NotNull Component status(@NotNull Messages messages, @NotNull CommandSender sender, @Nullable UpdateCheckResult updateResult) {
        if (updateResult == null) {
            return Component.text(text(messages, sender, "status.not-checked", "Not Checked"), NamedTextColor.DARK_GRAY);
        }

        if (updateResult.failed()) {
            return Component.text(text(messages, sender, "status.check-failed", "Check Failed"), NamedTextColor.RED);
        }

        if (updateResult.updateAvailable()) {
            return Component.text(text(messages, sender, "status.update-available", "Update Available"), NamedTextColor.YELLOW);
        }

        return Component.text(text(messages, sender, "status.latest", "Latest"), NamedTextColor.GOLD);
    }

    private static @NotNull Component field(@NotNull String key, @NotNull String value) {
        return Component.text(key + ": ", NamedTextColor.GRAY).append(Component.text(value, NamedTextColor.GOLD));
    }

    private static @Nullable Component updateActions(@NotNull Messages messages, @NotNull CommandSender sender, @Nullable UpdateCheckResult updateResult) {
        if (updateResult == null || !updateResult.updateAvailable()) {
            return null;
        }

        GitHubRelease release = updateResult.release();

        if (release == null) {
            return null;
        }

        Component open = Component.text("[" + text(messages, sender, "button.open", "OPEN") + "]", NamedTextColor.GOLD)
                .clickEvent(ClickEvent.openUrl(release.htmlUrl()))
                .hoverEvent(HoverEvent.showText(Component.text(text(messages, sender, "button.open-hover", "Open the release page."), NamedTextColor.GRAY)));

        Component update = Component.text("[" + text(messages, sender, "button.update", "UPDATE") + "]", NamedTextColor.GOLD)
                .clickEvent(ClickEvent.runCommand("/hdb update"))
                .hoverEvent(HoverEvent.showText(Component.text(text(messages, sender, "button.update-hover", "Download and install this update."), NamedTextColor.GRAY)));

        return open
                .append(Component.text(" -=- ", NamedTextColor.GRAY))
                .append(update);
    }

    private static @NotNull Component joinLines(@NotNull List<Component> lines) {
        if (lines.isEmpty()) {
            return Component.empty();
        }

        Component result = lines.getFirst();

        for (int index = 1; index < lines.size(); index++) {
            result = result.appendNewline().append(lines.get(index));
        }

        return result;
    }

    private static @NotNull String text(@NotNull Messages messages, @NotNull CommandSender sender, @NotNull String key, @NotNull String fallback) {
        return messages.text(sender, "command.version." + key, fallback);
    }

    private static @NotNull String value(@NotNull Messages messages, @NotNull CommandSender sender, @Nullable String value) {
        if (value == null || value.isBlank()) {
            return text(messages, sender, "value.unavailable", "Unavailable");
        }

        return value;
    }
}
