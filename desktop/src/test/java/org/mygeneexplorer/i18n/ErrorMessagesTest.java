package org.mygeneexplorer.i18n;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mygeneexplorer.export.ResultFileException;
import org.mygeneexplorer.service.ApiException;
import org.mygeneexplorer.service.ApiException.Reason;
import org.mygeneexplorer.service.InvalidSymbolException;

import java.io.IOException;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ErrorMessagesTest {

    private final Messages english = Messages.forLocale(Locale.ENGLISH);
    private final Messages french = Messages.forLocale(Locale.FRENCH);

    @Test
    void describesInvalidSymbols() {
        assertEquals("Please enter a gene symbol.", ErrorMessages.describe(
                new InvalidSymbolException(InvalidSymbolException.Reason.EMPTY, ""), english));
        assertEquals("Un symbole de gène comporte au plus 32 caractères.",
                ErrorMessages.describe(new InvalidSymbolException(
                        InvalidSymbolException.Reason.TOO_LONG, "A".repeat(33)), french));
        assertEquals("« B* » n’est pas un symbole de gène valide. Utilisez uniquement des"
                        + " lettres, chiffres, tirets, points ou traits de soulignement"
                        + " (ex. : BRCA1, HLA-A, C9orf72).",
                ErrorMessages.describe(new InvalidSymbolException(
                        InvalidSymbolException.Reason.MALFORMED, "B*"), french));
    }

    @ParameterizedTest(name = "{0} {1}")
    @CsvSource(delimiter = '|', value = {
            "TIMEOUT|0|mygene.info n’a pas répondu à temps. Veuillez réessayer.",
            "UNREACHABLE|0|Impossible de joindre mygene.info. Vérifiez votre connexion Internet.",
            "INVALID_RESPONSE|200|Réponse invalide reçue de mygene.info.",
            "HTTP_ERROR|429|Trop de requêtes envoyées à mygene.info."
                    + " Patientez un instant puis réessayez.",
            "HTTP_ERROR|503|mygene.info est temporairement indisponible (HTTP 503)."
                    + " Veuillez réessayer plus tard.",
            "HTTP_ERROR|400|mygene.info a rejeté la requête (HTTP 400)."
                    + " Vérifiez le symbole du gène.",
            "HTTP_ERROR|404|Réponse inattendue de mygene.info (HTTP 404).",
    })
    void describesApiErrorsInFrench(Reason reason, int status, String expected) {
        ApiException error = new ApiException(reason, "mygene.info", status, null);

        assertEquals(expected, ErrorMessages.describe(error, french));
    }

    @Test
    void englishApiMessagesMatchTheServiceDefaults() {
        for (Reason reason : Reason.values()) {
            for (int status : new int[] {0, 400, 404, 429, 500}) {
                ApiException error = new ApiException(reason, "mygene.info", status, null);
                assertEquals(error.getMessage(), ErrorMessages.describe(error, english));
            }
        }
    }

    @Test
    void describesResultFileErrors() {
        assertEquals("Impossible de lire a.json.", ErrorMessages.describe(
                new ResultFileException(ResultFileException.Reason.UNREADABLE, "a.json", null),
                french));
        assertEquals("a.json is not a valid MyGene Explorer results file.",
                ErrorMessages.describe(new ResultFileException(
                        ResultFileException.Reason.INVALID_FORMAT, "a.json", null), english));
        assertEquals("a.json was created by a newer version of MyGene Explorer."
                        + " Please update the application.",
                ErrorMessages.describe(new ResultFileException(
                        ResultFileException.Reason.UNSUPPORTED_VERSION, "a.json", null), english));
    }

    @Test
    void fallsBackToTheErrorMessageOrAGenericText() {
        assertEquals("Disk full", ErrorMessages.describe(new IOException("Disk full"), french));
        assertEquals("Une erreur inattendue est survenue.",
                ErrorMessages.describe(new IOException(" "), french));
        assertEquals("An unexpected error occurred.", ErrorMessages.describe(null, english));
    }
}
