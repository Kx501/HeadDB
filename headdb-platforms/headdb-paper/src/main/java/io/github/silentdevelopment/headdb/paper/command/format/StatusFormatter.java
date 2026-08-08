package io.github.silentdevelopment.headdb.paper.command.format;

import io.github.silentdevelopment.headdb.database.DatabaseStats;
import io.github.silentdevelopment.headdb.database.DatabaseStatus;
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.message.Messages;
import io.github.silentdevelopment.headdb.paper.permission.Permissions;
import io.github.silentdevelopment.headdb.paper.runtime.RefreshState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class StatusFormatter {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").withZone(ZoneId.systemDefault());

    private StatusFormatter() {
    }

    public static @NotNull List<Component> format(@NotNull HeadDBPlugin plugin, @NotNull CommandSender sender) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(sender, "sender");

        DatabaseStatus status = plugin.runtime().database().status();
        DatabaseStats remoteStats = plugin.runtime().database().stats();
        RefreshState refresh = plugin.runtime().refreshState();
        Messages messages = plugin.messages();

        int hiddenHeads = plugin.headRegistry().hiddenHeads().size();
        int moreHeads = plugin.headRegistry().customHeads().list().size();
        int overrides = plugin.headRegistry().overrides().list().size();
        int playerHeads = plugin.headRegistry().playerHeads().knownPlayers().size();
        int moreCategories = plugin.customCategories().list().size();

        List<Component> lines = new ArrayList<>();
        lines.add(Component.empty());
        lines.add(Component.text("> ", NamedTextColor.DARK_GRAY).append(Component.text(text(messages, sender, "title", "Status"), NamedTextColor.RED)));
        lines.add(databaseLine(messages, sender, status));
        lines.add(line(text(messages, sender, "label.heads", "Heads"), remoteStats.heads()));
        lines.add(line(text(messages, sender, "label.hidden-heads", "Hidden Heads"), hiddenHeads));
        lines.add(line(text(messages, sender, "label.more-heads", "More Heads"), moreHeads));
        lines.add(line(text(messages, sender, "label.player-heads", "Player Heads"), playerHeads));
        lines.add(line(text(messages, sender, "label.categories", "Categories"), remoteStats.categories()));
        lines.add(line(text(messages, sender, "label.more-categories", "More Categories"), moreCategories));
        lines.add(line(text(messages, sender, "label.tags", "Tags"), remoteStats.tags()));
        lines.add(line(text(messages, sender, "label.collections", "Collections"), remoteStats.collections()));
        lines.add(line(text(messages, sender, "label.revocations", "Revocations"), remoteStats.revocations()));
        lines.add(line(text(messages, sender, "label.overrides", "Overrides"), overrides));
        lines.add(refreshLine(messages, refresh, sender));
        lines.add(lastRefreshLine(messages, sender, refresh));

        String failure = firstPresent(status.lastError(), refresh.lastFailureMessage());
        if (failure != null) {
            lines.add(line(text(messages, sender, "label.last-error", "Last error"), failure));
        }

        addSupportLine(lines, messages, sender);
        lines.add(Component.empty());
        return List.copyOf(lines);
    }

    private static @NotNull Component databaseLine(@NotNull Messages messages, @NotNull CommandSender sender, @NotNull DatabaseStatus status) {
        Component line = Component.text(text(messages, sender, "label.database", "Database") + ": ", NamedTextColor.GRAY).append(Component.text(String.valueOf(status.state()), statusColor(status)));
        String source = normalize(status.source() == null ? null : String.valueOf(status.source()));

        if (source != null) {
            line = line.append(Component.text(" " + text(messages, sender, "from", "from") + " ", NamedTextColor.GRAY)).append(Component.text(source, NamedTextColor.GOLD));
        }

        return line;
    }

    private static @NotNull Component refreshLine(@NotNull Messages messages, @NotNull RefreshState refresh, @NotNull CommandSender sender) {
        String state = refresh.running()
                ? text(messages, sender, "value.running", "running") + " " + refresh.currentOperation()
                : text(messages, sender, "value.idle", "idle");
        Component line = line(text(messages, sender, "label.refresh", "Refresh"), state);

        if (!refresh.running() && Permissions.has(sender, Permissions.REFRESH)) {
            line = line.append(Component.text("  ")).append(refreshButton(messages, sender));
        }

        return line;
    }

    private static @NotNull Component lastRefreshLine(@NotNull Messages messages, @NotNull CommandSender sender, @NotNull RefreshState refresh) {
        String label = text(messages, sender, "label.last-refresh", "Last Refresh");

        if (refresh.lastOutcome() == RefreshState.RefreshOutcome.SUCCESS) {
            return line(label, refresh.lastOperation() + " " + text(messages, sender, "value.completed-at", "completed at") + " " + formatInstant(messages, sender, refresh.lastSuccessfulRefresh()));
        }

        if (refresh.lastOutcome() == RefreshState.RefreshOutcome.FAILURE) {
            return line(label, refresh.lastOperation() + " " + text(messages, sender, "value.failed-at", "failed at") + " " + formatInstant(messages, sender, refresh.lastFailedRefresh()));
        }

        return line(label, text(messages, sender, "value.never", "never"));
    }

    private static @NotNull Component refreshButton(@NotNull Messages messages, @NotNull CommandSender sender) {
        return Component.text("[ ", NamedTextColor.DARK_GRAY).append(Component.text(text(messages, sender, "button.refresh", "REFRESH"), NamedTextColor.GOLD).clickEvent(ClickEvent.runCommand("/hdb refresh")).hoverEvent(HoverEvent.showText(Component.text(text(messages, sender, "button.refresh-hover", "Click to refresh the database."), NamedTextColor.GRAY)))).append(Component.text(" ]", NamedTextColor.DARK_GRAY));
    }

    private static void addSupportLine(@NotNull List<Component> lines, @NotNull Messages messages, @NotNull CommandSender sender) {
        boolean canDebug = Permissions.has(sender, Permissions.DEBUG);
        boolean canReport = Permissions.has(sender, Permissions.REPORT);

        if (!canDebug && !canReport) {
            return;
        }

        Component line = Component.text(text(messages, sender, "label.support", "Support") + ": ", NamedTextColor.GRAY);

        if (canDebug) {
            line = line.append(Component.text("/hdb debug", NamedTextColor.GOLD));
        }

        if (canDebug && canReport) {
            line = line.append(Component.text(" | ", NamedTextColor.DARK_GRAY));
        }

        if (canReport) {
            line = line.append(Component.text("/hdb report", NamedTextColor.GOLD));
        }

        lines.add(line);
    }

    private static @NotNull NamedTextColor statusColor(@NotNull DatabaseStatus status) {
        String state = String.valueOf(status.state());

        if ("LOADED".equalsIgnoreCase(state)) {
            return NamedTextColor.GOLD;
        }

        if ("LOADING".equalsIgnoreCase(state)) {
            return NamedTextColor.YELLOW;
        }

        return NamedTextColor.RED;
    }

    private static @NotNull Component line(@NotNull String key, @Nullable Object value) {
        return Component.text(key + ": ", NamedTextColor.GRAY).append(Component.text(String.valueOf(value), NamedTextColor.GOLD));
    }

    private static @NotNull String formatInstant(@NotNull Messages messages, @NotNull CommandSender sender, @Nullable Instant instant) {
        if (instant == null) {
            return text(messages, sender, "value.never", "never");
        }

        return TIME_FORMAT.format(instant);
    }

    private static @NotNull String text(@NotNull Messages messages, @NotNull CommandSender sender, @NotNull String key, @NotNull String fallback) {
        return messages.text(sender, "command.status." + key, fallback);
    }

    private static @Nullable String firstPresent(@Nullable String first, @Nullable String second) {
        String normalizedFirst = normalize(first);

        if (normalizedFirst != null) {
            return normalizedFirst;
        }

        return normalize(second);
    }

    private static @Nullable String normalize(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

}
