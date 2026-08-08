package io.github.silentdevelopment.headdb.paper.command.search;

import io.github.silentdevelopment.headdb.model.HeadId;
import io.github.silentdevelopment.headdb.paper.message.MessageException;
import io.github.silentdevelopment.headdb.paper.message.MessageKey;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class SearchParser {

    private static final String REMOTE_PREFIX = "remote:";
    private static final String CUSTOM_PREFIX = "custom:";
    private static final String PLAYER_PREFIX = "player:";

    private SearchParser() {
    }

    public static @NotNull String singleId(@NotNull String raw, @NotNull String name) {
        String value = raw.trim();

        if (value.isEmpty()) {
            throw new MessageException(MessageKey.COMMAND_ERROR_SEARCH_EMPTY, Map.of("name", name));
        }

        if (value.contains(",")) {
            throw new MessageException(MessageKey.COMMAND_ERROR_SEARCH_SINGLE_ID, Map.of("name", name));
        }

        return value;
    }

    public static @NotNull Set<String> idList(@NotNull String raw, @NotNull String name) {
        Set<String> values = new LinkedHashSet<>();

        for (String token : raw.split(",")) {
            String value = token.trim();

            if (value.isEmpty()) {
                throw new MessageException(MessageKey.COMMAND_ERROR_SEARCH_EMPTY_ID, Map.of("name", name));
            }

            values.add(value);
        }

        return Set.copyOf(values);
    }

    public static @NotNull Set<HeadId> headIds(@NotNull String raw) {
        Set<HeadId> ids = new LinkedHashSet<>();

        for (String token : raw.split(",")) {
            String value = token.trim();

            if (value.isEmpty()) {
                throw new MessageException(MessageKey.COMMAND_ERROR_SEARCH_IDS_EMPTY_ID);
            }

            ids.add(headId(value));
        }

        return Set.copyOf(ids);
    }

    public static @NotNull HeadId headId(@NotNull String raw) {
        String value = raw.trim();

        if (value.isEmpty()) {
            throw new MessageException(MessageKey.COMMAND_ERROR_HEAD_ID_EMPTY);
        }

        if (startsWithPrefix(value, REMOTE_PREFIX)) {
            return remoteHeadId(value.substring(REMOTE_PREFIX.length()));
        }

        if (startsWithPrefix(value, CUSTOM_PREFIX)) {
            return customHeadId(value.substring(CUSTOM_PREFIX.length()));
        }

        if (startsWithPrefix(value, PLAYER_PREFIX)) {
            return playerHeadId(value.substring(PLAYER_PREFIX.length()));
        }

        if (looksPrefixed(value)) {
            throw new MessageException(MessageKey.COMMAND_ERROR_HEAD_ID_PREFIX, Map.of("raw", raw));
        }

        return remoteHeadId(value);
    }

    private static @NotNull HeadId remoteHeadId(@NotNull String raw) {
        String value = raw.trim();

        if (value.isEmpty()) {
            throw new MessageException(MessageKey.COMMAND_ERROR_REMOTE_ID_EMPTY);
        }

        if (!isUnsignedInteger(value)) {
            throw new MessageException(MessageKey.COMMAND_ERROR_REMOTE_ID_NUMERIC, Map.of("raw", raw));
        }

        try {
            return HeadId.remote(Integer.parseInt(value));
        } catch (NumberFormatException exception) {
            throw new MessageException(MessageKey.COMMAND_ERROR_REMOTE_ID_LARGE, Map.of("raw", raw), exception);
        }
    }

    private static @NotNull HeadId customHeadId(@NotNull String raw) {
        String value = raw.trim();

        if (value.isEmpty()) {
            throw new MessageException(MessageKey.COMMAND_ERROR_CUSTOM_ID_EMPTY);
        }

        return HeadId.custom(value);
    }

    private static @NotNull HeadId playerHeadId(@NotNull String raw) {
        String value = raw.trim();

        if (value.isEmpty()) {
            throw new MessageException(MessageKey.COMMAND_ERROR_PLAYER_ID_EMPTY);
        }

        try {
            return HeadId.player(UUID.fromString(value));
        } catch (IllegalArgumentException exception) {
            return new HeadId("player:" + value);
        }
    }

    private static boolean startsWithPrefix(@NotNull String value, @NotNull String prefix) {
        return value.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    private static boolean looksPrefixed(@NotNull String value) {
        int separator = value.indexOf(':');

        if (separator <= 0) {
            return false;
        }

        for (int index = 0; index < separator; index++) {
            char character = value.charAt(index);

            if (character >= 'a' && character <= 'z') {
                continue;
            }

            if (character >= 'A' && character <= 'Z') {
                continue;
            }

            return false;
        }

        return true;
    }

    private static boolean isUnsignedInteger(@NotNull String value) {
        if (value.isEmpty()) {
            return false;
        }

        for (int index = 0; index < value.length(); index++) {
            if (!Character.isDigit(value.charAt(index))) {
                return false;
            }
        }

        return true;
    }
}