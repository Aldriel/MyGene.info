package org.mygeneexplorer.controller;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mygeneexplorer.i18n.I18n;
import org.mygeneexplorer.i18n.Messages;
import org.mygeneexplorer.model.AppState;
import org.mygeneexplorer.model.ClinvarResult;
import org.mygeneexplorer.model.ClinvarVariant;
import org.mygeneexplorer.model.GeneInfo;
import org.mygeneexplorer.model.SearchResult;
import org.mygeneexplorer.prefs.UserPreferences;
import org.mygeneexplorer.service.MyGeneService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests of the controller logic that needs no network and no JavaFX toolkit. */
class MainControllerTest {

    private static final String TEST_ROOT = "org/mygeneexplorer-test-controller";
    private static final Messages ENGLISH = Messages.forLocale(Locale.ENGLISH);

    @TempDir
    Path tempDir;

    private Preferences node;
    private UserPreferences preferences;
    private ExecutorService executor;
    private AppState state;
    private I18n i18n;
    private MainController controller;

    @BeforeEach
    void createController() {
        node = Preferences.userRoot().node(TEST_ROOT + "/" + UUID.randomUUID());
        preferences = new UserPreferences(node);
        preferences.setFontSize(16);
        preferences.setMaxVariants(250);
        preferences.addRecentSearch("TP53");
        preferences.addRecentSearch("BRCA1");
        executor = Executors.newSingleThreadExecutor();
        state = new AppState();
        i18n = new I18n(Locale.ENGLISH);
        controller = new MainController(state, i18n, preferences, new MyGeneService(), executor);
    }

    @AfterEach
    void removeNode() throws BackingStoreException {
        executor.shutdownNow();
        node.removeNode();
    }

    @AfterAll
    static void removeTestRoot() throws BackingStoreException {
        Preferences.userRoot().node(TEST_ROOT).removeNode();
    }

    @Test
    void initializesStateFromPreferences() {
        assertEquals(16, state.getFontSize());
        assertEquals(250, state.getMaxVariants());
        assertEquals(List.of("BRCA1", "TP53"), state.getRecentSearches());
        assertEquals("Ready.", status());
        assertFalse(state.getStatus().error());
        assertNull(state.getResult());
    }

    @Test
    void rejectsInvalidSymbolWithoutSendingRequest() {
        controller.search("BRCA*");

        assertTrue(state.isQueryInvalid());
        assertFalse(state.isLoading());
        assertTrue(state.getStatus().error());
        assertTrue(status().startsWith("\"BRCA*\" is not a valid gene symbol"), status());
        assertEquals("BRCA*", state.getQuery());
    }

    @Test
    void rejectsEmptySymbol() {
        controller.search("   ");

        assertTrue(state.isQueryInvalid());
        assertEquals("Please enter a gene symbol.", status());
    }

    @Test
    void editingTheQueryClearsTheErrorHighlight() {
        controller.search("???");
        assertTrue(state.isQueryInvalid());

        state.setQuery("BRCA");

        assertFalse(state.isQueryInvalid());
    }

    @Test
    void invalidSearchKeepsThePreviousResult() {
        SearchResult previous = sampleResult();
        state.setResult(previous);

        controller.search("not a gene");

        assertSame(previous, state.getResult());
    }

    @Test
    void changesAndPersistsFontSizeWithinBounds() {
        controller.increaseFontSize();
        assertEquals(17, state.getFontSize());
        assertEquals(17, preferences.fontSize());

        controller.decreaseFontSize();
        controller.decreaseFontSize();
        assertEquals(15, state.getFontSize());

        controller.setFontSize(500);
        assertEquals(UserPreferences.MAX_FONT_SIZE, state.getFontSize());
        controller.setFontSize(1);
        assertEquals(UserPreferences.MIN_FONT_SIZE, state.getFontSize());

        controller.resetFontSize();
        assertEquals(UserPreferences.DEFAULT_FONT_SIZE, state.getFontSize());
        assertEquals(UserPreferences.DEFAULT_FONT_SIZE, preferences.fontSize());
    }

    @Test
    void switchesAndPersistsLanguage() {
        controller.setLanguage(Locale.FRENCH);

        assertEquals(Locale.FRENCH, i18n.locale());
        assertEquals(Locale.FRENCH, preferences.locale());
        assertEquals("Prêt.", status());
    }

    @Test
    void statusIsTranslatedAgainWhenLanguageChanges() {
        controller.search("");
        assertEquals("Please enter a gene symbol.", status());

        controller.setLanguage(Locale.FRENCH);

        assertEquals(Messages.forLocale(Locale.FRENCH).get("error.symbol.empty"), status());
    }

    @Test
    void changesAndPersistsMaxVariants() {
        controller.setMaxVariants(1000);

        assertEquals(1000, state.getMaxVariants());
        assertEquals(1000, preferences.maxVariants());
        assertThrows(IllegalArgumentException.class, () -> controller.setMaxVariants(42));
        assertEquals(1000, state.getMaxVariants());
    }

    @Test
    void clearsRecentSearches() {
        controller.clearRecentSearches();

        assertTrue(state.getRecentSearches().isEmpty());
        assertTrue(preferences.recentSearches().isEmpty());
    }

    @Test
    void reportsCopiedVariants() {
        controller.notifyCopied(3);

        assertEquals("3 variants copied to the clipboard.", status());
    }

    @Test
    void exportsNothingWithoutResult() {
        assertFalse(controller.exportCsv(tempDir.resolve("a.csv"), List.of(), Locale.US));
        assertFalse(controller.exportJson(tempDir.resolve("a.json")));
        assertNull(controller.suggestedFileName("csv"));
        assertFalse(Files.exists(tempDir.resolve("a.csv")));
    }

    @Test
    void exportsCsvWithLocalizedHeadersAndSeparator() throws IOException {
        SearchResult result = sampleResult();
        state.setResult(result);
        controller.setLanguage(Locale.FRENCH);
        Path file = tempDir.resolve("brca1.csv");

        assertTrue(controller.exportCsv(file, result.clinvar().variants(), Locale.FRANCE));

        String csv = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(csv.startsWith("\uFEFFGène;"), csv);
        assertTrue(csv.contains("BRCA1;55555;"), csv);
        assertFalse(state.getStatus().error());
        assertTrue(status().contains("brca1.csv"), status());
        assertEquals(Optional.of(tempDir.toAbsolutePath()), controller.lastDirectory());
    }

    @Test
    void exportsPdfReport() throws IOException {
        SearchResult result = sampleResult();
        state.setResult(result);
        Path file = tempDir.resolve("brca1.pdf");

        assertTrue(controller.exportPdf(file, result.clinvar().variants(), Locale.US));

        byte[] content = Files.readAllBytes(file);
        assertEquals("%PDF", new String(content, 0, 4, StandardCharsets.US_ASCII));
        assertEquals("Report exported to brca1.pdf.", status());
    }

    @Test
    void reportsPdfFailures() {
        assertFalse(controller.exportPdf(tempDir.resolve("none.pdf"), List.of(), Locale.US));

        state.setResult(sampleResult());
        assertFalse(controller.exportPdf(tempDir, List.of(), Locale.US));
        assertTrue(state.getStatus().error());
    }

    @Test
    void reportsCsvWriteFailure() {
        state.setResult(sampleResult());

        assertFalse(controller.exportCsv(tempDir, List.of(), Locale.US));

        assertTrue(state.getStatus().error());
        assertTrue(status().startsWith("Unable to write"), status());
    }

    @Test
    void exportsJsonThatCanBeOpenedAgain() {
        SearchResult result = sampleResult();
        state.setResult(result);
        Path file = tempDir.resolve("brca1.json");

        assertTrue(controller.exportJson(file));
        assertEquals("Results exported to brca1.json.", status());

        AppState otherState = new AppState();
        MainController other = new MainController(otherState, new I18n(Locale.ENGLISH),
                preferences, new MyGeneService(), executor);
        assertTrue(other.openResults(file));

        assertEquals(result, otherState.getResult());
        assertEquals("brca1", otherState.getQuery());
        assertFalse(otherState.getStatus().error());
        assertTrue(otherState.getStatus().render(ENGLISH).startsWith("Results opened from brca1.json"));
    }

    @Test
    void reportsJsonWriteFailure() {
        state.setResult(sampleResult());

        assertFalse(controller.exportJson(tempDir));

        assertTrue(state.getStatus().error());
    }

    @Test
    void rejectsInvalidFileAndKeepsCurrentResult() throws IOException {
        SearchResult current = sampleResult();
        state.setResult(current);
        Path file = Files.writeString(tempDir.resolve("notes.json"), "{\"hello\": 1}");

        assertFalse(controller.openResults(file));

        assertSame(current, state.getResult());
        assertTrue(state.getStatus().error());
        assertEquals("notes.json is not a valid MyGene Explorer results file.", status());
    }

    @Test
    void suggestsFileNameFromGeneAndDate() {
        state.setResult(sampleResult());

        String name = controller.suggestedFileName("csv");

        assertTrue(name.matches("BRCA1_clinvar_\\d{4}-\\d{2}-\\d{2}\\.csv"), name);
    }

    @Test
    void savesWindowStateAndShutsDown() {
        controller.saveWindowState(900, 600, false);
        controller.shutdown();

        assertEquals(900, preferences.windowWidth());
        assertEquals(600, preferences.windowHeight());
        assertTrue(executor.isShutdown());
        assertFalse(state.isLoading());
    }

    @Test
    void providesSixLocalizedCsvHeaders() {
        List<String> headers = MainController.csvHeaders(ENGLISH);

        assertEquals(List.of("Gene", "Variant ID", "HGVS", "Clinical significance", "Origin",
                "ClinVar URL"), headers);
    }

    private String status() {
        return state.getStatus().render(i18n.messages());
    }

    private static SearchResult sampleResult() {
        GeneInfo gene = new GeneInfo("672", "BRCA1", "BRCA1 DNA repair associated", 672L,
                "Summary.", "protein-coding", "17q21.31", List.of("RNF53"));
        ClinvarVariant variant = new ClinvarVariant("chr17:g.43045712G>A", 55555L,
                List.of("Pathogenic"), List.of("germline"));
        return new SearchResult("brca1", gene, new ClinvarResult(1, List.of(variant)),
                Instant.parse("2026-10-06T12:00:00Z"));
    }
}
