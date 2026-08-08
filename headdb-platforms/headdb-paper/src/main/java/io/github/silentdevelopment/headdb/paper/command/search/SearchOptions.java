package io.github.silentdevelopment.headdb.paper.command.search;

import io.github.silentdevelopment.headdb.model.HeadId;
import io.github.silentdevelopment.headdb.paper.message.MessageException;
import io.github.silentdevelopment.headdb.paper.message.MessageKey;
import io.github.silentdevelopment.headdb.paper.search.SearchRequest;
import io.github.silentdevelopment.headdb.query.HeadSort;
import io.github.silentdevelopment.headdb.query.SortDirection;
import io.github.silentdevelopment.relay.argument.Argument;
import io.github.silentdevelopment.relay.command.option.ValueCommandOption;
import io.github.silentdevelopment.relay.core.command.option.DefaultCommandOption;
import io.github.silentdevelopment.relay.paper.argument.PaperArgumentTypes;
import io.github.silentdevelopment.relay.paper.command.context.PaperCommandContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SearchOptions {

    public static final int DEFAULT_LIMIT = 10;
    public static final int MAX_LIMIT = 50;
    public static final int MAX_PAGE = 100_000;

    public static final Argument<String> CATEGORY_VALUE = Argument.required("category", PaperArgumentTypes.STRING);
    public static final Argument<String> IDS_VALUE = Argument.required("ids", PaperArgumentTypes.STRING);
    public static final Argument<String> TAGS_VALUE = Argument.required("tags", PaperArgumentTypes.STRING);
    public static final Argument<String> COLLECTIONS_VALUE = Argument.required("collections", PaperArgumentTypes.STRING);
    public static final Argument<HeadSort> SORT_VALUE = Argument.required("sort", PaperArgumentTypes.enumType(HeadSort.class));
    public static final Argument<SortDirection> DIRECTION_VALUE = Argument.required("direction", PaperArgumentTypes.enumType(SortDirection.class));
    public static final Argument<Integer> PAGE_VALUE = Argument.required("page", PaperArgumentTypes.INTEGER);
    public static final Argument<Integer> LIMIT_VALUE = Argument.required("limit", PaperArgumentTypes.INTEGER);

    public static final ValueCommandOption<String> CATEGORY = new DefaultCommandOption<>("category", "Filters by category ID.", List.of("c"), CATEGORY_VALUE);
    public static final ValueCommandOption<String> IDS = new DefaultCommandOption<>("ids", "Filters by comma-separated head IDs.", List.of("id", "i"), IDS_VALUE);
    public static final ValueCommandOption<String> TAGS = new DefaultCommandOption<>("tags", "Filters by comma-separated tag IDs.", List.of("tag", "t"), TAGS_VALUE);
    public static final ValueCommandOption<String> COLLECTIONS = new DefaultCommandOption<>("collections", "Filters by comma-separated collection IDs.", List.of("collection", "cols"), COLLECTIONS_VALUE);
    public static final ValueCommandOption<HeadSort> SORT = new DefaultCommandOption<>("sort", "Sort mode.", List.of("s"), SORT_VALUE);
    public static final ValueCommandOption<SortDirection> DIRECTION = new DefaultCommandOption<>("direction", "Sort direction.", List.of("dir", "d"), DIRECTION_VALUE);
    public static final ValueCommandOption<Integer> PAGE = new DefaultCommandOption<>("page", "Console result page.", List.of("p"), PAGE_VALUE);
    public static final ValueCommandOption<Integer> LIMIT = new DefaultCommandOption<>("limit", "Console result limit.", List.of("l"), LIMIT_VALUE);

    private SearchOptions() {
    }

    public static @NotNull SearchRequest advancedRequest(@NotNull PaperCommandContext context, @NotNull String query) {
        String category = context.hasOption(CATEGORY) ? SearchParser.singleId(context.getOptionValue(CATEGORY), "category") : null;
        Set<HeadId> ids = context.hasOption(IDS) ? SearchParser.headIds(context.getOptionValue(IDS)) : Set.of();
        Set<String> tags = context.hasOption(TAGS) ? SearchParser.idList(context.getOptionValue(TAGS), "tags") : Set.of();
        Set<String> collections = context.hasOption(COLLECTIONS) ? SearchParser.idList(context.getOptionValue(COLLECTIONS), "collections") : Set.of();
        HeadSort sort = context.hasOption(SORT) ? context.getOptionValue(SORT) : defaultSortForText(query);
        SortDirection direction = context.hasOption(DIRECTION) ? context.getOptionValue(DIRECTION) : defaultDirection(sort);
        int page = context.hasOption(PAGE) ? context.getOptionValue(PAGE) : 1;
        int limit = context.hasOption(LIMIT) ? context.getOptionValue(LIMIT) : DEFAULT_LIMIT;

        validatePage(page);
        validateLimit(limit);

        return SearchRequest.singleCategory(query, ids, category, tags, collections, sort, direction, page, limit);
    }

    public static @NotNull SearchRequest tagRequest(@NotNull PaperCommandContext context, @NotNull String tag) {
        return simpleRequest(context, "", Set.of(), null, Set.of(tag), Set.of(), HeadSort.NAME);
    }

    public static @NotNull SearchRequest categoryRequest(@NotNull PaperCommandContext context, @NotNull String category) {
        return simpleRequest(context, "", Set.of(), category, Set.of(), Set.of(), HeadSort.NAME);
    }

    public static @NotNull SearchRequest collectionRequest(@NotNull PaperCommandContext context, @NotNull String collection) {
        return simpleRequest(context, "", Set.of(), null, Set.of(), Set.of(collection), HeadSort.NAME);
    }

    public static @NotNull SearchRequest headRequest(@NotNull PaperCommandContext context, @NotNull HeadId id) {
        return simpleRequest(context, "", Set.of(id), null, Set.of(), Set.of(), HeadSort.ID);
    }

    private static @NotNull SearchRequest simpleRequest(
            @NotNull PaperCommandContext context,
            @NotNull String query,
            @NotNull Set<HeadId> ids,
            @Nullable String category,
            @NotNull Set<String> tags,
            @NotNull Set<String> collections,
            @NotNull HeadSort defaultSort
    ) {
        HeadSort sort = context.hasOption(SORT) ? context.getOptionValue(SORT) : defaultSort;
        SortDirection direction = context.hasOption(DIRECTION) ? context.getOptionValue(DIRECTION) : defaultDirection(sort);
        int page = context.hasOption(PAGE) ? context.getOptionValue(PAGE) : 1;
        int limit = context.hasOption(LIMIT) ? context.getOptionValue(LIMIT) : DEFAULT_LIMIT;

        validatePage(page);
        validateLimit(limit);

        return SearchRequest.singleCategory(query, ids, category, tags, collections, sort, direction, page, limit);
    }

    private static @NotNull HeadSort defaultSortForText(@NotNull String query) {
        if (!query.isBlank()) {
            return HeadSort.RELEVANCE;
        }

        return HeadSort.ID;
    }

    private static @NotNull SortDirection defaultDirection(@NotNull HeadSort sort) {
        if (sort == HeadSort.RELEVANCE) {
            return SortDirection.DESCENDING;
        }

        return SortDirection.ASCENDING;
    }

    private static void validatePage(int page) {
        if (page < 1) {
            throw new MessageException(MessageKey.COMMAND_ERROR_PAGE_MIN);
        }

        if (page > MAX_PAGE) {
            throw new MessageException(MessageKey.COMMAND_ERROR_PAGE_MAX, Map.of("max", String.valueOf(MAX_PAGE)));
        }
    }

    private static void validateLimit(int limit) {
        if (limit < 1) {
            throw new MessageException(MessageKey.COMMAND_ERROR_LIMIT_MIN);
        }

        if (limit > MAX_LIMIT) {
            throw new MessageException(MessageKey.COMMAND_ERROR_LIMIT_MAX, Map.of("max", String.valueOf(MAX_LIMIT)));
        }
    }
}