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

import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.mygeneexplorer.AppInfo;
import org.mygeneexplorer.i18n.Messages;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Modal dialogs of the main window, styled like the window that owns them. */
final class Dialogs {

    private final Window owner;
    private final Node styleSource;
    private final String stylesheet;

    /**
     * @param owner       window owning the dialogs
     * @param styleSource node whose inline style (text size) the dialogs copy
     * @param stylesheet  application stylesheet
     */
    Dialogs(Window owner, Node styleSource, String stylesheet) {
        this.owner = owner;
        this.styleSource = styleSource;
        this.stylesheet = stylesheet;
    }

    /**
     * Shows an error message.
     *
     * @param m       texts of the current language
     * @param message error to show
     */
    void showError(Messages m, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(m.get("dialog.error.title"));
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getButtonTypes().setAll(closeButton(m));
        prepare(alert);
        alert.showAndWait();
    }

    /**
     * Shows the application name, version, author, data sources and disclaimer in an
     * information alert.
     *
     * @param m       texts of the current language
     * @param openUrl opens the website and e-mail links
     */
    void showAbout(Messages m, Consumer<String> openUrl) {
        Label description = wrapped(m.get("about.description"));
        Label author = new Label(m.get("about.author", AppInfo.AUTHOR));
        author.setStyle("-fx-font-weight: bold;");
        String websiteUrl = m.get("brand.website");
        Hyperlink website = new Hyperlink(websiteUrl);
        website.setOnAction(e -> openUrl.accept(websiteUrl));
        Hyperlink contact = new Hyperlink(AppInfo.CONTACT_EMAIL);
        contact.setOnAction(e -> openUrl.accept("mailto:" + AppInfo.CONTACT_EMAIL));

        Label sources = wrapped(m.get("about.sources"));
        Label disclaimer = wrapped(m.get("app.disclaimer"));
        disclaimer.getStyleClass().addAll("muted", "small");
        Label runtime = new Label(m.get("about.runtime",
                System.getProperty("java.version"), System.getProperty("javafx.version")));
        runtime.getStyleClass().addAll("muted", "small");

        VBox content = new VBox(10, description, new VBox(author, website, contact), sources, disclaimer,
                runtime);
        content.setPrefWidth(460);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(m.get("about.title", AppInfo.NAME));
        alert.setHeaderText(AppInfo.NAME + "\n" + m.get("about.version", AppInfo.version()));
        alert.getDialogPane().setContent(content);
        alert.getButtonTypes().setAll(closeButton(m));
        prepare(alert);
        alert.showAndWait();
    }

    /**
     * Shows the keyboard shortcuts.
     *
     * @param m texts of the current language
     */
    void showShortcuts(Messages m) {
        Map<String, String> shortcuts = new LinkedHashMap<>();
        shortcuts.put("Enter", m.get("shortcuts.search"));
        shortcuts.put(display("Shortcut+L"), m.get("menu.edit.newSearch"));
        shortcuts.put(display("Shortcut+F"), m.get("menu.edit.filter"));
        shortcuts.put(display("Shortcut+C"), m.get("menu.edit.copy"));
        shortcuts.put(display("Shortcut+O"), m.get("menu.file.open"));
        shortcuts.put(display("Shortcut+E"), m.get("menu.file.exportCsv"));
        shortcuts.put(display("Shortcut+S"), m.get("menu.file.exportJson"));
        shortcuts.put(display("Shortcut+P"), m.get("menu.file.exportPdf"));
        shortcuts.put(display("Shortcut+Equals"), m.get("menu.options.zoomIn"));
        shortcuts.put(display("Shortcut+Minus"), m.get("menu.options.zoomOut"));
        shortcuts.put(display("Shortcut+0"), m.get("menu.options.zoomReset"));
        shortcuts.put(m.get("shortcuts.doubleClick"), m.get("table.openClinvar"));
        shortcuts.put(display("Shortcut+Q"), m.get("menu.file.quit"));

        GridPane grid = new GridPane();
        grid.setHgap(24);
        grid.setVgap(6);
        int row = 0;
        for (Map.Entry<String, String> entry : shortcuts.entrySet()) {
            Label keys = new Label(entry.getKey());
            keys.setStyle("-fx-font-weight: bold;");
            grid.addRow(row++, keys, new Label(entry.getValue()));
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(m.get("menu.help.shortcuts"));
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().setAll(closeButton(m));
        prepare(dialog);
        dialog.showAndWait();
    }

    /**
     * Attaches a dialog to the main window and gives it the same stylesheet and text size.
     *
     * @param dialog dialog to prepare
     */
    private void prepare(Dialog<?> dialog) {
        dialog.initOwner(owner);
        dialog.getDialogPane().getStylesheets().setAll(List.of(stylesheet));
        dialog.getDialogPane().setStyle(styleSource.getStyle());
    }

    /**
     * Creates the Close button of the dialogs.
     *
     * @param m texts of the current language
     * @return the button type
     */
    private static ButtonType closeButton(Messages m) {
        return new ButtonType(m.get("dialog.close"), ButtonBar.ButtonData.CANCEL_CLOSE);
    }

    /**
     * Creates a label whose text wraps to the dialog width.
     *
     * @param text label text
     * @return the label
     */
    private static Label wrapped(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        return label;
    }

    /**
     * Formats a key combination the way the platform displays it, e.g. "Ctrl+S" on Windows.
     *
     * @param combination combination, e.g. {@code "Shortcut+S"}
     * @return the display text
     */
    private static String display(String combination) {
        return KeyCombination.keyCombination(combination).getDisplayText();
    }
}
