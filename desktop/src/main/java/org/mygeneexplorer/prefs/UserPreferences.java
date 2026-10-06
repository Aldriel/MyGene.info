package org.mygeneexplorer.prefs;

import org.mygeneexplorer.i18n.Messages;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * User settings remembered between sessions: text size, language, number of variants, recent
 * searches, window size and last directory used for files.
 */
public final class UserPreferences {

    public static final int MIN_FONT_SIZE = 10;
    public static final int MAX_FONT_SIZE = 24;
    public static final int DEFAULT_FONT_SIZE = 14;

    /** Number of variants the user can choose to fetch. */
    public static final List<Integer> MAX_VARIANTS_CHOICES = List.of(100, 250, 500, 1000);
    public static final int DEFAULT_MAX_VARIANTS = 100;

    public static final int MAX_RECENT_SEARCHES = 10;

    public static final double DEFAULT_WINDOW_WIDTH = 1100;
    public static final double DEFAULT_WINDOW_HEIGHT = 780;
    private static final double MIN_WINDOW_SIZE = 400;

    private static final String FONT_SIZE = "fontSize";
    private static final String LANGUAGE = "language";
    private static final String MAX_VARIANTS = "maxVariants";
    private static final String RECENT_SEARCHES = "recentSearches";
    private static final String WINDOW_WIDTH = "windowWidth";
    private static final String WINDOW_HEIGHT = "windowHeight";
    private static final String WINDOW_MAXIMIZED = "windowMaximized";
    private static final String LAST_DIRECTORY = "lastDirectory";

    private final Preferences node;

    /**
     * Creates settings stored in a preferences node.
     *
     * @param node preferences node storing the settings
     */
    public UserPreferences(Preferences node) {
        this.node = node;
    }

    /** @return the settings of the current user */
    public static UserPreferences load() {
        return new UserPreferences(Preferences.userRoot().node("org/mygeneexplorer"));
    }

    /** @return the saved text size in pixels, within the supported range */
    public int fontSize() {
        return clampFontSize(node.getInt(FONT_SIZE, DEFAULT_FONT_SIZE));
    }

    /** @param size text size in pixels, clamped to the supported range */
    public void setFontSize(int size) {
        node.putInt(FONT_SIZE, clampFontSize(size));
    }

    /** @return the size limited to [{@value #MIN_FONT_SIZE}, {@value #MAX_FONT_SIZE}] */
    public static int clampFontSize(int size) {
        return Math.clamp(size, MIN_FONT_SIZE, MAX_FONT_SIZE);
    }

    /** @return the saved language, or the system language if supported, otherwise English */
    public Locale locale() {
        String tag = node.get(LANGUAGE, null);
        Locale requested = tag == null ? Locale.getDefault() : Locale.forLanguageTag(tag);
        return Messages.supportedLocale(requested);
    }

    /** @param locale language to save; unsupported languages are saved as English */
    public void setLocale(Locale locale) {
        node.put(LANGUAGE, Messages.supportedLocale(locale).toLanguageTag());
    }

    /** @return the saved number of variants per search, one of {@link #MAX_VARIANTS_CHOICES} */
    public int maxVariants() {
        int value = node.getInt(MAX_VARIANTS, DEFAULT_MAX_VARIANTS);
        return MAX_VARIANTS_CHOICES.contains(value) ? value : DEFAULT_MAX_VARIANTS;
    }

    /**
     * Saves the number of variants fetched per search.
     *
     * @param value one of {@link #MAX_VARIANTS_CHOICES}
     * @throws IllegalArgumentException if the value is not one of the choices
     */
    public void setMaxVariants(int value) {
        if (!MAX_VARIANTS_CHOICES.contains(value)) {
            throw new IllegalArgumentException("Unsupported number of variants: " + value);
        }
        node.putInt(MAX_VARIANTS, value);
    }

    /** @return the recent searches, most recent first */
    public List<String> recentSearches() {
        return Arrays.stream(node.get(RECENT_SEARCHES, "").split(","))
                .filter(s -> !s.isBlank())
                .limit(MAX_RECENT_SEARCHES)
                .toList();
    }

    /**
     * Records a search at the top of the recent searches, removing any older occurrence.
     *
     * @param symbol validated gene symbol
     */
    public void addRecentSearch(String symbol) {
        List<String> searches = new ArrayList<>(recentSearches());
        searches.remove(symbol);
        searches.addFirst(symbol);
        node.put(RECENT_SEARCHES, String.join(",",
                searches.subList(0, Math.min(searches.size(), MAX_RECENT_SEARCHES))));
    }

    /** Forgets all recent searches. */
    public void clearRecentSearches() {
        node.remove(RECENT_SEARCHES);
    }

    /** @return the saved width of the window content, at least 400 pixels */
    public double windowWidth() {
        return Math.max(MIN_WINDOW_SIZE, node.getDouble(WINDOW_WIDTH, DEFAULT_WINDOW_WIDTH));
    }

    /** @return the saved height of the window content, at least 400 pixels */
    public double windowHeight() {
        return Math.max(MIN_WINDOW_SIZE, node.getDouble(WINDOW_HEIGHT, DEFAULT_WINDOW_HEIGHT));
    }

    /** @return whether the window was maximized */
    public boolean windowMaximized() {
        return node.getBoolean(WINDOW_MAXIMIZED, false);
    }

    /**
     * Saves the window state. The size is kept unchanged while maximized, so the window
     * returns to its previous size when restored.
     *
     * @param width     width of the window content
     * @param height    height of the window content
     * @param maximized whether the window is maximized
     */
    public void setWindowState(double width, double height, boolean maximized) {
        node.putBoolean(WINDOW_MAXIMIZED, maximized);
        if (!maximized) {
            node.putDouble(WINDOW_WIDTH, width);
            node.putDouble(WINDOW_HEIGHT, height);
        }
    }

    /** @return the last directory used for files, if it still exists */
    public Optional<Path> lastDirectory() {
        String value = node.get(LAST_DIRECTORY, null);
        if (value == null) {
            return Optional.empty();
        }
        Path directory = Path.of(value);
        return Files.isDirectory(directory) ? Optional.of(directory) : Optional.empty();
    }

    /** @param directory directory of the last file opened or exported */
    public void setLastDirectory(Path directory) {
        node.put(LAST_DIRECTORY, directory.toAbsolutePath().toString());
    }

    /** Writes pending changes to persistent storage. */
    public void flush() {
        try {
            node.flush();
        } catch (BackingStoreException e) {
            // Settings are a convenience: failing to persist them must not prevent closing.
        }
    }
}
