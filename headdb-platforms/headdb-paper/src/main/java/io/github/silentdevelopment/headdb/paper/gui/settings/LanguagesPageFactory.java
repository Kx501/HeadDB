package io.github.silentdevelopment.headdb.paper.gui.settings;

import io.github.silentdevelopment.grafik.gui.GuiContext;
import io.github.silentdevelopment.grafik.key.GKey;
import io.github.silentdevelopment.grafik.key.PageKey;
import io.github.silentdevelopment.grafik.paper.core.element.ItemElement;
import io.github.silentdevelopment.grafik.paper.page.PaperPage;
import io.github.silentdevelopment.grafik.paper.page.PaperPageBuilder;
import io.github.silentdevelopment.grafik.paper.page.PaperPageFactory;
import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiHeadIcons;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiItems;
import io.github.silentdevelopment.headdb.paper.gui.common.GuiTitles;
import io.github.silentdevelopment.headdb.paper.gui.MenuState;
import io.github.silentdevelopment.headdb.paper.message.LocaleOption;
import io.github.silentdevelopment.headdb.paper.message.MessageKey;
import io.github.silentdevelopment.headdb.paper.permission.Permissions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.MenuType;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@SuppressWarnings("UnstableApiUsage")
public final class LanguagesPageFactory implements PaperPageFactory<MenuState> {

    public static final GKey<PageKey> KEY = GKey.of("languages");

    private static final String LANGUAGE_PAGE_STATE_KEY = "headdb.languages.page";
    private static final int ROWS = 6;
    private static final int SLOT_BACK = 45;
    private static final int SLOT_PREVIOUS = 48;
    private static final int SLOT_INFO = 49;
    private static final int SLOT_NEXT = 50;
    private static final int SLOT_RESET = 53;
    private static final int SLOT_DENIED = 22;

    private static final int[] LANGUAGE_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private final HeadDBPlugin plugin;

    public LanguagesPageFactory(@NotNull HeadDBPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    @Override
    public @NotNull GKey<PageKey> key() {
        return KEY;
    }

    @Override
    public @NotNull PaperPage<MenuState> create(@NotNull GuiContext<MenuState> context, @NotNull PaperPageBuilder<MenuState> page) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(page, "page");

        page.type(MenuType.GENERIC_9X6);
        Set<Integer> reservedSlots = new HashSet<>();
        Player player = player(context);
        page.title(GuiTitles.title(text("title.languages", "HeadDB Languages"), player != null && plugin.adminModes().enabled(player)));

        set(page, reservedSlots, SLOT_BACK, backButton());

        if (player == null || !Permissions.has(player, Permissions.GUI_SETTINGS_LANGUAGE)) {
            set(page, reservedSlots, SLOT_DENIED, deniedButton());
            set(page, reservedSlots, SLOT_INFO, infoButton(0, 0, 1));
            GuiItems.fillEmpty(plugin, page, ROWS, reservedSlots);
            return page.build();
        }

        renderLanguages(context, page, reservedSlots, player);
        renderControls(context, page, reservedSlots, player);

        GuiItems.fillEmpty(plugin, page, ROWS, reservedSlots);

        return page.build();
    }

    private void renderLanguages(@NotNull GuiContext<MenuState> context, @NotNull PaperPageBuilder<MenuState> page, @NotNull Set<Integer> reservedSlots, @NotNull Player player) {
        List<LocaleOption> locales = plugin.messages().availableLocales();
        int pageIndex = languagePage(context);
        int fromIndex = pageIndex * LANGUAGE_SLOTS.length;

        if (fromIndex >= locales.size() && pageIndex > 0) {
            pageIndex = Math.max(0, pageCount(locales.size()) - 1);
            setLanguagePage(context, pageIndex);
            fromIndex = pageIndex * LANGUAGE_SLOTS.length;
        }

        int toIndex = Math.min(locales.size(), fromIndex + LANGUAGE_SLOTS.length);
        int slotIndex = 0;
        LocaleOption current = plugin.messages().resolvedLocaleOption(player);

        for (int index = fromIndex; index < toIndex; index++) {
            LocaleOption locale = locales.get(index);
            int slot = LANGUAGE_SLOTS[slotIndex];

            set(page, reservedSlots, slot, languageButton(locale, locale.id().equals(current.id())));
            slotIndex++;
        }
    }

    private void renderControls(@NotNull GuiContext<MenuState> context, @NotNull PaperPageBuilder<MenuState> page, @NotNull Set<Integer> reservedSlots, @NotNull Player player) {
        List<LocaleOption> locales = plugin.messages().availableLocales();
        int languagePage = languagePage(context);
        int languagePages = pageCount(locales.size());

        if (languagePage > 0) {
            set(page, reservedSlots, SLOT_PREVIOUS, previousLanguagesButton(languagePage));
        }

        set(page, reservedSlots, SLOT_INFO, infoButton(locales.size(), languagePage, languagePages));

        if (languagePage + 1 < languagePages) {
            set(page, reservedSlots, SLOT_NEXT, nextLanguagesButton(languagePage));
        }

        set(page, reservedSlots, SLOT_RESET, resetLanguageButton(player));
    }

    private @NotNull ItemElement<MenuState> languageButton(@NotNull LocaleOption locale, boolean selected) {
        Objects.requireNonNull(locale, "locale");

        NamedTextColor color = selected ? NamedTextColor.GREEN : NamedTextColor.GOLD;

        return GuiHeadIcons.<MenuState>button(plugin, "language_" + locale.id().value().replace('-', '_').replace(':', '_').toLowerCase(java.util.Locale.ROOT), "languages", GuiItems.name(locale.displayName(), color), List.of(
                GuiItems.idDetail(text("label.id", "ID"), locale.id().value()),
                GuiItems.lore(selected ? text("hint.language-selected", "Currently selected.") : text("hint.language-select", "Click to select this language."), selected ? NamedTextColor.GREEN : NamedTextColor.GRAY)
        ), context -> {
            Player player = player(context);

            if (player == null) {
                return;
            }

            if (!Permissions.has(player, Permissions.GUI_SETTINGS_LANGUAGE)) {
                player.sendMessage(plugin.messages().render(player, MessageKey.COMMAND_ERROR_NO_PERMISSION));
                return;
            }

            plugin.messages().setLocale(player.getUniqueId(), locale.id());
            player.sendMessage(plugin.messages().languageChanged(player, locale));
            context.refresh();
        });
    }

    private @NotNull ItemElement<MenuState> backButton() {
        return GuiHeadIcons.<MenuState>button(plugin, "back", "back", context -> context.openPage(SettingsPageFactory.KEY));
    }

    private @NotNull ItemElement<MenuState> previousLanguagesButton(int currentPage) {
        return GuiHeadIcons.<MenuState>button(plugin, "previous_languages", "previous", context -> {
            setLanguagePage(context, currentPage - 1);
            context.refresh();
        });
    }

    private @NotNull ItemElement<MenuState> nextLanguagesButton(int currentPage) {
        return GuiHeadIcons.<MenuState>button(plugin, "next_languages", "next", context -> {
            setLanguagePage(context, currentPage + 1);
            context.refresh();
        });
    }

    private @NotNull ItemElement<MenuState> infoButton(int localeCount, int pageIndex, int pageCount) {
        return GuiHeadIcons.<MenuState>button(plugin, "languages_info", "info", GuiItems.name(text("name.languages", "Languages"), NamedTextColor.GOLD), List.of(
                GuiItems.lore(text("hint.locale-count", "Locales: %count%").replace("%count%", String.valueOf(localeCount)), NamedTextColor.GRAY),
                GuiItems.lore(text("hint.page-line", "Page: %page% / %pages%")
                        .replace("%page%", String.valueOf(pageIndex + 1))
                        .replace("%pages%", String.valueOf(Math.max(1, pageCount))), NamedTextColor.GRAY)
        ), ignored -> {});
    }

    private @NotNull ItemElement<MenuState> resetLanguageButton(@NotNull Player viewer) {
        LocaleOption current = plugin.messages().resolvedLocaleOption(viewer);

        return GuiHeadIcons.<MenuState>button(plugin, "reset_language", "reset-language", GuiItems.name(text("name.reset-language", "Reset Language"), NamedTextColor.RED), List.of(
                GuiItems.lore(text("hint.current-locale", "Current: %locale%").replace("%locale%", current.displayName()), NamedTextColor.GRAY),
                GuiItems.lore(text("hint.reset-language", "Reset to the configured default locale."), NamedTextColor.GRAY)
        ), context -> {
            Player player = player(context);

            if (player == null) {
                return;
            }

            if (!Permissions.has(player, Permissions.GUI_SETTINGS_LANGUAGE)) {
                player.sendMessage(plugin.messages().render(player, MessageKey.COMMAND_ERROR_NO_PERMISSION));
                return;
            }

            plugin.messages().clearLocale(player.getUniqueId());
            player.sendMessage(plugin.messages().languageReset(player));
            context.refresh();
        });
    }

    private @NotNull ItemElement<MenuState> deniedButton() {
        return GuiHeadIcons.<MenuState>button(plugin, "languages_denied", "no-permission", GuiItems.name(text("name.no-permission", "No Permission"), NamedTextColor.RED), List.of(GuiItems.lore(text("hint.no-language-permission", "You cannot change HeadDB language settings."), NamedTextColor.GRAY)), ignored -> {});
    }

    private @NotNull String text(@NotNull String key, @NotNull String fallback) {
        return plugin.guiConfig().text(key, fallback);
    }

    private static int languagePage(@NotNull GuiContext<MenuState> context) {
        Integer value = context.state().get(LANGUAGE_PAGE_STATE_KEY, Integer.class);

        if (value == null || value < 0) {
            return 0;
        }

        return value;
    }

    private static void setLanguagePage(@NotNull GuiContext<MenuState> context, int page) {
        context.state().put(LANGUAGE_PAGE_STATE_KEY, Math.max(0, page));
    }

    private static int pageCount(int entries) {
        if (entries <= 0) {
            return 1;
        }

        return (int) Math.ceil((double) entries / (double) LANGUAGE_SLOTS.length);
    }

    private static void set(@NotNull PaperPageBuilder<MenuState> page, @NotNull Set<Integer> reservedSlots, int slot, @NotNull ItemElement<MenuState> element) {
        page.set(slot, element);
        reservedSlots.add(slot);
    }

    private Player player(@NotNull GuiContext<MenuState> context) {
        Objects.requireNonNull(context, "context");
        return Bukkit.getPlayer(context.source().viewerId());
    }
}