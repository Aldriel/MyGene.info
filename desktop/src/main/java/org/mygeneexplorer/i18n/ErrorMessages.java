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

package org.mygeneexplorer.i18n;

import org.mygeneexplorer.export.ResultFileException;
import org.mygeneexplorer.service.ApiException;
import org.mygeneexplorer.service.GeneSymbols;
import org.mygeneexplorer.service.InvalidSymbolException;

/** Builds localized, user-friendly descriptions of the errors raised by the application. */
public final class ErrorMessages {

    /** Not instantiable: static utility class. */
    private ErrorMessages() {
    }

    /**
     * Describes an error in the current language.
     *
     * @param error    error to describe, possibly {@code null}
     * @param messages messages of the current language
     * @return a message suitable for display
     */
    public static String describe(Throwable error, Messages messages) {
        if (error instanceof InvalidSymbolException e) {
            return switch (e.reason()) {
                case EMPTY -> messages.get("error.symbol.empty");
                case TOO_LONG -> messages.get("error.symbol.tooLong", GeneSymbols.MAX_LENGTH);
                case MALFORMED -> messages.get("error.symbol.malformed", e.symbol());
            };
        }
        if (error instanceof ApiException e) {
            return describeApiError(e, messages);
        }
        if (error instanceof ResultFileException e) {
            return switch (e.reason()) {
                case UNREADABLE -> messages.get("error.file.unreadable", e.fileName());
                case INVALID_FORMAT -> messages.get("error.file.invalid", e.fileName());
                case UNSUPPORTED_VERSION -> messages.get("error.file.unsupported", e.fileName());
            };
        }
        if (error != null && error.getMessage() != null && !error.getMessage().isBlank()) {
            return error.getMessage();
        }
        return messages.get("error.unexpected");
    }

    /**
     * Describes a failed API call according to its cause and HTTP status.
     *
     * @param error    failed call
     * @param messages messages of the current language
     * @return a message suitable for display
     */
    private static String describeApiError(ApiException error, Messages messages) {
        String host = error.host();
        String status = String.valueOf(error.status());
        return switch (error.reason()) {
            case TIMEOUT -> messages.get("error.api.timeout", host);
            case UNREACHABLE -> messages.get("error.api.unreachable", host);
            case INVALID_RESPONSE -> messages.get("error.api.invalidResponse", host);
            case HTTP_ERROR -> {
                if (error.status() == 429) {
                    yield messages.get("error.api.tooManyRequests", host);
                }
                if (error.status() >= 500) {
                    yield messages.get("error.api.unavailable", host, status);
                }
                if (error.status() == 400) {
                    yield messages.get("error.api.badRequest", host);
                }
                yield messages.get("error.api.unexpectedStatus", host, status);
            }
        };
    }
}
