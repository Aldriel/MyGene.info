package org.mygeneexplorer.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("GeneSymbols.normalize")
class GeneSymbolsTest {

    @ParameterizedTest(name = "accepts \"{0}\" as {1}")
    @CsvSource(delimiter = '|', ignoreLeadingAndTrailingWhitespace = false, value = {
            "BRCA1|BRCA1",
            "brca1|BRCA1",
            "  TP53  |TP53",
            "HLA-A|HLA-A",
            "C9orf72|C9ORF72",
            "NKX2-1|NKX2-1",
            "MT-ND1|MT-ND1",
            "SNORD3A_1|SNORD3A_1",
            "GBA1.2|GBA1.2",
    })
    void acceptsValidSymbols(String input, String expected) {
        assertEquals(expected, GeneSymbols.normalize(input));
    }

    @ParameterizedTest(name = "asks for a symbol when the input is \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\n"})
    void rejectsMissingSymbol(String input) {
        InvalidSymbolException error =
                assertThrows(InvalidSymbolException.class, () -> GeneSymbols.normalize(input));
        assertEquals("Please enter a gene symbol.", error.getMessage());
    }

    @ParameterizedTest(name = "rejects \"{0}\"")
    @ValueSource(strings = {
            "B*", "BRCA?", "BRCA1 TP53", "name:kinase", "(((", "\"BRCA1\"", "BRCA1||TP53",
            "GÈNE1", "@#!", "-BRCA1", "<script>", "BRCA1;DROP",
    })
    void rejectsMalformedSymbols(String input) {
        InvalidSymbolException error =
                assertThrows(InvalidSymbolException.class, () -> GeneSymbols.normalize(input));
        assertTrue(error.getMessage().contains("is not a valid gene symbol"), error.getMessage());
        assertEquals(InvalidSymbolException.Reason.MALFORMED, error.reason());
        assertEquals(input.strip(), error.symbol());
    }

    @Test
    void enforcesMaximumLength() {
        String longest = "A".repeat(GeneSymbols.MAX_LENGTH);

        assertEquals(longest, GeneSymbols.normalize(longest));
        InvalidSymbolException error = assertThrows(InvalidSymbolException.class,
                () -> GeneSymbols.normalize(longest + "A"));
        assertEquals("Gene symbols are at most 32 characters long.", error.getMessage());
    }

    @Test
    void invalidSymbolIsAnIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> GeneSymbols.normalize("B*"));
    }
}
