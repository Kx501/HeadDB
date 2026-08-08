package io.github.silentdevelopment.headdb.paper.command.format;

import io.github.silentdevelopment.headdb.paper.message.Messages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ListFormatter {

    private ListFormatter() {
        throw new UnsupportedOperationException("This class cannot be instantiated.");
    }

    public static @NotNull List<Component> format(@NotNull Messages messages, @NotNull CommandSender sender, @NotNull String title, @NotNull List<Entry> entries, int page, int pageSize) {
        Objects.requireNonNull(messages, "messages");
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(entries, "entries");

        int totalPages = Math.max(1, (int) Math.ceil((double) entries.size() / (double) pageSize));
        int safePage = Math.max(1, Math.min(page, totalPages));
        int from = Math.min((safePage - 1) * pageSize, entries.size());
        int to = Math.min(from + pageSize, entries.size());

        List<Component> lines = new ArrayList<>();
        lines.add(Component.empty());
        lines.add(Component.text("> ", NamedTextColor.DARK_GRAY).append(Component.text(title, NamedTextColor.RED)).append(Component.text(" [" + safePage + "/" + totalPages + "]", NamedTextColor.GRAY)));

        if (entries.isEmpty()) {
            lines.add(Component.text("  " + messages.text(sender, "command.list.empty", "No entries found."), NamedTextColor.GRAY));
            lines.add(Component.empty());
            return List.copyOf(lines);
        }

        for (Entry entry : entries.subList(from, to)) {
            lines.add(Component.text("  " + entry.id(), NamedTextColor.GOLD).append(Component.text(" - ", NamedTextColor.DARK_GRAY)).append(Component.text(entry.name(), NamedTextColor.GRAY)));
        }

        lines.add(Component.empty());
        return List.copyOf(lines);
    }

    public record Entry(@NotNull String id, @NotNull String name) {

        public Entry {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(name, "name");
        }
    }
}