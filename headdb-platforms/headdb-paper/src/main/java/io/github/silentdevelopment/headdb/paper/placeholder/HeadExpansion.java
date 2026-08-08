package io.github.silentdevelopment.headdb.paper.placeholder;

import io.github.silentdevelopment.headdb.model.Head;
import io.github.silentdevelopment.headdb.model.HeadId;
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.local.texture.TextureValues;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

/**
 * Resolves {@code %headdb_<id>%} to the base64 texture value of a head, so that menu plugins can
 * render it through their own base64 head syntax.
 */
public final class HeadExpansion extends PlaceholderExpansion {

    private final HeadDBPlugin plugin;

    public HeadExpansion(@NotNull HeadDBPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public @NotNull String getIdentifier() {
        return "headdb";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getPluginMeta().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        return texture(params);
    }

    private @NotNull String texture(@NotNull String params) {
        Optional<HeadId> id = parse(params);

        if (id.isEmpty()) {
            return "";
        }

        try {
            return plugin.headRegistry().find(id.get())
                    .map(Head::texture)
                    .map(texture -> TextureValues.encode(texture.hash()))
                    .orElse("");
        } catch (IllegalStateException exception) {
            return "";
        }
    }

    private static @NotNull Optional<HeadId> parse(@NotNull String params) {
        String value = params.trim();

        if (value.isEmpty()) {
            return Optional.empty();
        }

        try {
            HeadId id = value.contains(":") ? new HeadId(value) : HeadId.remote(value);
            return id.isPlayer() ? Optional.empty() : Optional.of(id);
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

}
