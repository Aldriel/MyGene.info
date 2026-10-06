package org.mygeneexplorer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AppInfoTest {

    @Test
    void versionIsInjectedByTheBuild() {
        assertTrue(AppInfo.version().matches("\\d+\\.\\d+\\.\\d+.*"), AppInfo.version());
    }
}
