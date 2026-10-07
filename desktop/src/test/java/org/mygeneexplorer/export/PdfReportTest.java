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

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mygeneexplorer.i18n.Messages;
import org.mygeneexplorer.model.ClinvarResult;
import org.mygeneexplorer.model.ClinvarVariant;
import org.mygeneexplorer.model.GeneInfo;
import org.mygeneexplorer.model.SearchResult;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfReportTest {

    private static final Messages ENGLISH = Messages.forLocale(Locale.ENGLISH);
    private static final Messages FRENCH = Messages.forLocale(Locale.FRENCH);
    private static final Instant GENERATED = Instant.parse("2026-10-06T20:00:00Z");
    private static final PDFont HELVETICA = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

    @TempDir
    Path tempDir;

    @Test
    void containsGeneDistributionAndVariants() throws IOException {
        SearchResult result = result(List.of(
                variant(1001L, "chr17:g.43045712G>A", "Pathogenic", "germline"),
                variant(1002L, "chr17:g.43047643T>C", "Benign", "germline")));

        String text = render(result, result.clinvar().variants(), ENGLISH, Locale.US);

        assertTrue(text.contains("ClinVar variant report"), text);
        assertTrue(text.contains("BRCA1"));
        assertTrue(text.contains("BRCA1 DNA repair associated"));
        assertTrue(text.contains("17q21.31"));
        assertTrue(text.contains("RNF53, BRCC1"));
        assertTrue(text.contains("Clinical significance distribution"));
        assertTrue(text.contains("Pathogenic or likely pathogenic: 1 (50%)"), text);
        assertTrue(text.contains("chr17:g.43045712G>A"));
        assertTrue(text.contains("1002"));
        assertTrue(text.contains("Page 1 of 1"));
        assertTrue(text.contains("MyGene Explorer 1.2.3"));
        assertTrue(text.contains("Not intended for clinical decision-making"));
    }

    @Test
    void signsEveryPageWithClickableLinksInTheReportLanguage() throws IOException {
        List<ClinvarVariant> variants = IntStream.range(0, 120)
                .mapToObj(i -> variant(10_000L + i, "chr17:g." + i + "A>G", "Benign", "germline"))
                .toList();
        SearchResult result = result(variants);

        for (Messages messages : List.of(ENGLISH, FRENCH)) {
            Path file = tempDir.resolve("signed-" + messages.locale() + ".pdf");
            PdfReport.write(file, result, variants, messages, "1.2.3", Locale.US, GENERATED);

            String title = messages.get("brand.title");
            String website = messages.get("brand.website");
            try (PDDocument document = Loader.loadPDF(file.toFile())) {
                int pages = document.getNumberOfPages();
                assertTrue(pages > 1);
                String text = new PDFTextStripper().getText(document);
                assertEquals(pages, text.split(title, -1).length - 1, "signature on every page");

                for (PDPage page : document.getPages()) {
                    List<String> uris = page.getAnnotations().stream()
                            .map(a -> ((PDActionURI) ((PDAnnotationLink) a).getAction()).getURI())
                            .toList();
                    assertEquals(List.of(website, "mailto:contact@maximeethier.com"), uris);
                }
            }
        }
        assertEquals("https://www.maximeethier.com/en", ENGLISH.get("brand.website"));
        assertEquals("https://www.maximeethier.com", FRENCH.get("brand.website"));
        assertEquals("Maxime Ethier - Consultant en Bio-informatique", FRENCH.get("brand.title"));
    }

    @Test
    void listsOnlyTheGivenRows() throws IOException {
        ClinvarVariant shown = variant(1001L, "chr17:g.111A>G", "Pathogenic", "germline");
        ClinvarVariant hidden = variant(1002L, "chr17:g.222C>T", "Benign", "germline");
        SearchResult result = result(List.of(shown, hidden));

        String text = render(result, List.of(shown), ENGLISH, Locale.US);

        assertTrue(text.contains("chr17:g.111A>G"));
        assertFalse(text.contains("chr17:g.222C>T"));
        assertTrue(text.contains("Showing 1 of 2 loaded"), text);
    }

    @Test
    void paginatesLongTablesAndNumbersEveryPage() throws IOException {
        List<ClinvarVariant> variants = IntStream.range(0, 300)
                .mapToObj(i -> variant(10_000L + i, "chr17:g." + i + "A>G",
                        "Uncertain significance", "germline"))
                .toList();
        SearchResult result = result(variants);
        Path file = tempDir.resolve("long.pdf");

        PdfReport.write(file, result, variants, ENGLISH, "1.2.3", Locale.US, GENERATED);

        try (PDDocument document = Loader.loadPDF(file.toFile())) {
            int pages = document.getNumberOfPages();
            assertTrue(pages > 3, "pages: " + pages);
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("Page " + pages + " of " + pages));
            assertTrue(text.contains("chr17:g.299A>G"));
            assertEquals(pages, text.split("Variant ID", -1).length - 1,
                    "the table header is repeated on every page");
        }
    }

    @Test
    void writesFrenchReport() throws IOException {
        SearchResult result = result(List.of(
                variant(1001L, "chr17:g.1A>G", "Likely pathogenic", "germline")));

        String text = render(result, result.clinvar().variants(), FRENCH, Locale.CANADA_FRENCH);

        assertTrue(text.contains("Rapport des variants ClinVar"), text);
        assertTrue(text.contains("Répartition"), text);
        assertTrue(text.contains("Page 1 sur 1"));
        assertTrue(text.contains("Probablement pathogène"), text);
    }

    @Test
    void handlesEmptyResultsAndMissingValues() throws IOException {
        GeneInfo gene = new GeneInfo("x", "ABC1", null, null, null, null, null, List.of());
        SearchResult result = new SearchResult("abc1", gene, new ClinvarResult(0, List.of()),
                GENERATED);

        String text = render(result, List.of(), ENGLISH, Locale.UK);

        assertTrue(text.contains("ABC1"));
        assertTrue(text.contains("No variant to summarize."));
        assertTrue(text.contains("No ClinVar variant found for this gene."));
    }

    @Test
    void replacesCharactersTheStandardFontCannotDraw() throws IOException {
        GeneInfo gene = new GeneInfo("1", "TNF", "tumor necrosis factor α", 7124L,
                "Encodes TNF-α, a cytokine.\nSecond line.", "protein-coding", "6p21.33",
                List.of("TNFA"));
        SearchResult result = new SearchResult("tnf", gene, new ClinvarResult(0, List.of()),
                GENERATED);

        String text = render(result, List.of(), ENGLISH, Locale.US);

        assertTrue(text.contains("tumor necrosis factor ?"), text);
        assertTrue(text.contains("Encodes TNF-?, a cytokine. Second line."), text);
    }

    @Test
    void encodableKeepsAccentsAndNormalizesSpaces() {
        assertEquals("Gène – 42 % ’ok’", PdfReport.encodable("Gène – 42\u202F% ’ok’", HELVETICA));
        assertEquals("a b c", PdfReport.encodable("a\tb\u00A0c", HELVETICA));
        assertEquals("??", PdfReport.encodable("αβ", HELVETICA));
        assertEquals("", PdfReport.encodable(null, HELVETICA));
    }

    @Test
    void wrapsTextAndBreaksLongWords() throws IOException {
        List<String> lines = PdfReport.wrap("one two three four five", HELVETICA, 10, 40);
        assertTrue(lines.size() > 1);
        assertEquals("one two three four five", String.join(" ", lines));

        String hgvs = "NC_000017.11:g.43045712_43045720delinsACGTACGTACGT";
        List<String> broken = PdfReport.wrap(hgvs, HELVETICA, 10, 60);
        assertTrue(broken.size() > 1);
        assertEquals(hgvs, String.join("", broken));

        assertEquals(List.of(""), PdfReport.wrap("", HELVETICA, 10, 60));
    }

    @Test
    void choosesPaperSizeFromLocale() {
        assertEquals(PDRectangle.LETTER, PdfReport.pageSizeFor(Locale.US));
        assertEquals(PDRectangle.LETTER, PdfReport.pageSizeFor(Locale.CANADA_FRENCH));
        assertEquals(PDRectangle.A4, PdfReport.pageSizeFor(Locale.FRANCE));
        assertEquals(PDRectangle.A4, PdfReport.pageSizeFor(Locale.ENGLISH));
    }

    @Test
    void setsDocumentProperties() throws IOException {
        Path file = tempDir.resolve("props.pdf");
        SearchResult result = result(List.of());

        PdfReport.write(file, result, List.of(), ENGLISH, "1.2.3", Locale.US, GENERATED);

        try (PDDocument document = Loader.loadPDF(file.toFile())) {
            assertEquals("BRCA1 - ClinVar variant report",
                    document.getDocumentInformation().getTitle());
            assertEquals("Maxime Ethier - Biocomputing Consultant",
                    document.getDocumentInformation().getAuthor());
            assertEquals(PDRectangle.LETTER.getWidth(),
                    document.getPage(0).getMediaBox().getWidth());
        }
    }

    private String render(SearchResult result, List<ClinvarVariant> rows, Messages messages,
                          Locale locale) throws IOException {
        Path file = tempDir.resolve("report.pdf");
        PdfReport.write(file, result, rows, messages, "1.2.3", locale, GENERATED);
        try (PDDocument document = Loader.loadPDF(file.toFile())) {
            return new PDFTextStripper().getText(document).replaceAll("\\s+", " ");
        }
    }

    private static SearchResult result(List<ClinvarVariant> variants) {
        GeneInfo gene = new GeneInfo("672", "BRCA1", "BRCA1 DNA repair associated", 672L,
                "This gene encodes a nuclear phosphoprotein.", "protein-coding", "17q21.31",
                List.of("RNF53", "BRCC1"));
        return new SearchResult("brca1", gene, new ClinvarResult(variants.size() + 10, variants),
                Instant.parse("2026-10-06T12:00:00Z"));
    }

    private static ClinvarVariant variant(Long id, String hgvs, String significance,
                                          String origin) {
        return new ClinvarVariant(hgvs, id, List.of(significance), List.of(origin));
    }
}
