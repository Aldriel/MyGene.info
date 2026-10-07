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
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.mygeneexplorer.i18n.I18n;
import org.mygeneexplorer.model.GeneInfo;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/** Card summarizing the gene found: identity, type, location, aliases, summary and links. */
final class GeneCard extends VBox {

    private static final String NCBI_GENE_URL = "https://www.ncbi.nlm.nih.gov/gene/";
    private static final String GENECARDS_URL =
            "https://www.genecards.org/cgi-bin/carddisp.pl?gene=";
    private static final String NO_VALUE = "—";

    private final I18n i18n;
    private final Consumer<String> openUrl;

    private final Label symbol = new Label();
    private final Label name = new Label();
    private final Label type = new Label();
    private final Label location = new Label();
    private final Label aliases = new Label();
    private final Label summary = new Label();
    private final Hyperlink toggleSummary = new Hyperlink();
    private final Hyperlink ncbiLink = new Hyperlink();
    private final Hyperlink geneCardsLink = new Hyperlink("GeneCards");
    private final BooleanProperty expanded = new SimpleBooleanProperty();

    /**
     * Builds an empty card.
     *
     * @param i18n    current language
     * @param openUrl opens a URL in the browser
     */
    GeneCard(I18n i18n, Consumer<String> openUrl) {
        super(10);
        this.i18n = i18n;
        this.openUrl = openUrl;
        getStyleClass().add("card");

        symbol.getStyleClass().add("gene-symbol");
        name.getStyleClass().add("gene-name");
        name.setWrapText(true);
        HBox titleRow = new HBox(14, symbol, name);
        titleRow.setAlignment(Pos.BASELINE_LEFT);

        GridPane details = new GridPane();
        details.setHgap(16);
        details.setVgap(4);
        details.addRow(0, detailLabel("gene.type"), type);
        details.addRow(1, detailLabel("gene.location"), location);
        details.addRow(2, detailLabel("gene.aliases"), aliases);
        aliases.setWrapText(true);

        summary.getStyleClass().add("gene-summary");
        summary.wrapTextProperty().bind(expanded);
        summary.minHeightProperty().bind(Bindings.when(expanded)
                .then(Region.USE_PREF_SIZE).otherwise(Region.USE_COMPUTED_SIZE));
        toggleSummary.textProperty().bind(Bindings.when(expanded)
                .then(i18n.text("gene.showLess")).otherwise(i18n.text("gene.showMore")));
        toggleSummary.setOnAction(e -> expanded.set(!expanded.get()));

        HBox links = new HBox(4, toggleSummary, ncbiLink, geneCardsLink);
        links.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(titleRow, details, summary, links);
    }

    /**
     * Displays a gene, with its summary collapsed.
     *
     * @param gene gene to display
     */
    void show(GeneInfo gene) {
        symbol.setText(gene.symbol());
        name.setText(orDash(gene.name()));
        type.setText(orDash(gene.typeOfGene()));
        location.setText(orDash(gene.mapLocation()));
        aliases.setText(gene.aliases().isEmpty() ? NO_VALUE : String.join(", ", gene.aliases()));

        boolean hasSummary = gene.summary() != null && !gene.summary().isBlank();
        summary.setText(hasSummary ? gene.summary() : "");
        summary.setVisible(hasSummary);
        summary.setManaged(hasSummary);
        toggleSummary.setVisible(hasSummary);
        toggleSummary.setManaged(hasSummary);
        expanded.set(false);

        String entrezId = gene.entrezIdOrId();
        ncbiLink.textProperty().bind(i18n.text("gene.ncbiLink", entrezId));
        ncbiLink.setOnAction(e -> openUrl.accept(NCBI_GENE_URL + entrezId));
        geneCardsLink.setOnAction(e -> openUrl.accept(
                GENECARDS_URL + URLEncoder.encode(gene.symbol(), StandardCharsets.UTF_8)));
    }

    /**
     * Creates the translated label of a detail row.
     *
     * @param key message key
     * @return the label
     */
    private Label detailLabel(String key) {
        Label label = new Label();
        label.textProperty().bind(i18n.text(key));
        label.getStyleClass().add("detail-label");
        return label;
    }

    /**
     * Replaces a missing value with a dash.
     *
     * @param value value to display, may be {@code null}
     * @return the value, or a dash if it is missing or blank
     */
    private static String orDash(String value) {
        return value == null || value.isBlank() ? NO_VALUE : value;
    }
}
