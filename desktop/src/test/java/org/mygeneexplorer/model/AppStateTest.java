package org.mygeneexplorer.model;

import org.junit.jupiter.api.Test;
import org.mygeneexplorer.i18n.Messages;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppStateTest {

    private static final Messages ENGLISH = Messages.forLocale(Locale.ENGLISH);

    @Test
    void startsEmptyAndReady() {
        AppState state = new AppState();

        assertEquals("", state.getQuery());
        assertFalse(state.isQueryInvalid());
        assertFalse(state.isLoading());
        assertNull(state.getResult());
        assertEquals("Ready.", state.getStatus().render(ENGLISH));
        assertTrue(state.getRecentSearches().isEmpty());
    }

    @Test
    void notifiesListenersOfChanges() {
        AppState state = new AppState();
        AtomicInteger changes = new AtomicInteger();
        state.fontSizeProperty().addListener((obs, old, value) -> changes.incrementAndGet());
        state.loadingProperty().addListener((obs, old, value) -> changes.incrementAndGet());

        state.setFontSize(18);
        state.setLoading(true);

        assertEquals(2, changes.get());
        assertEquals(18, state.getFontSize());
        assertTrue(state.isLoading());
    }

    @Test
    void exposesRecentSearchesReadOnly() {
        AppState state = new AppState();
        state.setRecentSearches(List.of("BRCA1", "TP53"));

        assertEquals(List.of("BRCA1", "TP53"), state.getRecentSearches());
        assertThrows(UnsupportedOperationException.class,
                () -> state.getRecentSearches().add("CFTR"));
    }

    @Test
    void statusMessagesRenderInAnyLanguage() {
        StatusMessage info = StatusMessage.info("status.saved", "a.json");
        StatusMessage error = StatusMessage.error(m -> m.get("error.unexpected"));

        assertFalse(info.error());
        assertTrue(error.error());
        assertEquals("Results exported to a.json.", info.render(ENGLISH));
        assertEquals("Résultats exportés dans a.json.",
                info.render(Messages.forLocale(Locale.FRENCH)));
        assertEquals("An unexpected error occurred.", error.render(ENGLISH));
        assertThrows(NullPointerException.class, () -> new StatusMessage(null, false));
    }
}
