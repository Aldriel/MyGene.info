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

import javafx.geometry.Pos;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.mygeneexplorer.i18n.I18n;
import org.mygeneexplorer.i18n.Messages;
import org.mygeneexplorer.model.ClinvarVariant;
import org.mygeneexplorer.model.SignificanceCategory;

import java.util.List;
import java.util.Map;

/**
 * Distribution of the fetched variants by clinical significance: pie chart and legend with
 * counts and percentages. Each variant is counted once, in its most severe category.
 */
final class SummaryPane extends HBox {

    private final I18n i18n;
    private final PieChart chart = new PieChart();
    private final Label caption = new Label();
    private final Label pathogenicShare = new Label();
    private final GridPane legend = new GridPane();

    private List<ClinvarVariant> variants = List.of();
    private long total;

    SummaryPane(I18n i18n) {
        super(28);
        this.i18n = i18n;
        getStyleClass().add("tab-content");

        chart.setLegendVisible(false);
        chart.setLabelsVisible(false);
        chart.setAnimated(false);
        chart.setMinSize(280, 280);
        chart.setPrefSize(420, 420);
        HBox.setHgrow(chart, Priority.ALWAYS);

        Label title = new Label();
        title.textProperty().bind(i18n.text("summary.title"));
        title.getStyleClass().add("section-title");
        caption.getStyleClass().add("muted");
        caption.setWrapText(true);
        pathogenicShare.getStyleClass().add("highlight");
        pathogenicShare.setWrapText(true);
        legend.setHgap(12);
        legend.setVgap(8);

        VBox side = new VBox(12, title, caption, pathogenicShare, legend);
        side.setMinWidth(300);
        side.setAlignment(Pos.TOP_LEFT);
        getChildren().addAll(chart, side);

        i18n.messagesProperty().addListener(o -> refresh());
        refresh();
    }

    /**
     * Summarizes new variants.
     *
     * @param newVariants    variants loaded
     * @param totalInClinvar number of variants of the gene in ClinVar
     */
    void setVariants(List<ClinvarVariant> newVariants, long totalInClinvar) {
        variants = List.copyOf(newVariants);
        total = totalInClinvar;
        refresh();
    }

    /** Rebuilds the chart, caption and legend in the current language. */
    private void refresh() {
        Messages m = i18n.messages();
        chart.getData().clear();
        legend.getChildren().clear();

        if (variants.isEmpty()) {
            caption.setText(m.get("summary.empty"));
            pathogenicShare.setText("");
            return;
        }

        int size = variants.size();
        caption.setText(m.get("summary.caption", size, total));
        Map<SignificanceCategory, Long> counts = SignificanceCategory.countByPrimary(variants);
        long pathogenic = counts.get(SignificanceCategory.PATHOGENIC)
                + counts.get(SignificanceCategory.LIKELY_PATHOGENIC);
        pathogenicShare.setText(m.get("summary.pathogenicShare", pathogenic,
                (double) pathogenic / size));

        int row = 0;
        for (Map.Entry<SignificanceCategory, Long> entry : counts.entrySet()) {
            long count = entry.getValue();
            if (count == 0) {
                continue;
            }
            SignificanceCategory category = entry.getKey();
            String name = m.get(category.messageKey());
            String detail = m.get("summary.legendCount", count, (double) count / size);

            PieChart.Data slice = new PieChart.Data(name, count);
            chart.getData().add(slice);
            String color = "-fx-pie-color: " + category.color() + ";";
            if (slice.getNode() != null) {
                slice.getNode().setStyle(color);
                Tooltip.install(slice.getNode(), new Tooltip(name + " — " + detail));
            }

            Region swatch = new Region();
            swatch.getStyleClass().add("legend-swatch");
            swatch.setStyle("-fx-background-color: " + category.color() + ";");
            Label countLabel = new Label(detail);
            countLabel.getStyleClass().add("muted");
            legend.addRow(row++, swatch, new Label(name), countLabel);
        }
    }
}
