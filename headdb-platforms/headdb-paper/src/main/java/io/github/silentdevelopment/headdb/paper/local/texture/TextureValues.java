package io.github.silentdevelopment.headdb.paper.local.texture;

import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

public final class TextureValues {

    public static final String URL_PREFIX = "https://textures.minecraft.net/texture/";

    private TextureValues() {
    }

    public static @NotNull String url(@NotNull String hash) {
        Objects.requireNonNull(hash, "hash");
        return URL_PREFIX + hash;
    }

    public static @NotNull String encode(@NotNull String hash) {
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + url(hash) + "\"}}}";
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

}
