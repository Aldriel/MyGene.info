package org.mygeneexplorer.prefs;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserPreferencesTest {

    private static final String TEST_ROOT = "org/mygeneexplorer-test";

    private Preferences node;
    private UserPreferences preferences;

    @BeforeEach
    void createNode() {
        node = Preferences.userRoot().node(TEST_ROOT + "/" + UUID.randomUUID());
        preferences = new UserPreferences(node);
    }

    @AfterEach
    void removeNode() throws BackingStoreException {
        node.removeNode();
    }

    @AfterAll
    static void removeTestRoot() throws BackingStoreException {
        Preferences.userRoot().node(TEST_ROOT).removeNode();
    }

    @Test
    void providesDefaults() {
        assertEquals(UserPreferences.DEFAULT_FONT_SIZE, preferences.fontSize());
        assertEquals(UserPreferences.DEFAULT_MAX_VARIANTS, preferences.maxVariants());
        assertEquals(List.of(), preferences.recentSearches());
        assertEquals(UserPreferences.DEFAULT_WINDOW_WIDTH, preferences.windowWidth());
        assertEquals(UserPreferences.DEFAULT_WINDOW_HEIGHT, preferences.windowHeight());
        assertFalse(preferences.windowMaximized());
        assertEquals(Optional.empty(), preferences.lastDirectory());
    }

    @Test
    void storesAndClampsTheFontSize() {
        preferences.setFontSize(18);
        assertEquals(18, new UserPreferences(node).fontSize());

        preferences.setFontSize(99);
        assertEquals(UserPreferences.MAX_FONT_SIZE, preferences.fontSize());

        node.putInt("fontSize", 1);
        assertEquals(UserPreferences.MIN_FONT_SIZE, preferences.fontSize());
    }

    @Test
    void storesSupportedLanguagesOnly() {
        preferences.setLocale(Locale.CANADA_FRENCH);
        assertEquals(Locale.FRENCH, preferences.locale());

        preferences.setLocale(Locale.GERMAN);
        assertEquals(Locale.ENGLISH, preferences.locale());
    }

    @Test
    void storesAllowedNumbersOfVariants() {
        preferences.setMaxVariants(500);
        assertEquals(500, preferences.maxVariants());

        assertThrows(IllegalArgumentException.class, () -> preferences.setMaxVariants(42));
        node.putInt("maxVariants", 42);
        assertEquals(UserPreferences.DEFAULT_MAX_VARIANTS, preferences.maxVariants());
    }

    @Test
    void keepsRecentSearchesMostRecentFirstWithoutDuplicates() {
        preferences.addRecentSearch("BRCA1");
        preferences.addRecentSearch("TP53");
        preferences.addRecentSearch("BRCA1");

        assertEquals(List.of("BRCA1", "TP53"), preferences.recentSearches());
    }

    @Test
    void limitsAndClearsRecentSearches() {
        IntStream.range(0, 15).forEach(i -> preferences.addRecentSearch("GENE" + i));

        List<String> recent = preferences.recentSearches();
        assertEquals(UserPreferences.MAX_RECENT_SEARCHES, recent.size());
        assertEquals("GENE14", recent.getFirst());

        preferences.clearRecentSearches();
        assertEquals(List.of(), preferences.recentSearches());
    }

    @Test
    void storesTheWindowStateButKeepsTheSizeWhenMaximized() {
        preferences.setWindowState(900, 600, false);
        preferences.setWindowState(1920, 1080, true);

        assertEquals(900, preferences.windowWidth());
        assertEquals(600, preferences.windowHeight());
        assertTrue(preferences.windowMaximized());

        preferences.setWindowState(10, 10, false);
        assertEquals(400, preferences.windowWidth());
    }

    @Test
    void remembersExistingDirectoriesOnly(@TempDir Path dir) {
        preferences.setLastDirectory(dir);
        assertEquals(Optional.of(dir.toAbsolutePath()), preferences.lastDirectory());

        preferences.setLastDirectory(dir.resolve("deleted"));
        assertEquals(Optional.empty(), preferences.lastDirectory());
    }

    @Test
    void flushDoesNotFail() {
        preferences.setFontSize(16);
        preferences.flush();

        assertEquals(16, preferences.fontSize());
    }
}
