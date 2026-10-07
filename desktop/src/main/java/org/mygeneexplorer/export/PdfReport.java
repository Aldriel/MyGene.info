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

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary;
import org.mygeneexplorer.AppInfo;
import org.mygeneexplorer.i18n.Messages;
import org.mygeneexplorer.model.ClinvarVariant;
import org.mygeneexplorer.model.GeneInfo;
import org.mygeneexplorer.model.SearchResult;
import org.mygeneexplorer.model.SignificanceCategory;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * PDF report of a search result: gene card, distribution of clinical significance and table
 * of variants, with the author signature, page numbers and the data provenance on every page.
 *
 * <p>The report uses the standard PDF fonts, which every reader can display without embedding;
 * characters they cannot encode (e.g. Greek letters) are replaced by {@code ?}.
 */
public final class PdfReport {

    private static final float MARGIN = 50;
    private static final float FOOTER_HEIGHT = 46;
    private static final float LINE_SPACING = 1.3f;
    private static final float BODY_SIZE = 9.5f;
    private static final float SMALL_SIZE = 7.5f;
    private static final float CELL_PADDING = 4;
    private static final float SWATCH_SIZE = 7;
    private static final float[] TABLE_COLUMNS = {0.13f, 0.37f, 0.32f, 0.18f};

    private static final float[] TEXT = rgb("#0f172a");
    private static final float[] MUTED = rgb("#64748b");
    private static final float[] ACCENT = rgb("#4f46e5");
    private static final float[] RULE = rgb("#cbd5e1");
    private static final float[] HEADER_FILL = rgb("#e2e8f0");
    private static final float[] STRIPE_FILL = rgb("#f8fafc");
    private static final float[] BAR_TRACK = rgb("#f1f5f9");

    private final PDDocument document;
    private final PDRectangle pageSize;
    private final Messages messages;
    private final PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private final PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private final float width;

    private PDPageContentStream out;
    private float y;

    /**
     * Prepares a report rendered into a document.
     *
     * @param document document receiving the pages
     * @param pageSize size of the pages
     * @param messages texts of the report language
     */
    private PdfReport(PDDocument document, PDRectangle pageSize, Messages messages) {
        this.document = document;
        this.pageSize = pageSize;
        this.messages = messages;
        this.width = pageSize.getWidth() - 2 * MARGIN;
    }

    /**
     * Writes the report of a search result to a PDF file.
     *
     * @param file         destination file
     * @param result       result to report
     * @param rows         variants to list, in display order (e.g. filtered and sorted)
     * @param messages     texts of the report language
     * @param appVersion   version of the application, shown in the footer
     * @param formatLocale locale whose conventions select the paper size
     * @param generatedAt  generation time, shown in the footer
     * @throws IOException if the file cannot be written
     */
    public static void write(Path file, SearchResult result, List<ClinvarVariant> rows,
                             Messages messages, String appVersion, Locale formatLocale,
                             Instant generatedAt) throws IOException {
        try (PDDocument document = new PDDocument()) {
            new PdfReport(document, pageSizeFor(formatLocale), messages)
                    .render(result, rows, appVersion, generatedAt);
            document.save(file.toFile());
        }
    }

    /**
     * Returns the usual paper size of a locale: US Letter in the United States and Canada,
     * A4 elsewhere.
     *
     * @param locale locale of the user
     * @return the page size
     */
    public static PDRectangle pageSizeFor(Locale locale) {
        String country = locale.getCountry();
        return "US".equals(country) || "CA".equals(country) ? PDRectangle.LETTER : PDRectangle.A4;
    }

    /**
     * Renders every section, then the footers once the number of pages is known.
     *
     * @param result      result to report
     * @param rows        variants to list
     * @param appVersion  version of the application
     * @param generatedAt generation time
     * @throws IOException if a page cannot be written
     */
    private void render(SearchResult result, List<ClinvarVariant> rows, String appVersion,
                        Instant generatedAt) throws IOException {
        setMetadata(result, generatedAt);
        newPage();
        try {
            renderHeader(result);
            renderGeneDetails(result);
            renderDistribution(result);
            renderVariants(result, rows);
        } finally {
            out.close();
        }
        renderFooters(appVersion, generatedAt);
    }

    /**
     * Fills the document properties shown by PDF readers.
     *
     * @param result      result to report
     * @param generatedAt generation time
     */
    private void setMetadata(SearchResult result, Instant generatedAt) {
        PDDocumentInformation info = document.getDocumentInformation();
        info.setTitle(result.gene().symbol() + " - " + messages.get("pdf.title"));
        info.setAuthor(messages.get("brand.title"));
        info.setKeywords(messages.get("brand.website") + ", " + AppInfo.CONTACT_EMAIL);
        info.setCreator(AppInfo.NAME);
        info.setSubject(messages.get("status.sources"));
        Calendar created = GregorianCalendar.getInstance();
        created.setTimeInMillis(generatedAt.toEpochMilli());
        info.setCreationDate(created);
    }

    // ---------------------------------------------------------------------
    // Sections
    // ---------------------------------------------------------------------

    /**
     * Renders the title block: application, gene symbol and name.
     *
     * @param result result to report
     */
    private void renderHeader(SearchResult result) throws IOException {
        GeneInfo gene = result.gene();
        text(AppInfo.NAME + "  ·  " + messages.get("pdf.title"), bold, SMALL_SIZE + 1,
                MARGIN, ACCENT);
        y -= 22;
        drawText(gene.symbol(), bold, 22, MARGIN, y, TEXT);
        y -= 6;
        if (gene.name() != null && !gene.name().isBlank()) {
            paragraph(gene.name(), regular, 12, MUTED);
        }
        y -= 4;
        rule();
        y -= 12;
    }

    /**
     * Renders the gene details (type, location, aliases, identifiers, retrieval date) and its
     * summary.
     *
     * @param result result to report
     */
    private void renderGeneDetails(SearchResult result) throws IOException {
        GeneInfo gene = result.gene();
        float labelWidth = 120;
        detailRow(messages.get("gene.type"), gene.typeOfGene(), labelWidth);
        detailRow(messages.get("gene.location"), gene.mapLocation(), labelWidth);
        detailRow(messages.get("gene.aliases"), String.join(", ", gene.aliases()), labelWidth);
        detailRow(messages.get("pdf.entrez"), gene.entrezIdOrId(), labelWidth);
        detailRow(messages.get("pdf.retrieved"),
                messages.formatDateTime(result.retrievedAt()), labelWidth);

        if (gene.summary() != null && !gene.summary().isBlank()) {
            y -= 6;
            sectionTitle(messages.get("gene.summary"));
            paragraph(gene.summary(), regular, BODY_SIZE, TEXT);
        }
        y -= 10;
    }

    /**
     * Renders the distribution of the loaded variants by clinical significance, as bars.
     *
     * @param result result to report
     */
    private void renderDistribution(SearchResult result) throws IOException {
        List<ClinvarVariant> variants = result.clinvar().variants();
        sectionTitle(messages.get("summary.title"));
        if (variants.isEmpty()) {
            paragraph(messages.get("summary.empty"), regular, BODY_SIZE, MUTED);
            y -= 10;
            return;
        }

        int size = variants.size();
        Map<SignificanceCategory, Long> counts = SignificanceCategory.countByPrimary(variants);
        float lineHeight = BODY_SIZE * LINE_SPACING + 4;
        float nameWidth = 170;
        float barWidth = width - nameWidth - 110;

        for (Map.Entry<SignificanceCategory, Long> entry : counts.entrySet()) {
            long count = entry.getValue();
            if (count == 0) {
                continue;
            }
            ensureSpace(lineHeight);
            SignificanceCategory category = entry.getKey();
            float baseline = y - BODY_SIZE;
            float[] color = rgb(category.color());
            fillRect(MARGIN, baseline, SWATCH_SIZE, SWATCH_SIZE, color);
            drawText(messages.get(category.messageKey()), regular, BODY_SIZE,
                    MARGIN + SWATCH_SIZE + 6, baseline, TEXT);
            float barX = MARGIN + nameWidth;
            fillRect(barX, baseline - 1, barWidth, BODY_SIZE, BAR_TRACK);
            fillRect(barX, baseline - 1, Math.max(1, barWidth * count / size), BODY_SIZE, color);
            drawText(messages.get("summary.legendCount", count, (double) count / size),
                    regular, BODY_SIZE, barX + barWidth + 8, baseline, MUTED);
            y -= lineHeight;
        }

        long pathogenic = counts.get(SignificanceCategory.PATHOGENIC)
                + counts.get(SignificanceCategory.LIKELY_PATHOGENIC);
        y -= 4;
        paragraph(messages.get("summary.pathogenicShare", pathogenic, (double) pathogenic / size),
                bold, BODY_SIZE, TEXT);
        paragraph(messages.get("summary.caption", size, result.clinvar().total()),
                regular, SMALL_SIZE, MUTED);
        y -= 12;
    }

    /**
     * Renders the table of variants, repeating its header on every page.
     *
     * @param result result to report
     * @param rows   variants to list, in order
     */
    private void renderVariants(SearchResult result, List<ClinvarVariant> rows)
            throws IOException {
        sectionTitle(messages.get("pdf.variants"));
        paragraph(messages.get("filter.count", rows.size(), result.clinvar().variants().size(),
                result.clinvar().total()), regular, SMALL_SIZE, MUTED);
        y -= 6;

        if (rows.isEmpty()) {
            String key = result.clinvar().variants().isEmpty() ? "table.empty" : "filter.noMatch";
            paragraph(messages.get(key), regular, BODY_SIZE, MUTED);
            return;
        }

        List<String> headers = List.of(messages.get("table.variantId"),
                messages.get("table.hgvs"), messages.get("table.significance"),
                messages.get("table.origin"));
        float headerHeight = rowHeight(headers, bold);
        ensureSpace(headerHeight + BODY_SIZE * 3);
        tableHeader(headers, headerHeight);

        boolean striped = false;
        for (ClinvarVariant variant : rows) {
            List<String> cells = List.of(
                    variant.variantId() == null ? "—" : variant.variantId().toString(),
                    orDash(variant.hgvs()),
                    orDash(String.join(", ", variant.clinicalSignificances())),
                    orDash(String.join(", ", variant.origins())));
            float height = rowHeight(cells, regular);
            if (ensureSpace(height)) {
                tableHeader(headers, headerHeight);
                striped = false;
            }
            if (striped) {
                fillRect(MARGIN, y - height, width, height, STRIPE_FILL);
            }
            tableRow(cells, regular, height, SignificanceCategory.primaryOf(variant));
            striped = !striped;
        }
        strokeLine(MARGIN, y, MARGIN + width, y, RULE);
    }

    /**
     * Adds the footer of every page: author signature with clickable website and e-mail, page
     * number, disclaimer and provenance.
     *
     * @param appVersion  version of the application
     * @param generatedAt generation time
     */
    private void renderFooters(String appVersion, Instant generatedAt) throws IOException {
        int count = document.getNumberOfPages();
        String provenance = messages.get("pdf.generated", messages.formatDateTime(generatedAt),
                AppInfo.NAME, appVersion) + "  ·  " + messages.get("status.sources");
        String disclaimer = messages.get("app.disclaimer");
        for (int i = 0; i < count; i++) {
            PDPage page = document.getPage(i);
            try (PDPageContentStream footer = new PDPageContentStream(document, page,
                    PDPageContentStream.AppendMode.APPEND, true, true)) {
                out = footer;
                float top = MARGIN + FOOTER_HEIGHT - 10;
                strokeLine(MARGIN, top, MARGIN + width, top, RULE);

                String pageNumber = messages.get("pdf.page", i + 1, count);
                float numberWidth = textWidth(pageNumber, bold, SMALL_SIZE);
                signature(page, top - 11);
                drawText(pageNumber, bold, SMALL_SIZE, MARGIN + width - numberWidth, top - 11,
                        TEXT);

                drawText(fit(disclaimer, regular, SMALL_SIZE, width), regular, SMALL_SIZE,
                        MARGIN, top - 22, MUTED);
                drawText(fit(provenance, regular, SMALL_SIZE, width), regular, SMALL_SIZE,
                        MARGIN, top - 33, MUTED);
            }
        }
    }

    /**
     * Draws the author signature: name and title, website and e-mail, the last two clickable.
     *
     * @param page     page receiving the link annotations
     * @param baseline baseline of the signature
     */
    private void signature(PDPage page, float baseline) throws IOException {
        String separator = "  ·  ";
        String title = messages.get("brand.title");
        String website = messages.get("brand.website");
        String email = AppInfo.CONTACT_EMAIL;

        float x = MARGIN;
        drawText(title, bold, SMALL_SIZE, x, baseline, ACCENT);
        x += textWidth(encodable(title, bold), bold, SMALL_SIZE);
        drawText(separator, regular, SMALL_SIZE, x, baseline, MUTED);
        x += textWidth(separator, regular, SMALL_SIZE);
        x += linkedText(page, website, website, x, baseline);
        drawText(separator, regular, SMALL_SIZE, x, baseline, MUTED);
        x += textWidth(separator, regular, SMALL_SIZE);
        linkedText(page, email, "mailto:" + email, x, baseline);
    }

    /**
     * Draws small accent-colored text and makes it a clickable link.
     *
     * @param page     page receiving the link annotation
     * @param value    text to draw
     * @param uri      address opened when the text is clicked
     * @param x        left position
     * @param baseline baseline position
     * @return the width of the text
     */
    private float linkedText(PDPage page, String value, String uri, float x, float baseline)
            throws IOException {
        drawText(value, regular, SMALL_SIZE, x, baseline, ACCENT);
        float textWidth = textWidth(encodable(value, regular), regular, SMALL_SIZE);

        PDActionURI action = new PDActionURI();
        action.setURI(uri);
        PDAnnotationLink link = new PDAnnotationLink();
        link.setAction(action);
        link.setRectangle(new PDRectangle(x, baseline - 2, textWidth, SMALL_SIZE + 3));
        PDBorderStyleDictionary noBorder = new PDBorderStyleDictionary();
        noBorder.setWidth(0);
        link.setBorderStyle(noBorder);
        page.getAnnotations().add(link);
        return textWidth;
    }

    // ---------------------------------------------------------------------
    // Building blocks
    // ---------------------------------------------------------------------

    /** Starts a new page and moves the cursor to its top margin. */
    private void newPage() throws IOException {
        if (out != null) {
            out.close();
        }
        PDPage page = new PDPage(pageSize);
        document.addPage(page);
        out = new PDPageContentStream(document, page);
        y = pageSize.getHeight() - MARGIN;
    }

    /**
     * Starts a new page if the remaining space is too small.
     *
     * @param height space needed
     * @return whether a new page was started
     */
    private boolean ensureSpace(float height) throws IOException {
        if (y - height < MARGIN + FOOTER_HEIGHT) {
            newPage();
            return true;
        }
        return false;
    }

    /**
     * Renders a section title followed by a thin rule.
     *
     * @param title title text
     */
    private void sectionTitle(String title) throws IOException {
        ensureSpace(40);
        text(title, bold, 12.5f, MARGIN, ACCENT);
        y -= 4;
        rule();
        y -= 8;
    }

    /**
     * Renders a label and a wrapped value side by side.
     *
     * @param label      label of the row
     * @param value      value, a dash if missing
     * @param labelWidth width reserved for the label
     */
    private void detailRow(String label, String value, float labelWidth) throws IOException {
        List<String> lines = wrap(orDash(value), regular, BODY_SIZE, width - labelWidth);
        float lineHeight = BODY_SIZE * LINE_SPACING;
        ensureSpace(lines.size() * lineHeight);
        drawText(label, bold, BODY_SIZE, MARGIN, y - BODY_SIZE, MUTED);
        for (String line : lines) {
            drawText(line, regular, BODY_SIZE, MARGIN + labelWidth, y - BODY_SIZE, TEXT);
            y -= lineHeight;
        }
    }

    /**
     * Renders wrapped text across the full width, continuing on a new page if needed.
     *
     * @param value text to render
     * @param font  font to use
     * @param size  font size
     * @param color text color
     */
    private void paragraph(String value, PDFont font, float size, float[] color)
            throws IOException {
        for (String line : wrap(value, font, size, width)) {
            text(line, font, size, MARGIN, color);
        }
    }

    /**
     * Renders one line of text below the cursor and moves the cursor down.
     *
     * @param value text to render
     * @param font  font to use
     * @param size  font size
     * @param x     left position
     * @param color text color
     */
    private void text(String value, PDFont font, float size, float x, float[] color)
            throws IOException {
        float lineHeight = size * LINE_SPACING;
        ensureSpace(lineHeight);
        drawText(value, font, size, x, y - size, color);
        y -= lineHeight;
    }

    /** Draws a horizontal rule across the full width at the cursor. */
    private void rule() throws IOException {
        strokeLine(MARGIN, y, MARGIN + width, y, RULE);
    }

    /**
     * Draws the header row of the variant table.
     *
     * @param headers column titles
     * @param height  height of the row
     */
    private void tableHeader(List<String> headers, float height) throws IOException {
        fillRect(MARGIN, y - height, width, height, HEADER_FILL);
        tableRow(headers, bold, height, null);
    }

    /**
     * Draws a row of the variant table and moves the cursor below it.
     *
     * @param cells    cell texts, one per column
     * @param font     font of the cells
     * @param height   height of the row
     * @param category category shown as a colored mark in the significance column, or
     *                 {@code null}
     */
    private void tableRow(List<String> cells, PDFont font, float height,
                          SignificanceCategory category) throws IOException {
        float x = MARGIN;
        float lineHeight = BODY_SIZE * LINE_SPACING;
        for (int column = 0; column < cells.size(); column++) {
            float columnWidth = width * TABLE_COLUMNS[column];
            float textX = x + CELL_PADDING;
            float firstBaseline = y - CELL_PADDING - BODY_SIZE;
            if (column == 2 && category != null) {
                fillRect(textX, firstBaseline, SWATCH_SIZE, SWATCH_SIZE, rgb(category.color()));
                textX += SWATCH_SIZE + 4;
            }
            float baseline = firstBaseline;
            for (String line : wrap(cells.get(column), font, BODY_SIZE,
                    cellTextWidth(column))) {
                drawText(line, font, BODY_SIZE, textX, baseline, TEXT);
                baseline -= lineHeight;
            }
            x += columnWidth;
        }
        y -= height;
        strokeLine(MARGIN, y, MARGIN + width, y, RULE);
    }

    /**
     * Computes the height of a table row from its tallest cell.
     *
     * @param cells cell texts, one per column
     * @param font  font of the cells
     * @return the row height
     */
    private float rowHeight(List<String> cells, PDFont font) throws IOException {
        int lines = 1;
        for (int column = 0; column < cells.size(); column++) {
            lines = Math.max(lines,
                    wrap(cells.get(column), font, BODY_SIZE, cellTextWidth(column)).size());
        }
        return lines * BODY_SIZE * LINE_SPACING + 2 * CELL_PADDING;
    }

    /**
     * Returns the width available for text in a column, leaving room for the colored mark of
     * the significance column.
     *
     * @param column column index
     * @return the text width
     */
    private float cellTextWidth(int column) {
        float available = width * TABLE_COLUMNS[column] - 2 * CELL_PADDING;
        return column == 2 ? available - SWATCH_SIZE - 4 : available;
    }

    // ---------------------------------------------------------------------
    // Drawing primitives
    // ---------------------------------------------------------------------

    /**
     * Draws a line of text at an absolute position.
     *
     * @param value    text to draw
     * @param font     font to use
     * @param size     font size
     * @param x        left position
     * @param baseline baseline position
     * @param color    text color
     */
    private void drawText(String value, PDFont font, float size, float x, float baseline,
                          float[] color) throws IOException {
        out.beginText();
        out.setFont(font, size);
        out.setNonStrokingColor(color[0], color[1], color[2]);
        out.newLineAtOffset(x, baseline);
        out.showText(encodable(value, font));
        out.endText();
    }

    /**
     * Fills a rectangle.
     *
     * @param x      left position
     * @param bottom bottom position
     * @param w      width
     * @param h      height
     * @param color  fill color
     */
    private void fillRect(float x, float bottom, float w, float h, float[] color)
            throws IOException {
        out.setNonStrokingColor(color[0], color[1], color[2]);
        out.addRect(x, bottom, w, h);
        out.fill();
    }

    /**
     * Draws a thin line.
     *
     * @param x1    start x
     * @param y1    start y
     * @param x2    end x
     * @param y2    end y
     * @param color line color
     */
    private void strokeLine(float x1, float y1, float x2, float y2, float[] color)
            throws IOException {
        out.setStrokingColor(color[0], color[1], color[2]);
        out.setLineWidth(0.5f);
        out.moveTo(x1, y1);
        out.lineTo(x2, y2);
        out.stroke();
    }

    // ---------------------------------------------------------------------
    // Text helpers
    // ---------------------------------------------------------------------

    /**
     * Splits text into lines that fit a width, breaking long words such as HGVS notations.
     *
     * @param value    text to wrap
     * @param font     font used to measure
     * @param size     font size
     * @param maxWidth available width
     * @return the lines, at least one
     */
    static List<String> wrap(String value, PDFont font, float size, float maxWidth)
            throws IOException {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : encodable(value, font).split(" ")) {
            if (word.isEmpty()) {
                continue;
            }
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (textWidth(candidate, font, size) <= maxWidth) {
                line.setLength(0);
                line.append(candidate);
                continue;
            }
            if (!line.isEmpty()) {
                lines.add(line.toString());
                line.setLength(0);
            }
            for (char c : word.toCharArray()) {
                if (!line.isEmpty() && textWidth(line.toString() + c, font, size) > maxWidth) {
                    lines.add(line.toString());
                    line.setLength(0);
                }
                line.append(c);
            }
        }
        if (!line.isEmpty() || lines.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }

    /**
     * Shortens text with an ellipsis so it fits a width.
     *
     * @param value    text to fit
     * @param font     font used to measure
     * @param size     font size
     * @param maxWidth available width
     * @return the text, shortened if needed
     */
    private static String fit(String value, PDFont font, float size, float maxWidth)
            throws IOException {
        String text = encodable(value, font);
        if (textWidth(text, font, size) <= maxWidth) {
            return text;
        }
        String ellipsis = "…";
        int end = text.length();
        while (end > 0 && textWidth(text.substring(0, end) + ellipsis, font, size) > maxWidth) {
            end--;
        }
        return text.substring(0, end) + ellipsis;
    }

    /**
     * Measures text.
     *
     * @param value text, already encodable
     * @param font  font used to measure
     * @param size  font size
     * @return the width in points
     */
    private static float textWidth(String value, PDFont font, float size) throws IOException {
        return font.getStringWidth(value) / 1000 * size;
    }

    /**
     * Makes text drawable with a standard font: line breaks and special spaces become plain
     * spaces, and characters the font cannot encode become {@code ?}.
     *
     * @param value text to clean, may be {@code null}
     * @param font  font that will draw the text
     * @return the cleaned text
     */
    static String encodable(String value, PDFont font) {
        if (value == null) {
            return "";
        }
        StringBuilder text = new StringBuilder(value.length());
        value.codePoints().forEach(codePoint -> {
            if (Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint)) {
                text.append(' ');
                return;
            }
            String character = Character.toString(codePoint);
            try {
                font.encode(character);
                text.append(character);
            } catch (IOException | IllegalArgumentException e) {
                text.append('?');
            }
        });
        return text.toString();
    }

    /**
     * Replaces a missing value with a dash.
     *
     * @param value value, may be {@code null}
     * @return the value, or a dash if it is missing or blank
     */
    private static String orDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    /**
     * Converts a CSS hexadecimal color to PDF color components.
     *
     * @param hex color such as {@code #4f46e5}
     * @return red, green and blue components between 0 and 1
     */
    private static float[] rgb(String hex) {
        int value = Integer.parseInt(hex.substring(1), 16);
        return new float[]{
                ((value >> 16) & 0xFF) / 255f, ((value >> 8) & 0xFF) / 255f, (value & 0xFF) / 255f};
    }
}
