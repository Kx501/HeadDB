package io.github.silentdevelopment.headdb.paper.message;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

/**
 * An argument error whose text is resolved from the localized message files instead of a hardcoded string.
 */
public final class MessageException extends IllegalArgumentException {

    private final MessageKey key;
    private final Map<String, String> placeholders;

    public MessageException(@NotNull MessageKey key) {
        this(key, Map.of(), null);
    }

    public MessageException(@NotNull MessageKey key, @NotNull Map<String, String> placeholders) {
        this(key, placeholders, null);
    }

    public MessageException(@NotNull MessageKey key, @NotNull Map<String, String> placeholders, @Nullable Throwable cause) {
        super(key.path(), cause);
        this.key = Objects.requireNonNull(key, "key");
        this.placeholders = Map.copyOf(Objects.requireNonNull(placeholders, "placeholders"));
    }

    public @NotNull MessageKey key() {
        return key;
    }

    public @NotNull Map<String, String> placeholders() {
        return placeholders;
    }
}
