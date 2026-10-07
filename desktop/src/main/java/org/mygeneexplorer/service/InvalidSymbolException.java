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

package org.mygeneexplorer.service;

/**
 * Thrown when the user input is not a valid gene symbol.
 *
 * <p>The {@link #reason()} and {@link #symbol()} let the user interface build a localized
 * message; {@link #getMessage()} provides an English default.
 */
public class InvalidSymbolException extends IllegalArgumentException {

    /** Why the symbol was rejected. */
    public enum Reason {
        /** Nothing was entered. */
        EMPTY,
        /** The input is longer than {@link GeneSymbols#MAX_LENGTH} characters. */
        TOO_LONG,
        /** The input contains characters that cannot appear in a gene symbol. */
        MALFORMED
    }

    private final Reason reason;
    private final String symbol;

    /**
     * @param reason why the symbol was rejected
     * @param symbol trimmed user input
     */
    public InvalidSymbolException(Reason reason, String symbol) {
        super(defaultMessage(reason, symbol));
        this.reason = reason;
        this.symbol = symbol;
    }

    /** @return why the symbol was rejected */
    public Reason reason() {
        return reason;
    }

    /** @return the trimmed user input */
    public String symbol() {
        return symbol;
    }

    /**
     * Builds the English default message.
     *
     * @param reason why the symbol was rejected
     * @param symbol trimmed user input
     * @return the message
     */
    private static String defaultMessage(Reason reason, String symbol) {
        return switch (reason) {
            case EMPTY -> "Please enter a gene symbol.";
            case TOO_LONG ->
                    "Gene symbols are at most " + GeneSymbols.MAX_LENGTH + " characters long.";
            case MALFORMED -> "\"" + symbol + "\" is not a valid gene symbol."
                    + " Use letters, digits, hyphens, dots or underscores only"
                    + " (e.g. BRCA1, HLA-A, C9orf72).";
        };
    }
}
