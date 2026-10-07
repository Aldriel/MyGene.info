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

package org.mygeneexplorer.export;

import org.mygeneexplorer.model.ClinvarVariant;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Export of ClinVar variants as CSV (RFC 4180), readable by spreadsheet software.
 *
 * <p>Columns: gene, variant ID, HGVS, clinical significances, origins, ClinVar URL. Header
 * labels are supplied by the caller so they can be localized.
 */
public final class CsvExporter {

    /** Number of columns of the export. */
    public static final int COLUMN_COUNT = 6;

    /** Byte order mark that lets Excel detect UTF-8. */
    private static final String UTF8_BOM = "\uFEFF";
    private static final String LINE_END = "\r\n";
    private static final String VALUE_SEPARATOR = ", ";

    /** Not instantiable: static utility class. */
    private CsvExporter() {
    }

    /**
     * Returns the field separator spreadsheet software expects for a locale: {@code ;} where
     * the decimal separator is a comma (e.g. French), {@code ,} otherwise.
     */
    public static char separatorFor(Locale locale) {
        return DecimalFormatSymbols.getInstance(locale).getDecimalSeparator() == ',' ? ';' : ',';
    }

    /**
     * Builds the CSV document.
     *
     * @param geneSymbol symbol of the gene, repeated on every row
     * @param variants   variants to export, in order
     * @param headers    the {@value #COLUMN_COUNT} column labels
     * @param separator  field separator, e.g. {@code ,}, {@code ;} or a tab
     * @return the CSV text, without byte order mark
     */
    public static String toCsv(String geneSymbol, List<ClinvarVariant> variants,
                               List<String> headers, char separator) {
        if (headers.size() != COLUMN_COUNT) {
            throw new IllegalArgumentException(
                    "Expected " + COLUMN_COUNT + " headers, got " + headers.size());
        }

        StringBuilder csv = new StringBuilder();
        appendRow(csv, headers.stream(), separator);
        for (ClinvarVariant variant : variants) {
            appendRow(csv, Stream.of(
                    geneSymbol,
                    variant.variantId() == null ? "" : variant.variantId().toString(),
                    variant.hgvs(),
                    String.join(VALUE_SEPARATOR, variant.clinicalSignificances()),
                    String.join(VALUE_SEPARATOR, variant.origins()),
                    variant.clinvarUrl()), separator);
        }
        return csv.toString();
    }

    /**
     * Writes the CSV document to a file, encoded in UTF-8 with a byte order mark.
     *
     * @throws IOException if the file cannot be written
     */
    public static void write(Path file, String geneSymbol, List<ClinvarVariant> variants,
                             List<String> headers, char separator) throws IOException {
        Files.writeString(file, UTF8_BOM + toCsv(geneSymbol, variants, headers, separator),
                StandardCharsets.UTF_8);
    }

    /**
     * Appends one CSV line.
     *
     * @param csv       document being built
     * @param values    values of the row, in column order
     * @param separator field separator
     */
    private static void appendRow(StringBuilder csv, Stream<String> values, char separator) {
        csv.append(values
                        .map(value -> escape(value, separator))
                        .collect(Collectors.joining(String.valueOf(separator))))
                .append(LINE_END);
    }

    /**
     * Quotes a value when it contains the separator, a quote or a line break, and neutralizes
     * values that spreadsheet software would evaluate as formulas.
     *
     * @param value     raw value, may be {@code null}
     * @param separator field separator
     * @return the value as written in the CSV document
     */
    static String escape(String value, char separator) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String safe = "=+-@".indexOf(value.charAt(0)) >= 0 ? "'" + value : value;
        boolean needsQuotes = safe.indexOf(separator) >= 0 || safe.indexOf('"') >= 0
                || safe.indexOf('\n') >= 0 || safe.indexOf('\r') >= 0;
        return needsQuotes ? '"' + safe.replace("\"", "\"\"") + '"' : safe;
    }
}
