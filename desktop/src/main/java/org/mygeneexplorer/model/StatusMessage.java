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

package org.mygeneexplorer.model;

import org.mygeneexplorer.i18n.Messages;

import java.util.Objects;
import java.util.function.Function;

/**
 * Message shown in the status bar.
 *
 * <p>The text is kept as a function of the current {@link Messages} rather than as a string,
 * so it is translated again when the user switches language.
 *
 * @param text  produces the text in a given language
 * @param error whether the message reports an error
 */
public record StatusMessage(Function<Messages, String> text, boolean error) {

    /** Validates that the text function is present. */
    public StatusMessage {
        Objects.requireNonNull(text, "text");
    }

    /**
     * Creates an informational message from a bundle key.
     *
     * @param key  message key
     * @param args message arguments
     * @return the message
     */
    public static StatusMessage info(String key, Object... args) {
        return new StatusMessage(m -> m.get(key, args), false);
    }

    /**
     * Creates an informational message computed from the current language.
     *
     * @param text produces the text in a given language
     * @return the message
     */
    public static StatusMessage info(Function<Messages, String> text) {
        return new StatusMessage(text, false);
    }

    /**
     * Creates an error message from a bundle key.
     *
     * @param key  message key
     * @param args message arguments
     * @return the message
     */
    public static StatusMessage error(String key, Object... args) {
        return new StatusMessage(m -> m.get(key, args), true);
    }

    /**
     * Creates an error message computed from the current language.
     *
     * @param text produces the text in a given language
     * @return the message
     */
    public static StatusMessage error(Function<Messages, String> text) {
        return new StatusMessage(text, true);
    }

    /**
     * Renders the message in a language.
     *
     * @param messages texts of the language to use
     * @return the translated text
     */
    public String render(Messages messages) {
        return text.apply(messages);
    }
}
