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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mygeneexplorer.model.ClinvarVariant;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CsvExporterTest {

    private static final List<String> HEADERS =
            List.of("Gene", "Variant ID", "HGVS", "Clinical significance", "Origin", "URL");

    private static final ClinvarVariant VARIANT = new ClinvarVariant(
            "chr17:g.41199721C>T", 125845L,
            List.of("Pathogenic", "Likely pathogenic"), List.of("germline"));

    @Test
    void writesHeaderAndOneRowPerVariant() {
        String csv = CsvExporter.toCsv("BRCA1", List.of(VARIANT), HEADERS, ',');

        assertEquals("Gene,Variant ID,HGVS,Clinical significance,Origin,URL\r\n"
                + "BRCA1,125845,chr17:g.41199721C>T,\"Pathogenic, Likely pathogenic\",germline,"
                + "https://www.ncbi.nlm.nih.gov/clinvar/variation/125845/\r\n", csv);
    }

    @Test
    void semicolonSeparatorDoesNotQuoteCommas() {
        String csv = CsvExporter.toCsv("BRCA1", List.of(VARIANT), HEADERS, ';');

        assertEquals("BRCA1;125845;chr17:g.41199721C>T;Pathogenic, Likely pathogenic;germline;"
                + "https://www.ncbi.nlm.nih.gov/clinvar/variation/125845/", csv.split("\r\n")[1]);
    }

    @Test
    void leavesMissingValuesEmpty() {
        ClinvarVariant bare = new ClinvarVariant(null, null, List.of(), List.of());

        String csv = CsvExporter.toCsv("BRCA1", List.of(bare), HEADERS, ',');

        assertEquals("BRCA1,,,,,", csv.split("\r\n")[1]);
    }

    @ParameterizedTest(name = "escapes {0}")
    @CsvSource(delimiter = '|', quoteCharacter = '`', value = {
            "plain|plain",
            "a,b|`\"a,b\"`",
            "say \"hi\"|`\"say \"\"hi\"\"\"`",
            "`line\nbreak`|`\"line\nbreak\"`",
            "=SUM(A1)|'=SUM(A1)",
            "+1|'+1",
            "-1|'-1",
            "@cmd|'@cmd",
    })
    void escapesValues(String value, String expected) {
        assertEquals(expected, CsvExporter.escape(value, ','));
    }

    @Test
    void escapesNullAndEmptyAsEmpty() {
        assertEquals("", CsvExporter.escape(null, ','));
        assertEquals("", CsvExporter.escape("", ','));
    }

    @Test
    void choosesTheSeparatorExpectedBySpreadsheets() {
        assertEquals(';', CsvExporter.separatorFor(Locale.FRANCE));
        assertEquals(';', CsvExporter.separatorFor(Locale.CANADA_FRENCH));
        assertEquals(',', CsvExporter.separatorFor(Locale.US));
        assertEquals(',', CsvExporter.separatorFor(Locale.CANADA));
    }

    @Test
    void rejectsWrongNumberOfHeaders() {
        assertThrows(IllegalArgumentException.class,
                () -> CsvExporter.toCsv("BRCA1", List.of(), List.of("Gene"), ','));
    }

    @Test
    void writesUtf8WithByteOrderMark(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("export.csv");

        CsvExporter.write(file, "GÈNE", List.of(), HEADERS, ';');

        byte[] bytes = Files.readAllBytes(file);
        assertArrayEquals(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF},
                new byte[] {bytes[0], bytes[1], bytes[2]});
        assertEquals("\uFEFFGene;Variant ID;HGVS;Clinical significance;Origin;URL\r\n",
                Files.readString(file, StandardCharsets.UTF_8));
    }
}
