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

/** JavaFX desktop application of MyGene Explorer. */
module org.mygeneexplorer {
    requires javafx.controls;
    requires java.net.http;
    requires java.prefs;
    requires com.fasterxml.jackson.databind;
    requires org.apache.pdfbox;
    // PDFBox is an automatic module: it declares no dependencies, so the modules it needs at
    // run time must be required here to be resolved.
    requires java.desktop;
    requires org.apache.commons.logging;

    exports org.mygeneexplorer;
    exports org.mygeneexplorer.controller;
    exports org.mygeneexplorer.export;
    exports org.mygeneexplorer.i18n;
    exports org.mygeneexplorer.model;
    exports org.mygeneexplorer.prefs;
    exports org.mygeneexplorer.service;
    exports org.mygeneexplorer.ui;
}
