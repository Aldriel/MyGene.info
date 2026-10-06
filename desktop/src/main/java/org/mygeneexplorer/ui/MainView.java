package org.mygeneexplorer.ui;

import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.ListChangeListener;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.mygeneexplorer.AppInfo;
import org.mygeneexplorer.controller.MainController;
import org.mygeneexplorer.i18n.I18n;
import org.mygeneexplorer.i18n.Messages;
import org.mygeneexplorer.model.AppState;
import org.mygeneexplorer.model.SearchResult;
import org.mygeneexplorer.model.StatusMessage;
import org.mygeneexplorer.prefs.UserPreferences;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * View of the main window: menus, search bar, gene card, variant table, summary chart and
 * status bar.
 *
 * <p>It displays the {@link AppState} through bindings and forwards every user action to the
 * {@link MainController}. The text size is applied as {@code -fx-font-size} on the scene root;
 * the stylesheet sizes everything in {@code em}, so the whole interface scales with it.
 */
public final class MainView {

    private static final List<Integer> TEXT_SIZES = List.of(12, 14, 16, 20);
    private static final List<String> TEXT_SIZE_KEYS = List.of(
            "menu.options.textSize.small", "menu.options.textSize.normal",
            "menu.options.textSize.large", "menu.options.textSize.extraLarge");
    private static final List<String> EXAMPLES = List.of("BRCA1", "TP53", "CFTR", "HLA-A");
    private static final List<Integer> ICON_SIZES = List.of(16, 32, 48, 256);
    private static final PseudoClass ERROR = PseudoClass.getPseudoClass("error");
    private static final String STYLESHEET = Objects.requireNonNull(
            MainView.class.getResource("app.css")).toExternalForm();

    private final Stage stage;
    private final HostServices hostServices;
    private final AppState state;
    private final I18n i18n;
    private final MainController controller;

    private final BorderPane root = new BorderPane();
    private final StackPane content = new StackPane();
    private final TextField queryField = new TextField();
    private final Menu recentMenu = new Menu();
    private final ToggleGroup textSizeGroup = new ToggleGroup();

    private final GeneCard geneCard;
    private final VariantTablePane variantPane;
    private final SummaryPane summaryPane;
    private final Node welcomeView;
    private final Node resultsView;
    private final Dialogs dialogs;

    /**
     * Builds the window content and binds it to the state.
     *
     * @param stage        primary stage of the application
     * @param hostServices opens web pages and e-mail links
     * @param state        state to display
     * @param i18n         current language
     * @param controller   receives the user's actions
     */
    public MainView(Stage stage, HostServices hostServices, AppState state, I18n i18n,
                    MainController controller) {
        this.stage = stage;
        this.hostServices = hostServices;
        this.state = state;
        this.i18n = i18n;
        this.controller = controller;
        this.dialogs = new Dialogs(stage, root, STYLESHEET);

        geneCard = new GeneCard(i18n, this::openUrl);
        variantPane = new VariantTablePane(i18n, this::openUrl, controller::notifyCopied);
        summaryPane = new SummaryPane(i18n);
        welcomeView = buildWelcome();
        resultsView = buildResults();

        root.getStyleClass().add("app-root");
        root.setTop(new VBox(buildMenuBar(), buildHeader()));
        root.setCenter(content);
        root.setBottom(buildStatusBar());

        bindFontSize();
        state.resultProperty().addListener((obs, old, value) -> showResult(value));
        showResult(state.getResult());
    }

    /**
     * Shows the window.
     *
     * @param width     initial width of the content
     * @param height    initial height of the content
     * @param maximized whether to maximize the window
     */
    public void show(double width, double height, boolean maximized) {
        Scene scene = new Scene(root, width, height);
        scene.getStylesheets().add(STYLESHEET);
        scene.getAccelerators().put(shortcut(KeyCode.ADD), controller::increaseFontSize);
        scene.getAccelerators().put(shortcut(KeyCode.PLUS), controller::increaseFontSize);
        scene.getAccelerators().put(shortcut(KeyCode.SUBTRACT), controller::decreaseFontSize);
        scene.getAccelerators().put(shortcut(KeyCode.NUMPAD0), controller::resetFontSize);

        stage.setTitle(AppInfo.NAME);
        ICON_SIZES.forEach(size -> stage.getIcons().add(new Image(Objects.requireNonNull(
                MainView.class.getResource("icon-" + size + ".png")).toExternalForm())));
        stage.setMinWidth(760);
        stage.setMinHeight(520);
        stage.setScene(scene);
        stage.setMaximized(maximized);
        stage.show();
        queryField.requestFocus();
    }

    /** Hands the current window size to the controller so it is restored next time. */
    public void saveWindowState() {
        Scene scene = stage.getScene();
        if (scene != null) {
            controller.saveWindowState(scene.getWidth(), scene.getHeight(), stage.isMaximized());
        }
    }

    // ---------------------------------------------------------------------
    // Layout
    // ---------------------------------------------------------------------

    /**
     * Applies the text size of the state to the scene root and keeps the menu in sync.
     */
    private void bindFontSize() {
        root.styleProperty().bind(
                Bindings.format("-fx-font-size: %dpx;", state.fontSizeProperty()));
        state.fontSizeProperty().addListener((obs, old, size) ->
                selectToggle(textSizeGroup, Integer.valueOf(size.intValue())::equals));
        selectToggle(textSizeGroup, Integer.valueOf(state.getFontSize())::equals);
    }

    /**
     * Builds the header: title, search field, number of variants, search button and progress.
     *
     * @return the header node
     */
    private Node buildHeader() {
        Label title = new Label(AppInfo.NAME);
        title.getStyleClass().add("app-title");
        Label subtitle = boundLabel("app.subtitle");
        subtitle.getStyleClass().add("muted");

        queryField.promptTextProperty().bind(i18n.text("search.prompt"));
        queryField.setPrefColumnCount(24);
        queryField.textProperty().bindBidirectional(state.queryProperty());
        state.queryInvalidProperty().addListener((obs, old, invalid) ->
                queryField.pseudoClassStateChanged(ERROR, invalid));
        HBox.setHgrow(queryField, Priority.ALWAYS);

        ComboBox<Integer> maxVariantsBox = new ComboBox<>();
        Label maxLabel = boundLabel("search.maxVariants");
        maxLabel.setLabelFor(maxVariantsBox);
        maxVariantsBox.getItems().setAll(UserPreferences.MAX_VARIANTS_CHOICES);
        maxVariantsBox.setValue(state.getMaxVariants());
        maxVariantsBox.valueProperty().addListener((obs, old, value) -> {
            if (value != null) {
                controller.setMaxVariants(value);
            }
        });

        Button searchButton = new Button();
        searchButton.textProperty().bind(i18n.text("search.button"));
        searchButton.getStyleClass().add("primary-button");
        searchButton.setDefaultButton(true);
        searchButton.disableProperty().bind(queryField.textProperty().isEmpty());
        searchButton.setOnAction(e -> controller.search(queryField.getText()));

        ProgressIndicator progress = new ProgressIndicator();
        progress.setPrefSize(24, 24);
        progress.visibleProperty().bind(state.loadingProperty());

        HBox searchBar = new HBox(queryField, maxLabel, maxVariantsBox, searchButton, progress);
        searchBar.getStyleClass().add("search-bar");

        VBox header = new VBox(title, subtitle, searchBar);
        header.getStyleClass().add("header");
        return header;
    }

    /**
     * Builds the welcome card shown before the first search.
     *
     * @return the welcome node
     */
    private Node buildWelcome() {
        Label heading = boundLabel("welcome.title");
        heading.getStyleClass().add("welcome-title");
        Label text = boundLabel("welcome.text");
        text.setWrapText(true);

        HBox examples = new HBox(6, boundLabel("welcome.examples"));
        examples.setAlignment(Pos.CENTER_LEFT);
        for (String symbol : EXAMPLES) {
            Hyperlink link = new Hyperlink(symbol);
            link.setOnAction(e -> controller.search(symbol));
            examples.getChildren().add(link);
        }

        Label disclaimer = boundLabel("app.disclaimer");
        disclaimer.setWrapText(true);
        disclaimer.getStyleClass().addAll("muted", "small");

        VBox card = new VBox(heading, text, examples, disclaimer);
        card.getStyleClass().addAll("card", "welcome");
        card.setMaxHeight(Region.USE_PREF_SIZE);
        StackPane wrapper = new StackPane(card);
        wrapper.setAlignment(Pos.TOP_CENTER);
        wrapper.setPadding(new Insets(40, 24, 24, 24));
        return wrapper;
    }

    /**
     * Builds the results view: gene card above the Variants and Summary tabs.
     *
     * @return the results node
     */
    private Node buildResults() {
        Tab variantsTab = new Tab(null, variantPane);
        variantsTab.textProperty().bind(i18n.text("tab.variants"));
        Tab summaryTab = new Tab(null, summaryPane);
        summaryTab.textProperty().bind(i18n.text("tab.summary"));
        TabPane tabs = new TabPane(variantsTab, summaryTab);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabs, Priority.ALWAYS);

        VBox box = new VBox(geneCard, tabs);
        box.getStyleClass().add("content");
        return box;
    }

    /**
     * Builds the status bar: current message on the left, data sources or retrieval date on
     * the right. Both are translated again when the language changes.
     *
     * @return the status bar node
     */
    private Node buildStatusBar() {
        Label statusLabel = new Label();
        statusLabel.getStyleClass().add("status-label");
        statusLabel.textProperty().bind(Bindings.createStringBinding(
                () -> state.getStatus().render(i18n.messages()),
                state.statusProperty(), i18n.messagesProperty()));
        state.statusProperty().addListener((obs, old, status) ->
                statusLabel.pseudoClassStateChanged(ERROR, status.error()));
        statusLabel.pseudoClassStateChanged(ERROR, state.getStatus().error());

        Label sourceLabel = new Label();
        sourceLabel.getStyleClass().add("muted");
        sourceLabel.textProperty().bind(Bindings.createStringBinding(() -> {
            Messages m = i18n.messages();
            SearchResult current = state.getResult();
            return current == null
                    ? m.get("status.sources")
                    : m.get("status.retrievedAt", m.formatDateTime(current.retrievedAt()));
        }, state.resultProperty(), i18n.messagesProperty()));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bar = new HBox(statusLabel, spacer, sourceLabel);
        bar.getStyleClass().add("status-bar");
        return bar;
    }

    /**
     * Shows the result, or the welcome card when there is none.
     *
     * @param value result to show, may be {@code null}
     */
    private void showResult(SearchResult value) {
        if (value == null) {
            content.getChildren().setAll(welcomeView);
            return;
        }
        geneCard.show(value.gene());
        variantPane.setVariants(value.gene().symbol(), value.clinvar().variants(),
                value.clinvar().total());
        summaryPane.setVariants(value.clinvar().variants(), value.clinvar().total());
        content.getChildren().setAll(resultsView);
    }

    // ---------------------------------------------------------------------
    // Menus
    // ---------------------------------------------------------------------

    /**
     * Builds the menu bar: File, Edit, Options and Help.
     *
     * @return the menu bar
     */
    private MenuBar buildMenuBar() {
        return new MenuBar(buildFileMenu(), buildEditMenu(), buildOptionsMenu(), buildHelpMenu());
    }

    /**
     * Builds the File menu: open, CSV, JSON and PDF exports, recent searches and quit.
     *
     * @return the File menu
     */
    private Menu buildFileMenu() {
        BooleanBinding noResult = state.resultProperty().isNull();
        MenuItem open = menuItem("menu.file.open", shortcut(KeyCode.O), this::openResults);
        MenuItem exportCsv = menuItem("menu.file.exportCsv", shortcut(KeyCode.E), this::exportCsv);
        exportCsv.disableProperty().bind(noResult);
        MenuItem exportJson = menuItem("menu.file.exportJson", shortcut(KeyCode.S),
                this::exportJson);
        exportJson.disableProperty().bind(noResult);
        MenuItem exportPdf = menuItem("menu.file.exportPdf", shortcut(KeyCode.P), this::exportPdf);
        exportPdf.disableProperty().bind(noResult);

        recentMenu.textProperty().bind(i18n.text("menu.file.recent"));
        state.getRecentSearches().addListener(
                (ListChangeListener<String>) change -> refreshRecentMenu());
        refreshRecentMenu();

        MenuItem quit = menuItem("menu.file.quit", shortcut(KeyCode.Q), Platform::exit);
        return menu("menu.file", open, new SeparatorMenuItem(), exportCsv, exportJson, exportPdf,
                new SeparatorMenuItem(), recentMenu, new SeparatorMenuItem(), quit);
    }

    /**
     * Builds the Edit menu: new search, filter and copy.
     *
     * @return the Edit menu
     */
    private Menu buildEditMenu() {
        BooleanBinding noResult = state.resultProperty().isNull();
        MenuItem newSearch = menuItem("menu.edit.newSearch", shortcut(KeyCode.L), () -> {
            queryField.requestFocus();
            queryField.selectAll();
        });
        MenuItem filter = menuItem("menu.edit.filter", shortcut(KeyCode.F),
                variantPane::focusFilter);
        filter.disableProperty().bind(noResult);
        // No accelerator: Ctrl+C must keep copying text in text fields.
        MenuItem copy = menuItem("menu.edit.copy", null, variantPane::copySelection);
        copy.disableProperty().bind(noResult);
        return menu("menu.edit", newSearch, filter, copy);
    }

    /**
     * Builds the Options menu: language and text size.
     *
     * @return the Options menu
     */
    private Menu buildOptionsMenu() {
        return menu("menu.options", buildLanguageMenu(), buildTextSizeMenu());
    }

    /**
     * Builds the Help menu: keyboard shortcuts, links to the data sources and About.
     *
     * @return the Help menu
     */
    private Menu buildHelpMenu() {
        MenuItem shortcuts = menuItem("menu.help.shortcuts", null,
                () -> dialogs.showShortcuts(i18n.messages()));
        MenuItem mygene = linkItem("MyGene.info", "https://mygene.info");
        MenuItem myvariant = linkItem("MyVariant.info", "https://myvariant.info");
        MenuItem clinvar = linkItem("ClinVar (NCBI)", "https://www.ncbi.nlm.nih.gov/clinvar/");
        MenuItem about = menuItem("menu.help.about", null,
                () -> dialogs.showAbout(i18n.messages(), this::openUrl));
        return menu("menu.help", shortcuts, new SeparatorMenuItem(), mygene, myvariant, clinvar,
                new SeparatorMenuItem(), about);
    }

    /**
     * Builds the Language submenu. Each language is named in itself ("English", "Français")
     * so users can find theirs whatever the current language.
     *
     * @return the Language submenu
     */
    private Menu buildLanguageMenu() {
        Menu menu = new Menu();
        menu.textProperty().bind(i18n.text("menu.options.language"));
        ToggleGroup group = new ToggleGroup();
        for (Locale locale : Messages.SUPPORTED_LOCALES) {
            String name = locale.getDisplayLanguage(locale);
            RadioMenuItem item = new RadioMenuItem(
                    name.substring(0, 1).toUpperCase(locale) + name.substring(1));
            item.setUserData(locale);
            item.setToggleGroup(group);
            item.setOnAction(e -> controller.setLanguage(locale));
            menu.getItems().add(item);
        }
        selectToggle(group, i18n.locale()::equals);
        return menu;
    }

    /**
     * Builds the Text size submenu: preset sizes, zoom in, zoom out and reset.
     *
     * @return the Text size submenu
     */
    private Menu buildTextSizeMenu() {
        Menu menu = new Menu();
        menu.textProperty().bind(i18n.text("menu.options.textSize"));
        for (int i = 0; i < TEXT_SIZES.size(); i++) {
            int size = TEXT_SIZES.get(i);
            RadioMenuItem item = new RadioMenuItem();
            item.textProperty().bind(i18n.text(TEXT_SIZE_KEYS.get(i)));
            item.setUserData(size);
            item.setToggleGroup(textSizeGroup);
            item.setOnAction(e -> controller.setFontSize(size));
            menu.getItems().add(item);
        }
        menu.getItems().addAll(new SeparatorMenuItem(),
                menuItem("menu.options.zoomIn", shortcut(KeyCode.EQUALS),
                        controller::increaseFontSize),
                menuItem("menu.options.zoomOut", shortcut(KeyCode.MINUS),
                        controller::decreaseFontSize),
                menuItem("menu.options.zoomReset", shortcut(KeyCode.DIGIT0),
                        controller::resetFontSize));
        return menu;
    }

    /** Rebuilds the Recent searches submenu from the state. */
    private void refreshRecentMenu() {
        recentMenu.getItems().clear();
        List<String> recent = state.getRecentSearches();
        if (recent.isEmpty()) {
            MenuItem none = menuItem("menu.file.recent.empty", null, () -> { });
            none.setDisable(true);
            recentMenu.getItems().add(none);
            return;
        }
        for (String symbol : recent) {
            MenuItem item = new MenuItem(symbol);
            item.setMnemonicParsing(false);
            item.setOnAction(e -> controller.search(symbol));
            recentMenu.getItems().add(item);
        }
        recentMenu.getItems().addAll(new SeparatorMenuItem(),
                menuItem("menu.file.recent.clear", null, controller::clearRecentSearches));
    }

    // ---------------------------------------------------------------------
    // Files
    // ---------------------------------------------------------------------

    /** Asks for a destination, then exports the visible rows of the table as CSV. */
    private void exportCsv() {
        Messages m = i18n.messages();
        chooseSaveFile(m.get("dialog.exportCsv.title"), "csv",
                new FileChooser.ExtensionFilter(m.get("file.csv"), "*.csv"))
                .ifPresent(file -> reportFailure(controller.exportCsv(file,
                        variantPane.visibleVariants(), Locale.getDefault(Locale.Category.FORMAT))));
    }

    /** Asks for a destination, then exports the complete result as JSON. */
    private void exportJson() {
        Messages m = i18n.messages();
        chooseSaveFile(m.get("dialog.exportJson.title"), "json",
                new FileChooser.ExtensionFilter(m.get("file.json"), "*.json"))
                .ifPresent(file -> reportFailure(controller.exportJson(file)));
    }

    /** Asks for a destination, then exports a PDF report listing the visible rows. */
    private void exportPdf() {
        Messages m = i18n.messages();
        chooseSaveFile(m.get("dialog.exportPdf.title"), "pdf",
                new FileChooser.ExtensionFilter(m.get("file.pdf"), "*.pdf"))
                .ifPresent(file -> reportFailure(controller.exportPdf(file,
                        variantPane.visibleVariants(), Locale.getDefault(Locale.Category.FORMAT))));
    }

    /** Asks for a JSON file exported earlier, then opens it. */
    private void openResults() {
        Messages m = i18n.messages();
        FileChooser chooser = fileChooser(m.get("dialog.open.title"),
                new FileChooser.ExtensionFilter(m.get("file.json"), "*.json"));
        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            reportFailure(controller.openResults(file.toPath()));
        }
    }

    /**
     * Shows a save dialog preset with a file name derived from the current result.
     *
     * @param title     dialog title
     * @param extension file extension, without the dot
     * @param filter    file type filter
     * @return the chosen file, empty if the user cancelled
     */
    private Optional<Path> chooseSaveFile(String title, String extension,
                                          FileChooser.ExtensionFilter filter) {
        FileChooser chooser = fileChooser(title, filter);
        chooser.setInitialFileName(controller.suggestedFileName(extension));
        File file = chooser.showSaveDialog(stage);
        return Optional.ofNullable(file).map(File::toPath);
    }

    /**
     * Creates a file chooser opening in the last directory used.
     *
     * @param title  dialog title
     * @param filter file type filter
     * @return the file chooser
     */
    private FileChooser fileChooser(String title, FileChooser.ExtensionFilter filter) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(filter);
        controller.lastDirectory().map(Path::toFile).ifPresent(chooser::setInitialDirectory);
        return chooser;
    }

    /**
     * Repeats a file error of the status bar in a dialog, so it cannot go unnoticed.
     *
     * @param succeeded result of the file operation
     */
    private void reportFailure(boolean succeeded) {
        StatusMessage status = state.getStatus();
        if (!succeeded && status.error()) {
            Messages m = i18n.messages();
            dialogs.showError(m, status.render(m));
        }
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    /**
     * Opens a web page or an e-mail link with the system's default application.
     *
     * @param url address to open
     */
    private void openUrl(String url) {
        hostServices.showDocument(url);
    }

    /**
     * Selects the first toggle of a group whose user data matches, or none.
     *
     * @param group   toggle group
     * @param matches tests the user data of each toggle
     */
    private static void selectToggle(ToggleGroup group, Predicate<Object> matches) {
        Toggle match = group.getToggles().stream()
                .filter(toggle -> matches.test(toggle.getUserData()))
                .findFirst()
                .orElse(null);
        group.selectToggle(match);
    }

    /**
     * Creates a label translated in the current language.
     *
     * @param key message key
     * @return the label
     */
    private Label boundLabel(String key) {
        Label label = new Label();
        label.textProperty().bind(i18n.text(key));
        return label;
    }

    /**
     * Creates a menu translated in the current language.
     *
     * @param key   message key of the title
     * @param items menu items
     * @return the menu
     */
    private Menu menu(String key, MenuItem... items) {
        Menu menu = new Menu(null, null, items);
        menu.textProperty().bind(i18n.text(key));
        return menu;
    }

    /**
     * Creates a menu item translated in the current language.
     *
     * @param key         message key of the label
     * @param accelerator keyboard shortcut, may be {@code null}
     * @param action      action to run
     * @return the menu item
     */
    private MenuItem menuItem(String key, KeyCombination accelerator, Runnable action) {
        MenuItem item = new MenuItem();
        item.textProperty().bind(i18n.text(key));
        item.setAccelerator(accelerator);
        item.setOnAction(e -> action.run());
        return item;
    }

    /**
     * Creates a menu item opening a web page; its label is a name, never translated.
     *
     * @param text label of the item
     * @param url  address to open
     * @return the menu item
     */
    private MenuItem linkItem(String text, String url) {
        MenuItem item = new MenuItem(text);
        item.setOnAction(e -> openUrl(url));
        return item;
    }

    /**
     * Creates a shortcut combination: Ctrl on Windows and Linux, Cmd on macOS.
     *
     * @param code key of the combination
     * @return the combination
     */
    private static KeyCombination shortcut(KeyCode code) {
        return new KeyCodeCombination(code, KeyCombination.SHORTCUT_DOWN);
    }
}
