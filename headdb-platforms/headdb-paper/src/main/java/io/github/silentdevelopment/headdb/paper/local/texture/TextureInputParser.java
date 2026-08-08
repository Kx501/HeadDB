package io.github.silentdevelopment.headdb.paper.local.texture;

import com.destroystokyo.paper.profile.ProfileProperty;
import io.github.silentdevelopment.headdb.model.HeadTexture;
import io.github.silentdevelopment.headdb.paper.message.MessageException;
import io.github.silentdevelopment.headdb.paper.message.MessageKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;

public final class TextureInputParser {

    private static final String TEXTURE_URL_PREFIX = "https://textures.minecraft.net/texture/";

    public @NotNull HeadTexture parse(@NotNull String input) {
        Objects.requireNonNull(input, "input");
        String value = input.trim();

        if (value.isEmpty()) {
            throw new MessageException(MessageKey.COMMAND_ERROR_TEXTURE_EMPTY);
        }

        if (value.startsWith(TEXTURE_URL_PREFIX)) {
            return new HeadTexture(hashFromUrl(value));
        }

        if (value.contains(TEXTURE_URL_PREFIX)) {
            return new HeadTexture(extractHash(value));
        }

        Optional<String> decoded = decodeBase64(value);
        if (decoded.isPresent() && decoded.get().contains(TEXTURE_URL_PREFIX)) {
            return new HeadTexture(extractHash(decoded.get()));
        }

        return new HeadTexture(value);
    }

    public @NotNull HeadTexture fromItem(@NotNull ItemStack item) {
        Objects.requireNonNull(item, "item");

        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof SkullMeta skullMeta)) {
            throw new MessageException(MessageKey.COMMAND_ERROR_TEXTURE_NOT_HEAD);
        }

        if (skullMeta.getPlayerProfile() == null) {
            throw new MessageException(MessageKey.COMMAND_ERROR_TEXTURE_NO_PROFILE);
        }

        for (ProfileProperty property : skullMeta.getPlayerProfile().getProperties()) {
            if (!property.getName().equalsIgnoreCase("textures")) {
                continue;
            }

            return parse(property.getValue());
        }

        throw new MessageException(MessageKey.COMMAND_ERROR_TEXTURE_NO_PROPERTY);
    }

    private static @NotNull Optional<String> decodeBase64(@NotNull String value) {
        try {
            return Optional.of(new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private static @NotNull String extractHash(@NotNull String value) {
        int start = value.indexOf(TEXTURE_URL_PREFIX);
        if (start < 0) {
            throw new MessageException(MessageKey.COMMAND_ERROR_TEXTURE_NO_URL);
        }

        return hashFromUrl(value.substring(start));
    }

    private static @NotNull String hashFromUrl(@NotNull String url) {
        String hash = url.substring(TEXTURE_URL_PREFIX.length());
        int end = hash.length();

        for (int index = 0; index < hash.length(); index++) {
            char character = hash.charAt(index);
            if ((character >= '0' && character <= '9') || (character >= 'a' && character <= 'f') || (character >= 'A' && character <= 'F')) {
                continue;
            }
            end = index;
            break;
        }

        hash = hash.substring(0, end).toLowerCase(java.util.Locale.ROOT);
        if (hash.isBlank()) {
            throw new MessageException(MessageKey.COMMAND_ERROR_TEXTURE_NO_HASH);
        }

        return hash;
    }
}
