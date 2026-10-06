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
