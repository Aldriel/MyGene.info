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

import org.mygeneexplorer.service.InvalidSymbolException.Reason;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Validation of gene symbols entered by the user.
 *
 * <p>Official symbols (HGNC) only contain letters, digits, hyphens, dots and underscores
 * (e.g. {@code BRCA1}, {@code HLA-A}, {@code C9orf72}). Rejecting anything else before calling
 * the API prevents query-syntax injection (wildcards, field prefixes, parentheses) that would
 * yield misleading results or HTTP 400 errors.
 */
public final class GeneSymbols {

    /** Maximum accepted length of a gene symbol. */
    public static final int MAX_LENGTH = 32;

    private static final Pattern SYMBOL_PATTERN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]*");

    /** Not instantiable: static utility class. */
    private GeneSymbols() {
    }

    /**
     * Validates a gene symbol and returns it trimmed and upper-cased.
     *
     * @param input raw user input, possibly {@code null}
     * @return the normalized symbol
     * @throws InvalidSymbolException if the input is empty or malformed
     */
    public static String normalize(String input) {
        String symbol = input == null ? "" : input.strip();

        if (symbol.isEmpty()) {
            throw new InvalidSymbolException(Reason.EMPTY, symbol);
        }
        if (symbol.length() > MAX_LENGTH) {
            throw new InvalidSymbolException(Reason.TOO_LONG, symbol);
        }
        if (!SYMBOL_PATTERN.matcher(symbol).matches()) {
            throw new InvalidSymbolException(Reason.MALFORMED, symbol);
        }

        return symbol.toUpperCase(Locale.ROOT);
    }
}
