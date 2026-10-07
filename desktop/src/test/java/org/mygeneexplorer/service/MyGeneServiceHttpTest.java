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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mygeneexplorer.model.ClinvarResult;
import org.mygeneexplorer.model.GeneInfo;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Exercises the HTTP layer against a local server that simulates the BioThings APIs. */
class MyGeneServiceHttpTest {

    private static final Duration TEST_TIMEOUT = Duration.ofMillis(500);

    private HttpServer server;
    private ExecutorService handlers;
    private String host;
    private final AtomicReference<String> lastQuery = new AtomicReference<>();

    private volatile int status = 200;
    private volatile String body = "{}";
    private volatile long delayMillis;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", this::handle);
        handlers = Executors.newCachedThreadPool();
        server.setExecutor(handlers);
        server.start();
        host = "127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        handlers.shutdownNow();
        server.stop(0);
    }

    private void handle(HttpExchange exchange) throws IOException {
        lastQuery.set(URLDecoder.decode(exchange.getRequestURI().getRawQuery(),
                StandardCharsets.UTF_8));
        if (delayMillis > 0) {
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        } catch (IOException ignored) {
            // The client may have given up (timeout or cancellation).
        }
    }

    private MyGeneService serviceFor(String baseHost) {
        URI base = URI.create("http://" + baseHost);
        return new MyGeneService(HttpClient.newHttpClient(),
                base.resolve("/v3"), base.resolve("/v1"), TEST_TIMEOUT);
    }

    private MyGeneService service() {
        return serviceFor(host);
    }

    @Test
    void findGeneSendsTheExpectedQuery() throws Exception {
        body = """
                {"total": 1, "hits": [{"_id": "672", "symbol": "BRCA1",
                  "name": "BRCA1 DNA repair associated", "entrezgene": 672}]}
                """;

        Optional<GeneInfo> gene = service().findGene(" brca1 ");

        assertEquals("BRCA1", gene.orElseThrow().symbol());
        assertEquals("q=symbol:BRCA1&species=human"
                        + "&fields=symbol,name,entrezgene,summary,type_of_gene,map_location,alias"
                        + "&size=1",
                lastQuery.get());
    }

    @Test
    void findGeneReturnsEmptyWhenNothingMatches() throws Exception {
        body = "{\"total\": 0, \"hits\": []}";

        assertTrue(service().findGene("ZZZ999").isEmpty());
    }

    @Test
    void findClinvarVariantsReturnsTotalAndVariants() throws Exception {
        body = """
                {"total": 94, "hits": [{"_id": "chr9:g.1A>G",
                  "clinvar": {"variant_id": 7, "rcv": {"clinical_significance": "Benign"}}}]}
                """;

        ClinvarResult result = service().findClinvarVariants("C9orf72", 25);

        assertEquals(94, result.total());
        assertEquals(1, result.variants().size());
        assertTrue(lastQuery.get().startsWith("q=clinvar.gene.symbol:C9ORF72&fields="),
                lastQuery.get());
        assertTrue(lastQuery.get().endsWith("&size=25"), lastQuery.get());
    }

    @Test
    void invalidSymbolIsRejectedBeforeAnyRequest() {
        assertThrows(InvalidSymbolException.class, () -> service().findGene("B*"));
        assertThrows(InvalidSymbolException.class,
                () -> service().findClinvarVariants("a:b", 10));
        assertThrows(InvalidSymbolException.class, () -> service().createGeneTask(""));
        assertThrows(InvalidSymbolException.class,
                () -> service().createClinvarTask(null, 10));
        assertNull(lastQuery.get());
    }

    @ParameterizedTest(name = "HTTP {0}")
    @CsvSource(delimiter = '|', value = {
            "400|{host} rejected the request (HTTP 400). Please check the gene symbol.",
            "404|Unexpected response from {host} (HTTP 404).",
            "429|Too many requests sent to {host}. Please wait a moment and try again.",
            "500|{host} is temporarily unavailable (HTTP 500). Please try again later.",
            "503|{host} is temporarily unavailable (HTTP 503). Please try again later.",
    })
    void explainsHttpErrors(int httpStatus, String expectedMessage) {
        status = httpStatus;

        ApiException error = assertThrows(ApiException.class, () -> service().findGene("BRCA1"));

        assertEquals(expectedMessage.replace("{host}", host), error.getMessage());
        assertEquals(httpStatus, error.status());
        assertEquals(ApiException.Reason.HTTP_ERROR, error.reason());
        assertEquals(host, error.host());
    }

    @Test
    void rejectsOutOfRangeVariantCounts() {
        assertThrows(IllegalArgumentException.class,
                () -> service().findClinvarVariants("BRCA1", 0));
        assertThrows(IllegalArgumentException.class,
                () -> service().createClinvarTask("BRCA1", MyGeneService.MAX_CLINVAR_SIZE + 1));
        assertNull(lastQuery.get());
    }

    @Test
    void reportsInvalidJson() {
        body = "<html>Maintenance</html>";

        ApiException error = assertThrows(ApiException.class, () -> service().findGene("BRCA1"));

        assertEquals("Received an invalid response from " + host + ".", error.getMessage());
        assertEquals(ApiException.Reason.INVALID_RESPONSE, error.reason());
    }

    @Test
    void reportsTimeout() {
        delayMillis = TEST_TIMEOUT.toMillis() * 4;

        ApiException error = assertThrows(ApiException.class, () -> service().findGene("BRCA1"));

        assertEquals(host + " did not respond in time. Please try again.", error.getMessage());
        assertEquals(0, error.status());
        assertEquals(ApiException.Reason.TIMEOUT, error.reason());
    }

    @Test
    void reportsUnreachableServer() throws IOException {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            closedPort = socket.getLocalPort();
        }
        String unreachable = "127.0.0.1:" + closedPort;

        ApiException error = assertThrows(ApiException.class,
                () -> serviceFor(unreachable).findGene("BRCA1"));

        assertEquals("Unable to reach " + unreachable + ". Please check your internet connection.",
                error.getMessage());
        assertEquals(ApiException.Reason.UNREACHABLE, error.reason());
    }

    @Test
    void interruptingTheThreadCancelsTheRequest() throws Exception {
        delayMillis = 10_000;
        CountDownLatch started = new CountDownLatch(1);
        CompletableFuture<Throwable> outcome = new CompletableFuture<>();

        Thread worker = new Thread(() -> {
            started.countDown();
            try {
                serviceFor(host).findGene("BRCA1");
                outcome.complete(null);
            } catch (Throwable t) {
                outcome.complete(t);
            }
        });
        worker.start();
        started.await();
        Thread.sleep(100);
        worker.interrupt();

        Throwable thrown = outcome.get(2, TimeUnit.SECONDS);
        assertInstanceOf(InterruptedException.class, thrown);
    }

    @Test
    void apiExceptionKeepsTheCause() {
        delayMillis = TEST_TIMEOUT.toMillis() * 4;

        ApiException error = assertThrows(ApiException.class, () -> service().findGene("BRCA1"));

        assertInstanceOf(HttpTimeoutException.class, error.getCause());
    }
}
