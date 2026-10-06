package org.mygeneexplorer.i18n;

import java.text.MessageFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * Localized messages of the application for one language.
 *
 * <p>Messages are {@link MessageFormat} patterns stored in {@code messages_<lang>.properties}.
 * Bundles use the typographic apostrophe (’) because a plain one is a quoting character in
 * {@link MessageFormat} patterns.
 */
public final class Messages {

    /** Languages the user interface is translated into. */
    public static final List<Locale> SUPPORTED_LOCALES = List.of(Locale.ENGLISH, Locale.FRENCH);

    private static final String BUNDLE = "org.mygeneexplorer.i18n.messages";

    private final Locale locale;
    private final ResourceBundle bundle;

    /**
     * Loads the texts of a supported language.
     *
     * @param locale one of {@link #SUPPORTED_LOCALES}
     */
    private Messages(Locale locale) {
        this.locale = locale;
        this.bundle = ResourceBundle.getBundle(BUNDLE, locale);
    }

    /**
     * @param locale requested locale; unsupported languages fall back to English
     * @return the messages for the closest supported language
     */
    public static Messages forLocale(Locale locale) {
        return new Messages(supportedLocale(locale));
    }

    /** @return the supported locale with the same language, or English */
    public static Locale supportedLocale(Locale locale) {
        if (locale != null) {
            for (Locale supported : SUPPORTED_LOCALES) {
                if (supported.getLanguage().equals(locale.getLanguage())) {
                    return supported;
                }
            }
        }
        return Locale.ENGLISH;
    }

    /** @return the language of these texts */
    public Locale locale() {
        return locale;
    }

    /**
     * Formats a message.
     *
     * @param key  message key
     * @param args values of the {@code {0}}, {@code {1}}… placeholders
     * @return the formatted message, or {@code !key!} if the key is unknown
     */
    public String get(String key, Object... args) {
        String pattern;
        try {
            pattern = bundle.getString(key);
        } catch (MissingResourceException e) {
            return "!" + key + "!";
        }
        return new MessageFormat(pattern, locale).format(args);
    }

    /** @return a date and time formatted for this language, in the system time zone */
    public String formatDateTime(Instant instant) {
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                .withLocale(locale)
                .withZone(ZoneId.systemDefault())
                .format(instant);
    }

    /** @return every message key of the bundle */
    public Set<String> keys() {
        return Collections.unmodifiableSet(bundle.keySet());
    }

    /** @return the raw pattern of a message, or {@code null} if the key is unknown */
    String pattern(String key) {
        return bundle.containsKey(key) ? bundle.getString(key) : null;
    }
}
