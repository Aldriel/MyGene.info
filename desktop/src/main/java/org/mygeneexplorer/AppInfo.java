package org.mygeneexplorer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Name, version and author of the application. */
public final class AppInfo {

    /** Name of the application, shown in the title bar and the About dialog. */
    public static final String NAME = "MyGene Explorer";
    /** Author of the application. */
    public static final String AUTHOR = "Maxime Ethier";
    /** Contact address shown in the About dialog. */
    public static final String CONTACT_EMAIL = "contact@maximeethier.com";

    private static final String VERSION = loadVersion();

    /** Not instantiable: constants only. */
    private AppInfo() {
    }

    /** @return the version of the application, set by the Maven build */
    public static String version() {
        return VERSION;
    }

    /**
     * Reads the version written into {@code app.properties} by the build.
     *
     * @return the version, or {@code "dev"} when running outside a Maven build
     */
    private static String loadVersion() {
        try (InputStream in = AppInfo.class.getResourceAsStream("app.properties")) {
            if (in == null) {
                return "dev";
            }
            Properties properties = new Properties();
            properties.load(in);
            return properties.getProperty("version", "dev");
        } catch (IOException e) {
            return "dev";
        }
    }
}
