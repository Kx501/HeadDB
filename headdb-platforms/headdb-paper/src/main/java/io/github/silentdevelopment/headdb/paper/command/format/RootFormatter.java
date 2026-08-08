package io.github.silentdevelopment.headdb.paper.command.format;

import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.message.Messages;
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
        Messages messages = plugin.messages();

        List<Component> lines = new ArrayList<>();

        lines.add(Component.empty());
        lines.add(runningLine(messages, sender, description, buildInfo));
        lines.add(Component.empty());
        lines.add(helpLine(messages, sender));

        if (sender.hasPermission(Permissions.ADMIN)) {
            lines.add(Component.empty());
            lines.add(section(text(messages, sender, "section.build", "Build")));
            lines.add(field(text(messages, sender, "field.version", "Version"), buildInfo.version()));
            lines.add(field(text(messages, sender, "field.build", "Build"), buildInfo.displayBuild()));
            lines.add(field(text(messages, sender, "field.channel", "Channel"), buildInfo.channel()));
            lines.add(field(text(messages, sender, "field.run-id", "Run ID"), valueOrUnavailable(messages, sender, buildInfo.runId())));
            lines.add(field(text(messages, sender, "field.commit", "Commit"), valueOrUnavailable(messages, sender, buildInfo.commit())));
            lines.add(field(text(messages, sender, "field.full-commit", "Full commit"), valueOrUnavailable(messages, sender, buildInfo.fullCommit())));
            lines.add(field(text(messages, sender, "field.branch", "Branch"), valueOrUnavailable(messages, sender, buildInfo.branch())));
            lines.add(field(text(messages, sender, "field.timestamp", "Timestamp"), valueOrUnavailable(messages, sender, buildInfo.buildTime())));
        }

        Component buttons = actionButtons(messages, sender);
        if (!buttons.equals(Component.empty())) {
            lines.add(Component.empty());
            lines.add(buttons);
        }

        lines.add(Component.empty());

        return List.copyOf(lines);
    }

    private static @NotNull Component runningLine(
            @NotNull Messages messages,
            @NotNull CommandSender sender,
            @NotNull PluginMeta meta,
            @NotNull BuildInfo buildInfo
    ) {
        String openHover = text(messages, sender, "open-github", "Open on GitHub");

        return Component.text(text(messages, sender, "running", "Running") + " ", NamedTextColor.GRAY)
                .append(link(
                        meta.getName() + " " + buildInfo.version(),
                        HEADDB_URL,
                        openHover,
                        NamedTextColor.GOLD
                ))
                .append(Component.text(" " + text(messages, sender, "by", "by") + " ", NamedTextColor.GRAY))
                .append(link(
                        "SilentDevelopment",
                        SILENT_DEVELOPMENT_URL,
                        openHover,
                        NamedTextColor.GOLD
                ))
                .append(Component.text(" / ", NamedTextColor.DARK_GRAY))
                .append(link(
                        "TheSilentPro",
                        THE_SILENT_PRO_URL,
                        openHover,
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

    private static @NotNull Component helpLine(@NotNull Messages messages, @NotNull CommandSender sender) {
        return Component.text(" > ", NamedTextColor.DARK_GRAY)
                .append(Component.text(text(messages, sender, "help-prefix", "Run") + " ", NamedTextColor.GRAY))
                .append(Component.text("/hdb help", NamedTextColor.GOLD)
                        .clickEvent(ClickEvent.suggestCommand("/hdb help"))
                        .hoverEvent(HoverEvent.showText(Component.text(text(messages, sender, "help-hover", "Click to suggest {command}").replace("{command}", "/hdb help"), NamedTextColor.GRAY))))
                .append(Component.text(" " + text(messages, sender, "help-suffix", "for command information."), NamedTextColor.GRAY));
    }

    private static @NotNull Component actionButtons(@NotNull Messages messages, @NotNull CommandSender sender) {
        List<Component> buttons = new ArrayList<>();

        if (Permissions.has(sender, Permissions.RELOAD)) {
            buttons.add(button(
                    text(messages, sender, "button.reload", "RELOAD"),
                    "/hdb reload",
                    text(messages, sender, "button.reload-hover", "Reload config, messages, and runtime.")
            ));
        }

        if (Permissions.has(sender, Permissions.VERIFY)) {
            buttons.add(button(
                    text(messages, sender, "button.verify", "VERIFY"),
                    "/hdb verify",
                    text(messages, sender, "button.verify-hover", "Verify the remote database without replacing the active database.")
            ));
        }

        if (Permissions.has(sender, Permissions.REFRESH)) {
            buttons.add(button(
                    text(messages, sender, "button.refresh", "REFRESH"),
                    "/hdb refresh",
                    text(messages, sender, "button.refresh-hover", "Fetch the latest remote head database.")
            ));
        }

        if (Permissions.has(sender, Permissions.STATUS)) {
            buttons.add(button(
                    text(messages, sender, "button.status", "STATUS"),
                    "/hdb status",
                    text(messages, sender, "button.status-hover", "Show database and refresh status.")
            ));
        }

        if (Permissions.has(sender, Permissions.DEBUG)) {
            buttons.add(button(
                    text(messages, sender, "button.debug", "DEBUG"),
                    "/hdb debug",
                    text(messages, sender, "button.debug-hover", "Show detailed runtime diagnostics.")
            ));
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

    private static @NotNull String text(@NotNull Messages messages, @NotNull CommandSender sender, @NotNull String key, @NotNull String fallback) {
        return messages.text(sender, "command.root." + key, fallback);
    }

    private static @NotNull String valueOrUnavailable(@NotNull Messages messages, @NotNull CommandSender sender, String value) {
        if (value == null || value.isBlank()) {
            return text(messages, sender, "value.unavailable", "unavailable");
        }

        return value;
    }
}
