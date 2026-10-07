/*
 * Copyright 2026 Maxime Ethier - Consultant en Bioinformatique/Biocomputing Consultant
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
