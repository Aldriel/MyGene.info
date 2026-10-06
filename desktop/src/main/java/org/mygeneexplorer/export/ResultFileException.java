package org.mygeneexplorer.export;

import java.io.IOException;

/**
 * Thrown when a saved result file cannot be opened.
 *
 * <p>The {@link #reason()} and {@link #fileName()} let the user interface build a localized
 * message; {@link #getMessage()} provides an English default.
 */
public class ResultFileException extends IOException {

    /** Why the file could not be opened. */
    public enum Reason {
        /** The file does not exist or cannot be read. */
        UNREADABLE,
        /** The file is not a MyGene Explorer result file or is corrupted. */
        INVALID_FORMAT,
        /** The file was written by a newer version of the application. */
        UNSUPPORTED_VERSION
    }

    private final Reason reason;
    private final String fileName;

    /**
     * Creates the exception.
     *
     * @param reason   why the file could not be opened
     * @param fileName name of the file, without its directory
     * @param cause    underlying exception, or {@code null}
     */
    public ResultFileException(Reason reason, String fileName, Throwable cause) {
        super(defaultMessage(reason, fileName), cause);
        this.reason = reason;
        this.fileName = fileName;
    }

    /** @return why the file could not be opened */
    public Reason reason() {
        return reason;
    }

    /** @return the name of the file, without its directory */
    public String fileName() {
        return fileName;
    }

    /**
     * Builds the English default message.
     *
     * @param reason   why the file could not be opened
     * @param fileName name of the file
     * @return the message
     */
    private static String defaultMessage(Reason reason, String fileName) {
        return switch (reason) {
            case UNREADABLE -> "Unable to read " + fileName + ".";
            case INVALID_FORMAT -> fileName + " is not a valid MyGene Explorer result file.";
            case UNSUPPORTED_VERSION ->
                    fileName + " was created by a newer version of MyGene Explorer.";
        };
    }
}
