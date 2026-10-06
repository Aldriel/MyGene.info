package org.mygeneexplorer.service;

import java.io.IOException;

/**
 * Failure of a call to a BioThings API.
 *
 * <p>The {@link #reason()}, {@link #host()} and {@link #status()} let the user interface build a
 * localized message; {@link #getMessage()} provides an English default.
 */
public class ApiException extends IOException {

    /** Why the call failed. */
    public enum Reason {
        /** The server did not answer within the request timeout. */
        TIMEOUT,
        /** The server could not be reached (no network, DNS failure, connection refused). */
        UNREACHABLE,
        /** The server answered with a non-2xx HTTP status. */
        HTTP_ERROR,
        /** The server answered with a body that is not valid JSON. */
        INVALID_RESPONSE
    }

    private final Reason reason;
    private final String host;
    private final int status;

    /**
     * @param reason why the call failed
     * @param host   host of the API, e.g. {@code mygene.info}
     * @param status HTTP status code, or 0 if no response was received
     * @param cause  underlying exception, or {@code null}
     */
    public ApiException(Reason reason, String host, int status, Throwable cause) {
        super(defaultMessage(reason, host, status), cause);
        this.reason = reason;
        this.host = host;
        this.status = status;
    }

    /**
     * Creates the exception matching a non-2xx HTTP status.
     *
     * @param status HTTP status code received
     * @param host   host of the API
     * @return the exception
     */
    public static ApiException forStatus(int status, String host) {
        return new ApiException(Reason.HTTP_ERROR, host, status, null);
    }

    /** @return why the call failed */
    public Reason reason() {
        return reason;
    }

    /** @return the host of the API, e.g. {@code mygene.info} */
    public String host() {
        return host;
    }

    /** @return the HTTP status code, or 0 if no response was received */
    public int status() {
        return status;
    }

    /**
     * Builds the English default message.
     *
     * @param reason why the call failed
     * @param host   host of the API
     * @param status HTTP status code, or 0
     * @return the message
     */
    private static String defaultMessage(Reason reason, String host, int status) {
        return switch (reason) {
            case TIMEOUT -> host + " did not respond in time. Please try again.";
            case UNREACHABLE ->
                    "Unable to reach " + host + ". Please check your internet connection.";
            case INVALID_RESPONSE -> "Received an invalid response from " + host + ".";
            case HTTP_ERROR -> httpErrorMessage(status, host);
        };
    }

    /**
     * Builds the English default message of an HTTP error.
     *
     * @param status HTTP status code received
     * @param host   host of the API
     * @return the message
     */
    private static String httpErrorMessage(int status, String host) {
        if (status == 429) {
            return "Too many requests sent to " + host + ". Please wait a moment and try again.";
        }
        if (status >= 500) {
            return host + " is temporarily unavailable (HTTP " + status + ")."
                    + " Please try again later.";
        }
        if (status == 400) {
            return host + " rejected the request (HTTP 400). Please check the gene symbol.";
        }
        return "Unexpected response from " + host + " (HTTP " + status + ").";
    }
}
