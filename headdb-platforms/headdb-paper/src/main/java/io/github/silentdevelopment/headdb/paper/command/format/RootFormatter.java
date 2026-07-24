package io.github.silentdevelopment.headdb.paper.command.format;

import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.permission.Permissions;
import io.github.silentdevelopment.headdb.paper.runtime.BuildInfo;
import io.papermc.paper.plugin.configuration.PluginMeta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class RootFormatter {

    private static final String HEADDB_URL = "https://github.com/SilentDevelopment/HeadDB";
    private static final String SILENT_DEVELOPMENT_URL = "https://github.com/SilentDevelopment";
    private static final String THE_SILENT_PRO_URL = "https://github.com/TheSilentPro";

    private RootFormatter() {
    }

    public static @NotNull List<Component> format(@NotNull HeadDBPlugin plugin, @NotNull CommandSender sender) {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(sender, "sender");

        PluginMeta description = plugin.getPluginMeta();
        BuildInfo buildInfo = BuildInfo.read(plugin);

        List<Component> lines = new ArrayList<>();

        lines.add(Component.empty());
        lines.add(runningLine(description, buildInfo));
        lines.add(Component.empty());
        lines.add(helpLine());

        if (sender.hasPermission(Permissions.ADMIN)) {
            lines.add(Component.empty());
            lines.add(section("Build"));
            lines.add(field("Version", buildInfo.version()));
            lines.add(field("Build", buildInfo.displayBuild()));
            lines.add(field("Channel", buildInfo.channel()));
            lines.add(field("Run ID", valueOrUnavailable(buildInfo.runId())));
            lines.add(field("Commit", valueOrUnavailable(buildInfo.commit())));
            lines.add(field("Full commit", valueOrUnavailable(buildInfo.fullCommit())));
            lines.add(field("Branch", valueOrUnavailable(buildInfo.branch())));
            lines.add(field("Timestamp", valueOrUnavailable(buildInfo.buildTime())));
        }

        Component buttons = actionButtons(sender);
        if (!buttons.equals(Component.empty())) {
            lines.add(Component.empty());
            lines.add(buttons);
        }

        lines.add(Component.empty());

        return List.copyOf(lines);
    }

    private static @NotNull Component runningLine(
            @NotNull PluginMeta meta,
            @NotNull BuildInfo buildInfo
    ) {
        return Component.text("Running ", NamedTextColor.GRAY)
                .append(link(
                        meta.getName() + " " + buildInfo.version(),
                        HEADDB_URL,
                        "Open on GitHub",
                        NamedTextColor.GOLD
                ))
                .append(Component.text(" by ", NamedTextColor.GRAY))
                .append(link(
                        "SilentDevelopment",
                        SILENT_DEVELOPMENT_URL,
                        "Open on GitHub",
                        NamedTextColor.GOLD
                ))
                .append(Component.text(" / ", NamedTextColor.DARK_GRAY))
                .append(link(
                        "TheSilentPro",
                        THE_SILENT_PRO_URL,
                        "Open on GitHub",
                        NamedTextColor.GOLD
                ));
    }

    private static @NotNull Component section(@NotNull String title) {
        return Component.text(" > ", NamedTextColor.DARK_GRAY)
                .append(Component.text(title, NamedTextColor.GOLD));
    }

    private static @NotNull Component field(@NotNull String key, @NotNull String value) {
        return Component.text("      ")
                .append(Component.text(key + ": ", NamedTextColor.GRAY))
                .append(Component.text(value, NamedTextColor.GOLD));
    }

    private static @NotNull Component helpLine() {
        return Component.text(" > ", NamedTextColor.DARK_GRAY)
                .append(Component.text("Run ", NamedTextColor.GRAY))
                .append(Component.text("/hdb help", NamedTextColor.GOLD)
                        .clickEvent(ClickEvent.suggestCommand("/hdb help"))
                        .hoverEvent(HoverEvent.showText(Component.text("Click to suggest /hdb help", NamedTextColor.GRAY))))
                .append(Component.text(" for command information.", NamedTextColor.GRAY));
    }

    private static @NotNull Component actionButtons(@NotNull CommandSender sender) {
        List<Component> buttons = new ArrayList<>();

        if (Permissions.has(sender, Permissions.RELOAD)) {
            buttons.add(button("RELOAD", "/hdb reload", "Reload config, messages, and runtime."));
        }

        if (Permissions.has(sender, Permissions.VERIFY)) {
            buttons.add(button("VERIFY", "/hdb verify", "Verify the remote database without replacing the active database."));
        }

        if (Permissions.has(sender, Permissions.REFRESH)) {
            buttons.add(button("REFRESH", "/hdb refresh", "Fetch the latest remote head database."));
        }

        if (Permissions.has(sender, Permissions.STATUS)) {
            buttons.add(button("STATUS", "/hdb status", "Show database and refresh status."));
        }

        if (Permissions.has(sender, Permissions.DEBUG)) {
            buttons.add(button("DEBUG", "/hdb debug", "Show detailed runtime diagnostics."));
        }

        if (buttons.isEmpty()) {
            return Component.empty();
        }

        Component result = buttons.getFirst();

        for (int index = 1; index < buttons.size(); index++) {
            result = result.append(divider())
                    .append(buttons.get(index));
        }

        return result;
    }

    private static @NotNull Component button(
            @NotNull String label,
            @NotNull String command,
            @NotNull String hover
    ) {
        return Component.text("[ ", NamedTextColor.DARK_GRAY)
                .append(Component.text(label, NamedTextColor.GOLD)
                        .clickEvent(ClickEvent.runCommand(command))
                        .hoverEvent(HoverEvent.showText(Component.text(hover, NamedTextColor.GRAY))))
                .append(Component.text(" ]", NamedTextColor.DARK_GRAY));
    }

    private static @NotNull Component divider() {
        return Component.text(" · ", NamedTextColor.DARK_GRAY);
    }

    private static @NotNull Component link(
            @NotNull String text,
            @NotNull String url,
            @NotNull String hover,
            @NotNull NamedTextColor color
    ) {
        return Component.text(text, color)
                .clickEvent(ClickEvent.openUrl(url))
                .hoverEvent(HoverEvent.showText(Component.text(hover, NamedTextColor.GRAY)));
    }

    private static @NotNull String valueOrUnavailable(String value) {
        if (value == null || value.isBlank()) {
            return "unavailable";
        }

        return value;
    }
}
