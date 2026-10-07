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

package org.mygeneexplorer.controller;

import javafx.concurrent.Task;
import org.mygeneexplorer.AppInfo;
import org.mygeneexplorer.export.CsvExporter;
import org.mygeneexplorer.export.PdfReport;
import org.mygeneexplorer.export.ResultFiles;
import org.mygeneexplorer.i18n.ErrorMessages;
import org.mygeneexplorer.i18n.I18n;
import org.mygeneexplorer.i18n.Messages;
import org.mygeneexplorer.model.AppState;
import org.mygeneexplorer.model.ClinvarResult;
import org.mygeneexplorer.model.ClinvarVariant;
import org.mygeneexplorer.model.GeneInfo;
import org.mygeneexplorer.model.SearchResult;
import org.mygeneexplorer.model.StatusMessage;
import org.mygeneexplorer.prefs.UserPreferences;
import org.mygeneexplorer.service.GeneSymbols;
import org.mygeneexplorer.service.InvalidSymbolException;
import org.mygeneexplorer.service.MyGeneService;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

/**
 * Controller of the main window: handles the user's actions and updates the {@link AppState}.
 *
 * <p>It owns no JavaFX control, so it can be unit tested without a user interface. Settings
 * changed through it (text size, language, number of variants, recent searches) are persisted
 * in the {@link UserPreferences}.
 */
public final class MainController {

    private final AppState state;
    private final I18n i18n;
    private final UserPreferences preferences;
    private final MyGeneService service;
    private final ExecutorService executor;
    private Task<?> currentTask;

    /**
     * Creates the controller and initializes the state from the saved settings.
     *
     * @param state       state to update
     * @param i18n        current language
     * @param preferences saved settings
     * @param service     client of the BioThings APIs
     * @param executor    runs the requests in the background; shut down by {@link #shutdown()}
     */
    public MainController(AppState state, I18n i18n, UserPreferences preferences,
                          MyGeneService service, ExecutorService executor) {
        this.state = state;
        this.i18n = i18n;
        this.preferences = preferences;
        this.service = service;
        this.executor = executor;

        state.setFontSize(preferences.fontSize());
        state.setMaxVariants(preferences.maxVariants());
        state.setRecentSearches(preferences.recentSearches());
        state.setStatus(StatusMessage.info("status.ready"));
        state.queryProperty().addListener((obs, old, text) -> state.setQueryInvalid(false));
    }

    /**
     * Returns the localized column headers of the CSV export and of copied rows.
     *
     * @param m texts of the language to use
     * @return the {@value CsvExporter#COLUMN_COUNT} column headers
     */
    public static List<String> csvHeaders(Messages m) {
        return List.of(m.get("export.gene"), m.get("table.variantId"), m.get("table.hgvs"),
                m.get("table.significance"), m.get("table.origin"), m.get("export.clinvarUrl"));
    }

    // ---------------------------------------------------------------------
    // Search
    // ---------------------------------------------------------------------

    /**
     * Validates a gene symbol, then fetches the gene and its ClinVar variants in the background.
     *
     * <p>Any running search is cancelled. Invalid input is rejected before any request is sent.
     *
     * @param input text typed or chosen by the user
     */
    public void search(String input) {
        state.setQuery(input);
        cancelSearch();

        String symbol;
        try {
            symbol = GeneSymbols.normalize(input);
        } catch (InvalidSymbolException ex) {
            state.setQueryInvalid(true);
            reportError(ex);
            return;
        }

        int size = state.getMaxVariants();
        state.setResult(null);
        state.setLoading(true);
        state.setStatus(StatusMessage.info("status.queryingGene", symbol));

        Task<Optional<GeneInfo>> geneTask = service.createGeneTask(symbol);
        geneTask.setOnSucceeded(e -> onGeneReceived(symbol, size, geneTask.getValue()));
        geneTask.setOnFailed(e -> onSearchFailed(geneTask.getException()));
        run(geneTask);
    }

    /** Cancels the running search, if any. */
    public void cancelSearch() {
        if (currentTask != null) {
            currentTask.cancel();
            currentTask = null;
        }
        state.setLoading(false);
    }

    /**
     * Continues a search once the gene is known: fetches its ClinVar variants.
     *
     * @param query  validated symbol typed by the user
     * @param size   maximum number of variants to fetch
     * @param gene   gene found, empty if the symbol is unknown
     */
    private void onGeneReceived(String query, int size, Optional<GeneInfo> gene) {
        if (gene.isEmpty()) {
            state.setLoading(false);
            state.setQueryInvalid(true);
            state.setStatus(StatusMessage.error("error.geneNotFound", query));
            return;
        }
        GeneInfo found = gene.get();
        state.setStatus(StatusMessage.info("status.queryingVariants", found.symbol()));
        Task<ClinvarResult> clinvarTask = service.createClinvarTask(found.symbol(), size);
        clinvarTask.setOnSucceeded(e -> onClinvarReceived(query, found, clinvarTask.getValue()));
        clinvarTask.setOnFailed(e -> onSearchFailed(clinvarTask.getException()));
        run(clinvarTask);
    }

    /**
     * Completes a search: publishes the result and records it in the recent searches.
     *
     * @param query   validated symbol typed by the user
     * @param gene    gene found
     * @param clinvar its ClinVar variants
     */
    private void onClinvarReceived(String query, GeneInfo gene, ClinvarResult clinvar) {
        currentTask = null;
        state.setLoading(false);
        state.setResult(new SearchResult(query, gene, clinvar, Instant.now()));
        preferences.addRecentSearch(gene.symbol());
        state.setRecentSearches(preferences.recentSearches());
        state.setStatus(StatusMessage.info("status.found", clinvar.variants().size(),
                clinvar.total(), gene.symbol()));
    }

    /**
     * Ends a search that failed, reporting the cause in the status bar.
     *
     * @param error cause of the failure
     */
    private void onSearchFailed(Throwable error) {
        currentTask = null;
        state.setLoading(false);
        reportError(error);
    }

    /**
     * Runs a request in the background and remembers it so it can be cancelled.
     *
     * @param task request to run
     */
    private void run(Task<?> task) {
        currentTask = task;
        executor.execute(task);
    }

    // ---------------------------------------------------------------------
    // Settings
    // ---------------------------------------------------------------------

    /**
     * Changes the base text size of the interface, within the supported range.
     *
     * @param size requested size, in pixels
     */
    public void setFontSize(int size) {
        int clamped = UserPreferences.clampFontSize(size);
        state.setFontSize(clamped);
        preferences.setFontSize(clamped);
    }

    /** Enlarges the text by one pixel. */
    public void increaseFontSize() {
        setFontSize(state.getFontSize() + 1);
    }

    /** Reduces the text by one pixel. */
    public void decreaseFontSize() {
        setFontSize(state.getFontSize() - 1);
    }

    /** Restores the default text size. */
    public void resetFontSize() {
        setFontSize(UserPreferences.DEFAULT_FONT_SIZE);
    }

    /**
     * Switches the language of the interface; unsupported languages fall back to English.
     *
     * @param locale requested language
     */
    public void setLanguage(Locale locale) {
        i18n.setLocale(locale);
        preferences.setLocale(locale);
    }

    /**
     * Changes the maximum number of variants fetched by the next searches.
     *
     * @param value one of {@link UserPreferences#MAX_VARIANTS_CHOICES}
     * @throws IllegalArgumentException if the value is not one of the choices
     */
    public void setMaxVariants(int value) {
        preferences.setMaxVariants(value);
        state.setMaxVariants(value);
    }

    /** Empties the recent searches. */
    public void clearRecentSearches() {
        preferences.clearRecentSearches();
        state.setRecentSearches(List.of());
    }

    /**
     * Reports in the status bar that variants were copied to the clipboard.
     *
     * @param count number of variants copied
     */
    public void notifyCopied(int count) {
        state.setStatus(StatusMessage.info("status.copied", count));
    }

    // ---------------------------------------------------------------------
    // Files
    // ---------------------------------------------------------------------

    /**
     * Exports variants of the current result as CSV.
     *
     * @param file         destination file
     * @param rows         variants to export, in display order
     * @param formatLocale locale whose spreadsheet conventions select the separator
     * @return {@code true} on success; on failure the status bar reports the error
     */
    public boolean exportCsv(Path file, List<ClinvarVariant> rows, Locale formatLocale) {
        SearchResult current = state.getResult();
        if (current == null) {
            return false;
        }
        rememberDirectory(file);
        try {
            CsvExporter.write(file, current.gene().symbol(), rows,
                    csvHeaders(i18n.messages()), CsvExporter.separatorFor(formatLocale));
            state.setStatus(StatusMessage.info("status.exported", rows.size(), fileName(file)));
            return true;
        } catch (IOException ex) {
            state.setStatus(StatusMessage.error("error.file.write", fileName(file)));
            return false;
        }
    }

    /**
     * Exports the complete current result as a JSON document that can be opened again.
     *
     * @param file destination file
     * @return {@code true} on success; on failure the status bar reports the error
     */
    public boolean exportJson(Path file) {
        SearchResult current = state.getResult();
        if (current == null) {
            return false;
        }
        rememberDirectory(file);
        try {
            ResultFiles.save(current, file, AppInfo.version());
            state.setStatus(StatusMessage.info("status.saved", fileName(file)));
            return true;
        } catch (IOException ex) {
            state.setStatus(StatusMessage.error("error.file.write", fileName(file)));
            return false;
        }
    }

    /**
     * Exports the current result as a PDF report in the current language.
     *
     * @param file         destination file
     * @param rows         variants to list, in display order
     * @param formatLocale locale whose conventions select the paper size
     * @return {@code true} on success; on failure the status bar reports the error
     */
    public boolean exportPdf(Path file, List<ClinvarVariant> rows, Locale formatLocale) {
        SearchResult current = state.getResult();
        if (current == null) {
            return false;
        }
        rememberDirectory(file);
        try {
            PdfReport.write(file, current, rows, i18n.messages(), AppInfo.version(),
                    formatLocale, Instant.now());
            state.setStatus(StatusMessage.info("status.exportedPdf", fileName(file)));
            return true;
        } catch (IOException ex) {
            state.setStatus(StatusMessage.error("error.file.write", fileName(file)));
            return false;
        }
    }

    /**
     * Opens results previously exported as JSON, replacing the current result.
     *
     * @param file file to open
     * @return {@code true} on success; on failure the current result is kept and the status
     *         bar reports the error
     */
    public boolean openResults(Path file) {
        rememberDirectory(file);
        try {
            SearchResult loaded = ResultFiles.load(file);
            cancelSearch();
            state.setQuery(loaded.query());
            state.setResult(loaded);
            state.setStatus(StatusMessage.info(m -> m.get("status.loaded", fileName(file),
                    m.formatDateTime(loaded.retrievedAt()))));
            return true;
        } catch (IOException ex) {
            reportError(ex);
            return false;
        }
    }

    /**
     * Suggests a file name for exporting the current result.
     *
     * @param extension file extension, without the dot
     * @return e.g. {@code BRCA1_clinvar_2026-10-06.csv}, or {@code null} without a result
     */
    public String suggestedFileName(String extension) {
        SearchResult current = state.getResult();
        if (current == null) {
            return null;
        }
        LocalDate date = LocalDate.ofInstant(current.retrievedAt(), ZoneId.systemDefault());
        return current.gene().symbol() + "_clinvar_" + date + "." + extension;
    }

    /** @return the directory of the last file opened or exported, if it still exists */
    public Optional<Path> lastDirectory() {
        return preferences.lastDirectory();
    }

    /**
     * Remembers the directory of a file so the next file dialog opens there.
     *
     * @param file file chosen by the user
     */
    private void rememberDirectory(Path file) {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            preferences.setLastDirectory(parent);
        }
    }

    /**
     * Returns the name of a file, without its directory, for messages.
     *
     * @param file a file path
     * @return its last element
     */
    private static String fileName(Path file) {
        Path name = file.getFileName();
        return name == null ? file.toString() : name.toString();
    }

    // ---------------------------------------------------------------------
    // Lifecycle
    // ---------------------------------------------------------------------

    /**
     * Remembers the size of the main window for the next session.
     *
     * @param width     width of the window content
     * @param height    height of the window content
     * @param maximized whether the window is maximized
     */
    public void saveWindowState(double width, double height, boolean maximized) {
        preferences.setWindowState(width, height, maximized);
    }

    /** Cancels pending requests, stops the background threads and saves the settings. */
    public void shutdown() {
        cancelSearch();
        executor.shutdownNow();
        preferences.flush();
    }

    /**
     * Shows an error in the status bar, translated in the current language.
     *
     * @param error error to describe
     */
    private void reportError(Throwable error) {
        state.setStatus(StatusMessage.error(m -> ErrorMessages.describe(error, m)));
    }
}
