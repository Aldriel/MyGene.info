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
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.shape.SVGPath;
import org.mygeneexplorer.AppInfo;
import org.mygeneexplorer.i18n.I18n;

import java.util.function.Consumer;

/**
 * Discreet footer: the licence at the left, then, centred in the remaining space on one line
 * (more if the window is narrow), author signature with website and e-mail, availability for
 * contracts and the Ko-fi support button. The gaps between them are set in app.css.
 *
 * <p>The official Ko-fi widget is a script writing HTML into a web page; it is reproduced here
 * with a native button in the same colours, opening the same page.
 */
final class AppFooter extends HBox {

    private static final String COPYRIGHT = "© 2026 ";
    private static final double CUP_SCALE = 1.3;
    private static final String CUP_SHAPE = "M1 2h10v4.5A3.5 3.5 0 0 1 7.5 10h-3A3.5 3.5 0 0 1 1 "
            + "6.5zM11 3h1.2a2.2 2.2 0 0 1 0 4.4H11V6.2h1.2a1 1 0 0 0 0-2H11z";

    private final I18n i18n;

    /**
     * Builds the footer.
     *
     * @param i18n    current language
     * @param openUrl opens web pages and e-mail links
     */
    AppFooter(I18n i18n, Consumer<String> openUrl) {
        this.i18n = i18n;
        getStyleClass().add("app-footer");

        Hyperlink license = new Hyperlink();
        license.textProperty().bind(i18n.text("footer.license"));
        license.setTooltip(boundTooltip("footer.licenseTitle"));
        license.setMinWidth(Region.USE_PREF_SIZE);
        license.setOnAction(e -> openUrl.accept(AppInfo.LICENSE_URL));

        Hyperlink website = new Hyperlink();
        website.textProperty().bind(Bindings.concat(COPYRIGHT, i18n.text("brand.title")));
        website.setTooltip(boundTooltip("footer.websiteTitle"));
        // The address depends on the language, so it is read when the link is clicked.
        website.setOnAction(e -> openUrl.accept(i18n.messages().get("brand.website")));
        Label separator = new Label("·");
        Hyperlink email = new Hyperlink(AppInfo.CONTACT_EMAIL);
        email.setTooltip(boundTooltip("footer.emailTitle"));
        email.setOnAction(e -> openUrl.accept("mailto:" + AppInfo.CONTACT_EMAIL));
        HBox signature = new HBox(website, separator, email);
        signature.getStyleClass().add("footer-signature");

        Label availability = new Label();
        availability.textProperty().bind(i18n.text("footer.availability"));
        availability.getStyleClass().add("footer-tagline");

        SVGPath cup = new SVGPath();
        cup.setContent(CUP_SHAPE);
        cup.getStyleClass().add("kofi-cup");
        cup.setScaleX(CUP_SCALE);
        cup.setScaleY(CUP_SCALE);
        // The group sizes the button to the scaled cup.
        Button kofi = new Button(null, new Group(cup));
        kofi.textProperty().bind(i18n.text("footer.kofi"));
        kofi.setTooltip(boundTooltip("footer.kofiTitle"));
        kofi.getStyleClass().add("kofi-button");
        kofi.setFocusTraversable(false);
        kofi.setOnAction(e -> openUrl.accept(AppInfo.KOFI_URL));

        FlowPane main = new FlowPane(signature, availability, kofi);
        main.getStyleClass().add("footer-main");
        main.setAlignment(Pos.CENTER);
        HBox.setHgrow(main, Priority.ALWAYS);

        setAlignment(Pos.CENTER_LEFT);
        getChildren().addAll(license, main);
    }

    /**
     * Creates a tooltip translated in the current language.
     *
     * @param key message key
     * @return the tooltip
     */
    private Tooltip boundTooltip(String key) {
        Tooltip tooltip = new Tooltip();
        tooltip.textProperty().bind(i18n.text(key));
        return tooltip;
    }
}
