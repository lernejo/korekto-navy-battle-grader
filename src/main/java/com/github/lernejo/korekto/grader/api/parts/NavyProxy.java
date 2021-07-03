package com.github.lernejo.korekto.grader.api.parts;

import com.github.lernejo.korekto.grader.api.LaunchingContext;
import com.github.lernejo.korekto.grader.api.NavyApiClient;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

public class NavyProxy implements AutoCloseable, ProxyConfiguration {

    private static final Set<String> DISALLOWED_HEADERS = Set.of("connection", "content-length", "expect", "host", "upgrade");

    public final List<HttpEx> toStandaloneExchanges = new ArrayList<>();
    public final List<HttpEx> toSecondExchanges = new ArrayList<>();
    private final HttpServer standaloneProxy;
    private final HttpServer secondProxy;
    private boolean noForwardMode;
    private final Set<ExecutorService> services = new HashSet<>();

    private NavyProxy(int standaloneProxyPort, int secondProxyPort, int standalonePlayerPort, int secondPlayerPort) {
        this.secondProxy = startHttpServer(secondProxyPort, secondPlayerPort, standaloneProxyPort, toSecondExchanges);
        this.standaloneProxy = startHttpServer(standaloneProxyPort, standalonePlayerPort, secondProxyPort, toStandaloneExchanges);
    }

    static NavyProxy createStarted(LaunchingContext context) {
        return new NavyProxy(context.standaloneProxyPort, context.secondProxyPort, context.standalonePlayerPort, context.secondPlayerPort);
    }

    private HttpServer startHttpServer(int proxyPort, int destPort, int returnPort, List<HttpEx> logs) {
        final HttpServer server;
        try {
            server = HttpServer.create(new InetSocketAddress(proxyPort), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        services.add(executorService);
        server.setExecutor(executorService);
        server.createContext("/", new CallHandler(proxyPort, destPort, returnPort, this, logs));
        server.start();
        return server;
    }

    @Override
    public void close() {
        standaloneProxy.stop(0);
        secondProxy.stop(0);
        services.forEach(s -> s.shutdownNow());
    }

    public NavyProxy noForwardMode() {
        noForwardMode = true;
        return this;
    }

    @Override
    public boolean doForward() {
        return !noForwardMode;
    }

    private static class CallHandler implements HttpHandler {

        private final int selfPort;
        private final int destPort;
        private final int returnPort;
        private final ProxyConfiguration conf;
        private final List<HttpEx> logs;
        private final HttpClient client;

        public CallHandler(int selfPort, int destPort, int returnPort, ProxyConfiguration conf, List<HttpEx> logs) {
            this.selfPort = selfPort;
            this.destPort = destPort;
            this.returnPort = returnPort;
            this.conf = conf;
            this.logs = logs;

            client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(1))
                .build();
        }

        private static String readInputStream(InputStream is) {
            StringBuilder textBuilder = new StringBuilder();
            try (Reader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8.name()))) {
                int c = 0;
                while ((c = reader.read()) != -1) {
                    textBuilder.append((char) c);
                }
                return textBuilder.toString();
            } catch (IOException e) {
                return null;
            }
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                String originalBody = readInputStream(exchange.getRequestBody());
                if (conf.doForward()) {
                    forwardPostRequestAndRespond(exchange, originalBody);
                } else {
                    notFound(exchange, originalBody);
                }
            } else if ("GET".equals(exchange.getRequestMethod())) {
                if (conf.doForward()) {
                    forwardGetRequestAndRespond(exchange);
                } else {
                    notFound(exchange, null);
                }
            }
        }

        private void forwardPostRequestAndRespond(HttpExchange exchange, String originalRequestBody) throws IOException {
            Optional<Map<String, Object>> requestPayload = NavyApiClient.GameServerInfo.parseAsMap(originalRequestBody);

            String body;
            if (requestPayload.isPresent() && requestPayload.get().containsKey("url")) {
                requestPayload.get().put("url", "http://localhost:" + returnPort);
                body = NavyApiClient.om.writeValueAsString(requestPayload.get());
            } else {
                body = originalRequestBody;
            }
            try {
                HttpResponse<String> response = forwardRequest(exchange, b -> b.POST(HttpRequest.BodyPublishers.ofString(body)).build());
                Optional<Map<String, Object>> responsePayload = NavyApiClient.GameServerInfo.parseAsMap(response.body());
                String newResponseBody;
                if(responsePayload.isPresent() && responsePayload.get().containsKey("url")) {
                    responsePayload.get().put("url", "http://localhost:" + selfPort);
                    newResponseBody = NavyApiClient.om.writeValueAsString(responsePayload.get());
                } else {
                    newResponseBody = response.body();
                }
                recordAndRespond(exchange, originalRequestBody, response, newResponseBody);
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted server thread", e);
            }
        }

        private void forwardGetRequestAndRespond(HttpExchange exchange) throws IOException {
            try {
                HttpResponse<String> response = forwardRequest(exchange, b -> b.GET().build());
                recordAndRespond(exchange, null, response, response.body());
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted server thread", e);
            }
        }

        private void recordAndRespond(HttpExchange exchange, String originalRequestBody, HttpResponse<String> response, String newResponseBody) throws
            IOException {
            logs.add(new HttpEx(
                new HttpEx.Request(exchange.getRequestMethod().toUpperCase(), exchange.getRequestURI().toString(), toMap(exchange.getRequestHeaders()), originalRequestBody),
                new HttpEx.Response(response.statusCode(), toMap(response.headers().map()), response.body())));
            response.headers().map().forEach((k, v) -> exchange.getResponseHeaders().add(k, toRawHeaderValue(v)));
            exchange.sendResponseHeaders(response.statusCode(), newResponseBody.length());
            OutputStream os = exchange.getResponseBody();
            os.write(newResponseBody.getBytes());
            os.close();
        }

        private void notFound(HttpExchange exchange, String originalBody) throws IOException {
            logs.add(new HttpEx(
                new HttpEx.Request(exchange.getRequestMethod().toUpperCase(), exchange.getRequestURI().toString(), toMap(exchange.getRequestHeaders()), originalBody),
                null));
            exchange.sendResponseHeaders(404, 0);
            exchange.getResponseBody().close();
        }

        private HttpResponse<String> forwardRequest(HttpExchange
                                                        exchange, Function<HttpRequest.Builder, HttpRequest> requestBuilder) throws
            IOException, InterruptedException {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + destPort + exchange.getRequestURI().getPath() + "?" + exchange.getRequestURI().getQuery()))
                .timeout(Duration.ofSeconds(1));
            exchange.getRequestHeaders()
                .forEach((k, v) -> {
                    if (!DISALLOWED_HEADERS.contains(k.toLowerCase())) {
                        builder.header(k, toRawHeaderValue(v));
                    }
                });
            HttpRequest request = requestBuilder.apply(builder);
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response;
        }

        private Map<String, String> toMap(Map<String, List<String>> headers) {
            return headers.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().toLowerCase(), e -> toRawHeaderValue(e.getValue())));
        }

        private String toRawHeaderValue(List<String> v) {
            return String.join(",", v);
        }
    }
}

interface ProxyConfiguration {
    boolean doForward();
}
