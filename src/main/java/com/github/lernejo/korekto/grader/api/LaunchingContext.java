package com.github.lernejo.korekto.grader.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.lernejo.korekto.grader.api.parts.HttpEx;
import com.github.lernejo.korekto.toolkit.GradingConfiguration;
import com.github.lernejo.korekto.toolkit.GradingContext;
import com.github.lernejo.korekto.toolkit.partgrader.MavenContext;
import okhttp3.OkHttpClient;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class LaunchingContext extends GradingContext implements MavenContext {


    public static final String START_ENDPOINT = "/api/game/start";

    public final int standalonePlayerPort;
    public final int standaloneProxyPort;
    public final int secondProxyPort;
    public final int secondPlayerPort;

    private boolean compilationFailed;
    private boolean testFailed;
    public boolean httpServerFailed;
    public boolean httpClientFailed;
    public List<HttpEx> toStandaloneExchanges = List.of();
    public List<HttpEx> toSecondExchanges = List.of();
    public Response<String> fireResponse;
    public boolean fireApiOk;
    public boolean attemptFireRequest;

    /**
     * In Secs.
     */
    public static long serverStartTime() {
        return Long.parseLong(System.getProperty("server_start_timeout", "3"));
    }

    /**
     * In Secs.
     */
    public static long clientSocketTimeout() {
        return Long.parseLong(System.getProperty("client_socket_timeout", "2"));
    }

    public LaunchingContext(GradingConfiguration configuration) {
        super(configuration);
        Random random = new Random();
        this.standalonePlayerPort = random.nextInt(600) + 7000;
        this.standaloneProxyPort = random.nextInt(600) + 8000;
        this.secondPlayerPort = random.nextInt(600) + 9000;
        this.secondProxyPort = random.nextInt(600) + 10000;
    }

    public static NavyApiClient newClient(int port) {
        OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(clientSocketTimeout(), TimeUnit.SECONDS)
            .readTimeout(clientSocketTimeout(), TimeUnit.SECONDS)
            .writeTimeout(clientSocketTimeout(), TimeUnit.SECONDS)
            .build();
        Retrofit retrofit = new Retrofit.Builder()
            .baseUrl("http://localhost:" + port + '/')
            .client(client)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(JacksonConverterFactory.create(new ObjectMapper().setDefaultLeniency(true)))
            .build();
        return retrofit.create(NavyApiClient.class);
    }

    @Override
    public boolean hasCompilationFailed() {
        return compilationFailed;
    }

    @Override
    public boolean hasTestFailed() {
        return testFailed;
    }

    @Override
    public void markAsCompilationFailed() {
        compilationFailed = true;
    }

    @Override
    public void markAsTestFailed() {
        testFailed = true;
    }
}
