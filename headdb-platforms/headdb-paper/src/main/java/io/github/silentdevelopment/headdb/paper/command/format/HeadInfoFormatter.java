package io.github.silentdevelopment.headdb.paper.command.format;

import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.paper.message.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

public final class HeadInfoFormatter {

    private static final int MAX_DISPLAYED_IDS = 12;

    private HeadInfoFormatter() {}

    public static @NotNull List<Component> format(@NotNull Messages messages, @NotNull CommandSender sender, @NotNull Head head) {
        Objects.requireNonNull(messages, "messages");
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(head, "head");

        return List.of(
                Component.empty(),
                Component.text("> ", NamedTextColor.GRAY).append(Component.text(text(messages, sender, "title", "Head Info"), NamedTextColor.GOLD)),
                line(text(messages, sender, "label.name", "Name"), head.name()),
                line(text(messages, sender, "label.id", "ID"), head.id().toString()),
                line(text(messages, sender, "label.source", "Source"), head.id().source()),
                line(text(messages, sender, "label.category", "Category"), head.category()),
                line(text(messages, sender, "label.tags", "Tags"), join(messages, sender, head.tags())),
                line(text(messages, sender, "label.collections", "Collections"), join(messages, sender, head.collections())),
                Component.text(text(messages, sender, "label.texture", "Texture") + ": ", NamedTextColor.GRAY).append(Component.text(head.texture().hash(), NamedTextColor.GOLD).clickEvent(ClickEvent.copyToClipboard(head.texture().hash())).hoverEvent(HoverEvent.showText(Component.text(text(messages, sender, "copy-hover", "Click to copy."), NamedTextColor.GRAY)))),
                Component.empty()
        );
    }

    private static @NotNull Component line(@NotNull String key, @NotNull Object value) {
        return Component.text(key + ": ", NamedTextColor.GRAY).append(Component.text(String.valueOf(value), NamedTextColor.GOLD));
    }

    private static @NotNull String join(@NotNull Messages messages, @NotNull CommandSender sender, @NotNull Collection<String> values) {
        if (values.isEmpty()) {
            return text(messages, sender, "value.none", "none");
        }

        StringJoiner joiner = new StringJoiner(", ");
        int index = 0;

        for (String value : values) {
            if (index >= MAX_DISPLAYED_IDS) {
                joiner.add(text(messages, sender, "value.overflow", "+{count} more").replace("{count}", String.valueOf(values.size() - MAX_DISPLAYED_IDS)));
                break;
            }

            joiner.add(value);
            index++;
        }

        return joiner.toString();
    }

    private static @NotNull String text(@NotNull Messages messages, @NotNull CommandSender sender, @NotNull String key, @NotNull String fallback) {
        return messages.text(sender, "command.info." + key, fallback);
    }
}
