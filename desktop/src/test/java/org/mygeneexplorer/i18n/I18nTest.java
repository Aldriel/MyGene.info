package org.mygeneexplorer.i18n;

import javafx.beans.binding.StringBinding;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class I18nTest {

    @Test
    void bindingsFollowTheLanguage() {
        I18n i18n = new I18n(Locale.ENGLISH);
        StringBinding button = i18n.text("search.button");
        StringBinding notFound = i18n.text("error.geneNotFound", "ZZZ9");

        assertEquals("Search", button.get());
        i18n.setLocale(Locale.FRENCH);

        assertEquals(Locale.FRENCH, i18n.locale());
        assertEquals("Rechercher", button.get());
        assertEquals("Aucun gène humain trouvé pour le symbole « ZZZ9 ».", notFound.get());
    }

    @Test
    void computedBindingsFollowTheLanguage() {
        I18n i18n = new I18n(Locale.FRENCH);
        StringBinding locale = i18n.text(m -> m.locale().getLanguage());

        i18n.setLocale(Locale.ENGLISH);

        assertEquals("en", locale.get());
    }

    @Test
    void settingTheSameLanguageKeepsTheMessages() {
        I18n i18n = new I18n(Locale.CANADA_FRENCH);
        Messages before = i18n.messages();

        i18n.setLocale(Locale.FRANCE);

        assertSame(before, i18n.messagesProperty().get());
    }
}
