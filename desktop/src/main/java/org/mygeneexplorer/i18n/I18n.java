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

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;

import java.util.Locale;
import java.util.function.Function;

/**
 * Current language of the user interface.
 *
 * <p>Texts are exposed as bindings that are recomputed whenever the language changes, so the
 * whole interface switches language without being rebuilt.
 */
public final class I18n {

    private final ReadOnlyObjectWrapper<Messages> messages;

    /**
     * Creates the language holder.
     *
     * @param locale initial language; unsupported languages fall back to English
     */
    public I18n(Locale locale) {
        messages = new ReadOnlyObjectWrapper<>(Messages.forLocale(locale));
    }

    /** @return the texts of the current language, observable to react to language changes */
    public ReadOnlyObjectProperty<Messages> messagesProperty() {
        return messages.getReadOnlyProperty();
    }

    /** @return the texts of the current language */
    public Messages messages() {
        return messages.get();
    }

    /** @return the current language */
    public Locale locale() {
        return messages.get().locale();
    }

    /**
     * Switches the language; unsupported languages fall back to English.
     *
     * @param locale requested language
     */
    public void setLocale(Locale locale) {
        if (!Messages.supportedLocale(locale).equals(locale())) {
            messages.set(Messages.forLocale(locale));
        }
    }

    /**
     * Returns a binding to a message, updated when the language changes.
     *
     * @param key  message key
     * @param args message arguments
     * @return the binding
     */
    public StringBinding text(String key, Object... args) {
        return text(m -> m.get(key, args));
    }

    /**
     * Returns a binding to a computed text, updated when the language changes.
     *
     * @param text produces the text in a given language
     * @return the binding
     */
    public StringBinding text(Function<Messages, String> text) {
        return Bindings.createStringBinding(() -> text.apply(messages.get()), messages);
    }
}
