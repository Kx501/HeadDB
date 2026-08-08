package io.github.silentdevelopment.headdb.paper.command.subcommand;

import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.model.HeadId;
import io.github.silentdevelopment.headdb.model.HeadTexture;
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.command.CommandRequirements;
import io.github.silentdevelopment.headdb.paper.command.Suggestions;
import io.github.silentdevelopment.headdb.paper.local.custom.StoredCustomHead;
import io.github.silentdevelopment.headdb.paper.local.texture.TextureInputParser;
import io.github.silentdevelopment.headdb.paper.message.MessageException;
import io.github.silentdevelopment.headdb.paper.message.MessageKey;
import io.github.silentdevelopment.headdb.paper.permission.Permissions;
import io.github.silentdevelopment.relay.argument.Argument;
import io.github.silentdevelopment.relay.command.Command;
import io.github.silentdevelopment.relay.paper.argument.PaperArgumentTypes;
import io.github.silentdevelopment.relay.paper.command.AbstractPaperCommand;
import io.github.silentdevelopment.relay.paper.command.PaperCommands;
import io.github.silentdevelopment.relay.paper.command.context.PaperCommandContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class CustomCommand extends AbstractPaperCommand {

    private static final int MAX_AMOUNT = 64;
    private static final Argument<String> ACTION = Argument.optional("action", PaperArgumentTypes.STRING);
    private static final Argument<String> FIRST = Argument.optional("id", PaperArgumentTypes.STRING);
    private static final Argument<String> SECOND = Argument.optional("value", PaperArgumentTypes.STRING);
    private static final Argument<String> THIRD = Argument.optional("extra", PaperArgumentTypes.STRING);
    private static final Argument<String> FOURTH = Argument.optional("extra2", PaperArgumentTypes.STRING);

    private final HeadDBPlugin plugin;
    private final TextureInputParser textures;

    public CustomCommand(@NotNull HeadDBPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.textures = new TextureInputParser();
    }

    @Override
    protected void handle(@NotNull PaperCommandContext context) {
        if (!context.has(ACTION)) {
            usage(context);
            return;
        }

        String action = context.get(ACTION).trim().toLowerCase(java.util.Locale.ROOT);

        try {
            switch (action) {
                case "list" -> list(context);
                case "info" -> info(context);
                case "create" -> create(context);
                case "createheld" -> createHeld(context);
                case "delete" -> delete(context);
                case "rename" -> rename(context);
                case "give" -> give(context);
                default -> usage(context);
            }
        } catch (IllegalArgumentException exception) {
            plugin.messages().send(context.sender(), plugin.messages().invalidArgument(context.sender(), exception));
        }
    }

    @Override
    protected @NotNull Command buildCommand() {
        return PaperCommands.literal("custom")
                .description("Manages local custom heads.")
                .requirement(CommandRequirements.permission(Permissions.CUSTOM_LIST))
                .signature(ACTION, FIRST, SECOND, THIRD, FOURTH)
                .suggest(ACTION, context -> List.of("list", "info", "create", "createheld", "delete", "rename", "give"))
                .suggest(FIRST, Suggestions.customHeads(plugin))
                .suggest(THIRD, Suggestions.players())
                .suggest(FOURTH, Suggestions.amounts())
                .noArgs()
                .build();
    }

    private void list(@NotNull PaperCommandContext context) {
        require(context, Permissions.CUSTOM_LIST);
        List<StoredCustomHead> heads = plugin.headRegistry().customHeads().listStored().stream().filter(head -> !head.draft()).sorted(Comparator.comparing(StoredCustomHead::id)).toList();
        int page = context.has(FIRST) ? page(context.get(FIRST)) : 1;
        int from = Math.min((page - 1) * 10, heads.size());
        int to = Math.min(from + 10, heads.size());
        int totalPages = Math.max(1, (int) Math.ceil(heads.size() / 10.0));

        send(context, MessageKey.COMMAND_CUSTOM_LIST_HEADER, Map.of("page", String.valueOf(page), "pages", String.valueOf(totalPages)));
        if (heads.isEmpty()) {
            send(context, MessageKey.COMMAND_CUSTOM_LIST_EMPTY, Map.of());
            return;
        }
        for (StoredCustomHead head : heads.subList(from, to)) {
            plugin.messages().send(context.sender(), Component.text("- ", NamedTextColor.DARK_GRAY).append(Component.text("custom:" + head.id(), NamedTextColor.GOLD)).append(Component.text(" - " + head.name(), NamedTextColor.GRAY)));
        }
    }

    private void info(@NotNull PaperCommandContext context) {
        require(context, Permissions.CUSTOM_INFO);
        StoredCustomHead head = stored(id(context));
        String none = plugin.messages().text(context.sender(), "command.custom.value-none", "none");
        send(context, MessageKey.COMMAND_CUSTOM_INFO_HEADER, Map.of("name", head.name()));
        send(context, MessageKey.COMMAND_CUSTOM_INFO_ID, Map.of("value", "custom:" + head.id()));
        send(context, MessageKey.COMMAND_CUSTOM_INFO_CATEGORY, Map.of("value", head.category()));
        send(context, MessageKey.COMMAND_CUSTOM_INFO_TAGS, Map.of("value", head.tags().isEmpty() ? none : String.join(", ", head.tags())));
        send(context, MessageKey.COMMAND_CUSTOM_INFO_COLLECTIONS, Map.of("value", head.collections().isEmpty() ? none : String.join(", ", head.collections())));
        send(context, MessageKey.COMMAND_CUSTOM_INFO_TEXTURE, Map.of("value", head.textureHash()));
    }

    private void create(@NotNull PaperCommandContext context) {
        require(context, Permissions.CUSTOM_CREATE);
        String id = idRaw(context);
        String textureInput = required(context, SECOND, MessageKey.COMMAND_USAGE_CUSTOM_CREATE);
        String name = context.has(THIRD) ? context.get(THIRD).trim() : displayName(id);
        HeadTexture texture = textures.parse(textureInput);
        UUID createdBy = context.isPlayer() ? context.player().getUniqueId() : null;
        StoredCustomHead head = new StoredCustomHead(id, name, texture.hash(), null, List.of(), Set.of("custom"), Set.of(), "custom", Instant.now(), Instant.now(), createdBy);
        plugin.headRegistry().customHeads().save(head);
        changed();
        send(context, MessageKey.COMMAND_CUSTOM_CREATED, Map.of("id", "custom:" + head.id()));
    }

    private void createHeld(@NotNull PaperCommandContext context) {
        require(context, Permissions.CUSTOM_CREATE);
        if (!context.isPlayer()) {
            throw new MessageException(MessageKey.COMMAND_ERROR_CONSOLE_CREATEHELD);
        }
        String id = idRaw(context);
        String name = context.has(SECOND) ? context.get(SECOND).trim() : displayName(id);
        HeadTexture texture = textures.fromItem(context.player().getInventory().getItemInMainHand());
        StoredCustomHead head = new StoredCustomHead(id, name, texture.hash(), null, List.of(), Set.of("custom"), Set.of(), "custom", Instant.now(), Instant.now(), context.player().getUniqueId());
        plugin.headRegistry().customHeads().save(head);
        changed();
        send(context, MessageKey.COMMAND_CUSTOM_CREATED_HELD, Map.of("id", "custom:" + head.id()));
    }

    private void delete(@NotNull PaperCommandContext context) {
        require(context, Permissions.CUSTOM_DELETE);
        HeadId id = id(context);
        boolean deleted = plugin.headRegistry().customHeads().delete(id);
        changed();
        send(context, deleted ? MessageKey.COMMAND_CUSTOM_DELETED : MessageKey.COMMAND_CUSTOM_DELETE_MISSING, Map.of("id", id.display()));
    }

    private void rename(@NotNull PaperCommandContext context) {
        require(context, Permissions.CUSTOM_RENAME);
        StoredCustomHead head = stored(id(context));
        String name = required(context, SECOND, MessageKey.COMMAND_USAGE_CUSTOM_RENAME);
        plugin.headRegistry().customHeads().save(head.withName(name));
        changed();
        send(context, MessageKey.COMMAND_CUSTOM_RENAMED, Map.of("id", "custom:" + head.id(), "name", name));
    }

    private void give(@NotNull PaperCommandContext context) {
        require(context, Permissions.CUSTOM_GIVE);
        Head head = stored(id(context)).toHead();
        ParsedTarget parsedTarget = parsedGiveTarget(context);
        Player target = target(context, parsedTarget.targetName());
        int amount = parsedTarget.amount();
        if (!Permissions.canCustomGiveTo(context.sender(), target)) {
            send(context, MessageKey.COMMAND_ERROR_NO_PERMISSION, Map.of());
            return;
        }
        if (context.isPlayer() && !plugin.economy().charge(context.player(), head, amount)) {
            return;
        }
        for (int index = 0; index < amount; index++) {
            if (target.getInventory().firstEmpty() == -1) {
                plugin.messages().send(context.sender(), plugin.messages().giveInventoryFull(context.sender(), target));
                return;
            }

            ItemStack item = plugin.itemFactory().create(head);
            if (!target.getInventory().addItem(item).isEmpty()) {
                plugin.messages().send(context.sender(), plugin.messages().giveInventoryFull(context.sender(), target));
                return;
            }
        }
        plugin.messages().send(context.sender(), plugin.messages().giveSuccess(context.sender(), head, target));
    }

    private @NotNull ParsedTarget parsedGiveTarget(@NotNull PaperCommandContext context) {
        if (!context.has(SECOND)) {
            return new ParsedTarget(null, 1);
        }

        String second = context.get(SECOND).trim();
        if (second.isEmpty()) {
            throw new MessageException(MessageKey.COMMAND_USAGE_CUSTOM_GIVE);
        }

        if (context.has(THIRD)) {
            return new ParsedTarget(second, amount(context.get(THIRD)));
        }

        if (isAmount(second)) {
            return new ParsedTarget(null, amount(second));
        }

        return new ParsedTarget(second, 1);
    }

    private @NotNull StoredCustomHead stored(@NotNull HeadId id) {
        return plugin.headRegistry().customHeads().findStored(id).orElseThrow(() -> new MessageException(MessageKey.COMMAND_ERROR_UNKNOWN_CUSTOM_HEAD, Map.of("id", id.toString())));
    }

    private @NotNull HeadId id(@NotNull PaperCommandContext context) {
        return HeadId.custom(idRaw(context));
    }

    private @NotNull String idRaw(@NotNull PaperCommandContext context) {
        return StoredCustomHead.normalizeSlug(required(context, FIRST, MessageKey.COMMAND_ERROR_CUSTOM_ID_REQUIRED));
    }

    private @NotNull String required(@NotNull PaperCommandContext context, @NotNull Argument<String> argument, @NotNull MessageKey key) {
        if (!context.has(argument) || context.get(argument).trim().isEmpty()) {
            throw new MessageException(key);
        }
        return context.get(argument).trim();
    }

    private @Nullable Player target(@NotNull PaperCommandContext context, @Nullable String name) {
        if (name == null || name.isBlank()) {
            if (context.isPlayer()) {
                return context.player();
            }
            throw new MessageException(MessageKey.COMMAND_USAGE_CUSTOM_GIVE_CONSOLE);
        }
        Player player = Bukkit.getPlayerExact(name.trim());
        if (player == null) {
            throw new MessageException(MessageKey.COMMAND_ERROR_PLAYER_NOT_ONLINE, Map.of("player", name));
        }
        return player;
    }

    private int page(@NotNull String raw) {
        int page = Integer.parseInt(raw.trim());
        if (page < 1) {
            throw new MessageException(MessageKey.COMMAND_ERROR_PAGE_MIN);
        }

        return page;
    }

    private boolean isAmount(@NotNull String raw) {
        try {
            amount(raw);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private int amount(@NotNull String raw) {
        int amount;

        try {
            amount = Integer.parseInt(raw.trim());
        } catch (NumberFormatException exception) {
            throw new MessageException(MessageKey.COMMAND_ERROR_AMOUNT_NUMBER);
        }

        if (amount < 1 || amount > MAX_AMOUNT) {
            throw new MessageException(MessageKey.COMMAND_ERROR_AMOUNT_RANGE, Map.of("max", String.valueOf(MAX_AMOUNT)));
        }

        return amount;
    }

    private void require(@NotNull PaperCommandContext context, @NotNull String permission) {
        if (!Permissions.has(context.sender(), permission)) {
            throw new MessageException(MessageKey.COMMAND_ERROR_NO_PERMISSION);
        }
    }

    private void changed() {
        plugin.headRegistry().onLocalMutation();
        plugin.clearSearchCache();
        plugin.clearItemCache();
    }

    private void usage(@NotNull PaperCommandContext context) {
        send(context, MessageKey.COMMAND_USAGE_CUSTOM, Map.of());
    }

    private void send(@NotNull PaperCommandContext context, @NotNull MessageKey key, @NotNull Map<String, String> placeholders) {
        plugin.messages().send(context.sender(), plugin.messages().render(context.sender(), key, placeholders));
    }

    private static @NotNull String displayName(@NotNull String id) {
        String[] parts = id.replace('_', '-').split("-");
        java.util.List<String> words = new java.util.ArrayList<>();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }

            words.add(Character.toUpperCase(part.charAt(0)) + part.substring(1));
        }

        if (words.isEmpty()) {
            return id;
        }

        return String.join(" ", words);
    }

    private record ParsedTarget(@Nullable String targetName, int amount) {
    }
}
