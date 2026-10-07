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

package org.mygeneexplorer.ui;

import javafx.beans.binding.Bindings;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleLongProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.mygeneexplorer.controller.MainController;
import org.mygeneexplorer.export.CsvExporter;
import org.mygeneexplorer.i18n.I18n;
import org.mygeneexplorer.i18n.Messages;
import org.mygeneexplorer.model.ClinvarVariant;
import org.mygeneexplorer.model.SignificanceCategory;
import org.mygeneexplorer.model.VariantFilter;

import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Sortable, filterable table of ClinVar variants, with copy to clipboard and a context menu.
 */
final class VariantTablePane extends VBox {

    private static final String NO_VALUE = "—";
    private static final KeyCombination COPY =
            new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN);

    /**
     * Entry of the category filter.
     *
     * @param category category to keep, {@code null} for all
     */
    private record CategoryChoice(SignificanceCategory category) {
    }

    private static final CategoryChoice ALL = new CategoryChoice(null);

    private final I18n i18n;
    private final Consumer<String> openUrl;
    private final Consumer<Integer> onCopied;

    private final ObservableList<ClinvarVariant> variants = FXCollections.observableArrayList();
    private final FilteredList<ClinvarVariant> filtered = new FilteredList<>(variants);
    private final TableView<ClinvarVariant> table = new TableView<>();
    private final TextField filterField = new TextField();
    private final ComboBox<CategoryChoice> categoryBox = new ComboBox<>();
    private final LongProperty total = new SimpleLongProperty();
    private String geneSymbol = "";

    /**
     * @param i18n     current language
     * @param openUrl  opens a URL in the browser
     * @param onCopied called with the number of variants copied to the clipboard
     */
    VariantTablePane(I18n i18n, Consumer<String> openUrl, Consumer<Integer> onCopied) {
        this.i18n = i18n;
        this.openUrl = openUrl;
        this.onCopied = onCopied;
        getStyleClass().add("tab-content");

        getChildren().addAll(buildFilterBar(), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        buildTable();
    }

    /**
     * Displays new variants and resets the filter.
     *
     * @param geneSymbol     symbol of the gene, used when copying rows
     * @param newVariants    variants to display
     * @param totalInClinvar number of variants of the gene in ClinVar
     */
    void setVariants(String geneSymbol, List<ClinvarVariant> newVariants, long totalInClinvar) {
        this.geneSymbol = geneSymbol;
        filterField.clear();
        categoryBox.setValue(ALL);
        total.set(totalInClinvar);
        variants.setAll(newVariants);
        table.getSelectionModel().clearSelection();
        table.scrollTo(0);
    }

    /** @return the variants currently displayed, filtered and sorted as on screen */
    List<ClinvarVariant> visibleVariants() {
        return List.copyOf(table.getItems());
    }

    /** Moves the keyboard focus to the filter field and selects its text. */
    void focusFilter() {
        filterField.requestFocus();
        filterField.selectAll();
    }

    /** Copies the selected rows, or all visible rows if none is selected, as tab-separated text. */
    void copySelection() {
        List<ClinvarVariant> rows = table.getSelectionModel().getSelectedItems().isEmpty()
                ? visibleVariants()
                : List.copyOf(table.getSelectionModel().getSelectedItems());
        if (rows.isEmpty()) {
            return;
        }
        List<String> headers = MainController.csvHeaders(i18n.messages());
        ClipboardContent content = new ClipboardContent();
        content.putString(CsvExporter.toCsv(geneSymbol, rows, headers, '\t'));
        Clipboard.getSystemClipboard().setContent(content);
        onCopied.accept(rows.size());
    }

    // ---------------------------------------------------------------------
    // Construction
    // ---------------------------------------------------------------------

    /**
     * Builds the filter bar: text filter, category filter, Clear button and row count.
     *
     * @return the filter bar
     */
    private HBox buildFilterBar() {
        filterField.promptTextProperty().bind(i18n.text("filter.prompt"));
        filterField.setPrefColumnCount(22);

        categoryBox.getItems().add(ALL);
        for (SignificanceCategory category : SignificanceCategory.values()) {
            categoryBox.getItems().add(new CategoryChoice(category));
        }
        categoryBox.setValue(ALL);
        updateCategoryConverter();
        i18n.messagesProperty().addListener(o -> updateCategoryConverter());

        Button clear = new Button();
        clear.textProperty().bind(i18n.text("filter.clear"));
        clear.setOnAction(e -> {
            filterField.clear();
            categoryBox.setValue(ALL);
        });

        filtered.predicateProperty().bind(Bindings.createObjectBinding(
                () -> new VariantFilter(filterField.getText(), categoryBox.getValue() == null
                        ? null : categoryBox.getValue().category()),
                filterField.textProperty(), categoryBox.valueProperty()));
        clear.disableProperty().bind(Bindings.createBooleanBinding(
                () -> !((VariantFilter) filtered.getPredicate()).isActive(),
                filtered.predicateProperty()));

        Label count = new Label();
        count.getStyleClass().add("muted");
        count.textProperty().bind(Bindings.createStringBinding(
                () -> i18n.messages().get("filter.count", filtered.size(), variants.size(),
                        total.get()),
                i18n.messagesProperty(), filtered, variants, total));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bar = new HBox(8, filterField, categoryBox, clear, spacer, count);
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    /** Translates the entries of the category filter in the current language. */
    private void updateCategoryConverter() {
        Messages m = i18n.messages();
        categoryBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(CategoryChoice choice) {
                if (choice == null || choice.category() == null) {
                    return m.get("filter.allCategories");
                }
                return m.get(choice.category().messageKey());
            }

            @Override
            public CategoryChoice fromString(String text) {
                throw new UnsupportedOperationException();
            }
        });
    }

    /** Configures the columns, sorting, selection and keyboard shortcuts of the table. */
    private void buildTable() {
        TableColumn<ClinvarVariant, Long> idColumn = new TableColumn<>();
        idColumn.textProperty().bind(i18n.text("table.variantId"));
        idColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().variantId()));
        idColumn.setCellFactory(c -> new VariantLinkCell());
        idColumn.setPrefWidth(120);

        TableColumn<ClinvarVariant, String> hgvsColumn = new TableColumn<>();
        hgvsColumn.textProperty().bind(i18n.text("table.hgvs"));
        hgvsColumn.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().hgvs()));
        hgvsColumn.setCellFactory(c -> {
            TableCell<ClinvarVariant, String> cell = new TableCell<>() {
                @Override
                protected void updateItem(String hgvs, boolean empty) {
                    super.updateItem(hgvs, empty);
                    setText(empty ? null : hgvs);
                }
            };
            cell.getStyleClass().add("hgvs");
            return cell;
        });
        hgvsColumn.setPrefWidth(250);

        TableColumn<ClinvarVariant, ClinvarVariant> significanceColumn = new TableColumn<>();
        significanceColumn.textProperty().bind(i18n.text("table.significance"));
        significanceColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        significanceColumn.setCellFactory(c -> new SignificanceCell());
        significanceColumn.setComparator(Comparator
                .comparing(SignificanceCategory::primaryOf)
                .thenComparing(v -> String.join(",", v.clinicalSignificances())));
        significanceColumn.setPrefWidth(340);

        TableColumn<ClinvarVariant, String> originColumn = new TableColumn<>();
        originColumn.textProperty().bind(i18n.text("table.origin"));
        originColumn.setCellValueFactory(c -> {
            List<String> origins = c.getValue().origins();
            return new ReadOnlyStringWrapper(
                    origins.isEmpty() ? NO_VALUE : String.join(", ", origins));
        });
        originColumn.setPrefWidth(220);

        table.getColumns().setAll(List.of(idColumn, hgvsColumn, significanceColumn, originColumn));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        SortedList<ClinvarVariant> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);

        Label placeholder = new Label();
        placeholder.getStyleClass().add("muted");
        placeholder.textProperty().bind(Bindings.createStringBinding(
                () -> i18n.messages().get(variants.isEmpty() ? "table.empty" : "filter.noMatch"),
                i18n.messagesProperty(), variants));
        table.setPlaceholder(placeholder);

        table.setRowFactory(t -> createRow());
        table.setOnKeyPressed(e -> {
            if (COPY.match(e)) {
                copySelection();
                e.consume();
            }
        });
    }

    /**
     * Creates a table row with a context menu and double-click to open ClinVar.
     *
     * @return the row
     */
    private TableRow<ClinvarVariant> createRow() {
        TableRow<ClinvarVariant> row = new TableRow<>();

        MenuItem open = new MenuItem();
        open.textProperty().bind(i18n.text("table.openClinvar"));
        open.setOnAction(e -> openUrl.accept(row.getItem().clinvarUrl()));
        open.disableProperty().bind(Bindings.createBooleanBinding(
                () -> row.getItem() == null || row.getItem().clinvarUrl() == null,
                row.itemProperty()));

        MenuItem copyHgvs = new MenuItem();
        copyHgvs.textProperty().bind(i18n.text("table.copyHgvs"));
        copyHgvs.setOnAction(e -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(row.getItem().hgvs());
            Clipboard.getSystemClipboard().setContent(content);
            onCopied.accept(1);
        });

        MenuItem copyRows = new MenuItem();
        copyRows.textProperty().bind(i18n.text("table.copyRows"));
        copyRows.setOnAction(e -> copySelection());

        ContextMenu menu = new ContextMenu(open, new SeparatorMenuItem(), copyHgvs, copyRows);
        row.contextMenuProperty().bind(Bindings.when(row.emptyProperty())
                .then((ContextMenu) null).otherwise(menu));

        row.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2
                    && !row.isEmpty() && row.getItem().clinvarUrl() != null) {
                openUrl.accept(row.getItem().clinvarUrl());
            }
        });
        return row;
    }

    /** Cell showing the ClinVar identifier as a link to the NCBI record. */
    private final class VariantLinkCell extends TableCell<ClinvarVariant, Long> {

        private final Hyperlink link = new Hyperlink();

        VariantLinkCell() {
            link.setOnAction(e -> {
                ClinvarVariant variant = getTableRow().getItem();
                if (variant != null && variant.clinvarUrl() != null) {
                    openUrl.accept(variant.clinvarUrl());
                }
            });
        }

        @Override
        protected void updateItem(Long id, boolean empty) {
            super.updateItem(id, empty);
            if (empty) {
                setText(null);
                setGraphic(null);
            } else if (id == null) {
                setText(NO_VALUE);
                setGraphic(null);
            } else {
                link.setText(id.toString());
                setText(null);
                setGraphic(link);
            }
        }
    }

    /** Cell showing each clinical significance as a colored badge. */
    private static final class SignificanceCell extends TableCell<ClinvarVariant, ClinvarVariant> {

        @Override
        protected void updateItem(ClinvarVariant variant, boolean empty) {
            super.updateItem(variant, empty);
            setText(null);
            if (empty || variant == null) {
                setGraphic(null);
                return;
            }
            if (variant.clinicalSignificances().isEmpty()) {
                setGraphic(new Label(NO_VALUE));
                return;
            }
            HBox badges = new HBox(4);
            badges.setAlignment(Pos.CENTER_LEFT);
            for (String significance : variant.clinicalSignificances()) {
                Label badge = new Label(significance);
                badge.getStyleClass().addAll("badge",
                        SignificanceCategory.classify(significance).id());
                badges.getChildren().add(badge);
            }
            setGraphic(badges);
        }
    }
}
