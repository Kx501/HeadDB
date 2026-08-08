package io.github.silentdevelopment.headdb.paper.local.player;

import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.model.HeadId;
import io.github.silentdevelopment.headdb.model.HeadTexture;
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.message.MessageException;
import io.github.silentdevelopment.headdb.paper.message.MessageKey;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class BukkitPlayerHeadService implements PlayerHeadService {

    private static final String PLAYER_CATEGORY = "players";
    private static final HeadTexture FALLBACK_TEXTURE = new HeadTexture("0");

    private final HeadDBPlugin plugin;
    private final PlayerHeadCache cache;
    private final Duration successTtl;
    private final Duration failedTtl;
    private final boolean externalLookup;

    public BukkitPlayerHeadService(@NotNull HeadDBPlugin plugin, @NotNull PlayerHeadCache cache, @NotNull Duration successTtl, @NotNull Duration failedTtl, boolean externalLookup) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.cache = Objects.requireNonNull(cache, "cache");
        this.successTtl = Objects.requireNonNull(successTtl, "successTtl");
        this.failedTtl = Objects.requireNonNull(failedTtl, "failedTtl");
        this.externalLookup = externalLookup;
    }

    @Override
    public @NotNull CompletableFuture<Head> resolve(@NotNull String nameOrUuid) {
        String lookup = normalizeLookup(nameOrUuid);
        Optional<Head> cached = resolveCached(lookup);
        if (cached.isPresent()) {
            return CompletableFuture.completedFuture(cached.get());
        }

        try {
            return CompletableFuture.completedFuture(resolveNow(lookup));
        } catch (Throwable throwable) {
            return CompletableFuture.failedFuture(throwable);
        }
    }

    @Override
    public @NotNull Optional<Head> resolveCached(@NotNull String nameOrUuid) {
        String lookup = normalizeLookup(nameOrUuid);
        Instant now = Instant.now();

        Optional<CachedPlayerHead> cached = cache.find(lookup);
        if (cached.isPresent() && fresh(cached.get(), now)) {
            return Optional.of(toHead(cached.get(), lookup));
        }

        Optional<PlayerHeadEntry> local = localKnown(lookup);
        if (local.isEmpty()) {
            return Optional.empty();
        }

        CachedPlayerHead entry = new CachedPlayerHead(lookup, local.get().uuid(), local.get().name(), null, null, now, null);
        return Optional.of(toHead(entry, lookup));
    }

    @Override
    public @NotNull Collection<PlayerHeadEntry> knownPlayers() {
        List<PlayerHeadEntry> players = new ArrayList<>();

        for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
            String name = player.getName();
            if (name == null || name.isBlank()) {
                continue;
            }

            players.add(new PlayerHeadEntry(name, player.getUniqueId()));
        }

        players.sort(Comparator.comparing(entry -> entry.name().toLowerCase(Locale.ROOT)));
        return List.copyOf(players);
    }

    @Override
    public @NotNull List<PlayerHeadEntry> searchKnownPlayers(@NotNull String query, int limit) {
        String normalized = query.trim().toLowerCase(Locale.ROOT);
        List<PlayerHeadEntry> matches = new ArrayList<>();

        for (PlayerHeadEntry entry : knownPlayers()) {
            if (!entry.name().toLowerCase(Locale.ROOT).contains(normalized)) {
                continue;
            }

            matches.add(entry);
            if (matches.size() >= limit) {
                break;
            }
        }

        return List.copyOf(matches);
    }

    private @NotNull Head resolveNow(@NotNull String lookup) {
        Optional<Head> cached = resolveCached(lookup);
        if (cached.isPresent()) {
            return cached.get();
        }

        if (!externalLookup && localKnown(lookup).isEmpty()) {
            throw new MessageException(MessageKey.COMMAND_ERROR_UNKNOWN_LOCAL_PLAYER, Map.of("player", lookup));
        }

        OfflinePlayer offlinePlayer = offlinePlayer(lookup);
        String name = offlinePlayer.getName() == null || offlinePlayer.getName().isBlank() ? lookup : offlinePlayer.getName();
        UUID uuid = offlinePlayer.getUniqueId();
        CachedPlayerHead entry = new CachedPlayerHead(lookup, uuid, name, null, null, Instant.now(), null);
        cache.save(entry);
        return toHead(entry, lookup);
    }

    private @NotNull Head toHead(@NotNull CachedPlayerHead entry, @NotNull String fallbackLookup) {
        HeadId id = entry.uuid() == null ? new HeadId("player:" + fallbackLookup) : HeadId.player(entry.uuid());
        String name = entry.name().isBlank() ? fallbackLookup : entry.name();
        HeadTexture texture = entry.textureHash() == null ? FALLBACK_TEXTURE : new HeadTexture(entry.textureHash());
        return new Head(id, name, texture, PLAYER_CATEGORY, Set.of("player"), Set.of());
    }

    private @NotNull Optional<PlayerHeadEntry> localKnown(@NotNull String lookup) {
        for (PlayerHeadEntry entry : knownPlayers()) {
            if (entry.name().equalsIgnoreCase(lookup)) {
                return Optional.of(entry);
            }

            if (entry.uuid() != null && entry.uuid().toString().equalsIgnoreCase(lookup)) {
                return Optional.of(entry);
            }
        }

        return Optional.empty();
    }

    @SuppressWarnings("deprecation")
    private @NotNull OfflinePlayer offlinePlayer(@NotNull String lookup) {
        try {
            return Bukkit.getOfflinePlayer(UUID.fromString(lookup));
        } catch (IllegalArgumentException ignored) {
            return Bukkit.getOfflinePlayer(lookup);
        }
    }

    private boolean fresh(@NotNull CachedPlayerHead entry, @NotNull Instant now) {
        if (entry.failedAt() != null) {
            return now.minus(failedTtl).isBefore(entry.failedAt());
        }

        return now.minus(successTtl).isBefore(entry.resolvedAt());
    }

    private static @NotNull String normalizeLookup(@NotNull String lookup) {
        Objects.requireNonNull(lookup, "lookup");

        String normalized = lookup.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("player:")) {
            normalized = normalized.substring("player:".length());
        }

        if (normalized.isBlank()) {
            throw new MessageException(MessageKey.COMMAND_ERROR_PLAYER_LOOKUP_BLANK);
        }

        return normalized;
    }
}