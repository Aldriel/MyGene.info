package org.mygeneexplorer.i18n;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessagesTest {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\d+)");

    private final Messages english = Messages.forLocale(Locale.ENGLISH);
    private final Messages french = Messages.forLocale(Locale.FRENCH);

    @Test
    void everyMessageIsTranslatedInEveryLanguage() {
        assertEquals(new TreeSet<>(english.keys()), new TreeSet<>(french.keys()));
        assertTrue(english.keys().size() > 100, "unexpectedly few messages");
    }

    @Test
    void translationsUseTheSamePlaceholders() {
        for (String key : english.keys()) {
            assertEquals(placeholders(english.pattern(key)), placeholders(french.pattern(key)),
                    "placeholders of " + key);
        }
    }

    @Test
    void patternsDoNotUsePlainApostrophes() {
        for (Messages messages : new Messages[] {english, french}) {
            for (String key : messages.keys()) {
                assertFalse(messages.pattern(key).contains("'"),
                        key + " (" + messages.locale() + ") must use the typographic apostrophe");
            }
        }
    }

    @Test
    void everyMessageFormatsWithoutLeftoverPlaceholders() {
        Object[] args = {1, 2, 3};
        for (Messages messages : new Messages[] {english, french}) {
            for (String key : messages.keys()) {
                String text = messages.get(key, args);
                assertFalse(text.contains("{"), key + " (" + messages.locale() + "): " + text);
            }
        }
    }

    @Test
    void formatsNumbersAndChoicesPerLanguage() {
        assertEquals("Showing 3 of 100 loaded · 14,415 in ClinVar",
                english.get("filter.count", 3, 100, 14415L));
        assertEquals("3 affichés sur 100 chargés · 14\u202f415 dans ClinVar",
                french.get("filter.count", 3, 100, 14415L));
        assertEquals("1 variant copied to the clipboard.", english.get("status.copied", 1));
        assertEquals("2 variants copiés dans le presse-papiers.", french.get("status.copied", 2));
        assertEquals("Pathogenic or likely pathogenic: 3 (25%)",
                english.get("summary.pathogenicShare", 3, 0.25));
    }

    @Test
    void fallsBackToEnglishForUnsupportedLanguages() {
        assertEquals(Locale.ENGLISH, Messages.supportedLocale(Locale.GERMAN));
        assertEquals(Locale.ENGLISH, Messages.supportedLocale(null));
        assertEquals(Locale.FRENCH, Messages.supportedLocale(Locale.CANADA_FRENCH));
        assertEquals(Locale.ENGLISH, Messages.forLocale(Locale.JAPANESE).locale());
        assertEquals("Search", Messages.forLocale(Locale.GERMAN).get("search.button"));
    }

    @Test
    void marksUnknownKeys() {
        assertEquals("!no.such.key!", english.get("no.such.key"));
    }

    @Test
    void formatsDatesPerLanguage() {
        Instant instant = Instant.parse("2026-10-06T18:30:00Z");

        assertTrue(english.formatDateTime(instant).contains("2026"));
        assertTrue(french.formatDateTime(instant).contains("oct"));
    }

    private static Set<String> placeholders(String pattern) {
        Set<String> found = new TreeSet<>();
        Matcher matcher = PLACEHOLDER.matcher(pattern);
        while (matcher.find()) {
            found.add(matcher.group(1));
        }
        return found;
    }
}
