package com.github.lernejo.korekto.grader.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.lernejo.korekto.grader.api.parts.HttpEx;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import java.util.List;
import java.util.Random;

public class LaunchingContext {


    public static final String START_ENDPOINT = "/api/game/start";

    public final int standalonePlayerPort;
    public final int standaloneProxyPort;
    public final int secondProxyPort;
    public final int secondPlayerPort;

    public boolean compilationFailed;
    public boolean testFailed;
    public boolean httpServerFailed;
    public boolean httpClientFailed;
    public List<HttpEx> toStandaloneExchanges = List.of();
    public List<HttpEx> toSecondExchanges = List.of();
    public Response<String> fireResponse;
    public boolean fireApiOk;

    /**
     * In Secs.
     */
    public static long serverStartTime() {
        return Long.parseLong(System.getProperty("server_start_timeout", "3"));
    }

    public LaunchingContext() {
        Random random = new Random();
        this.standalonePlayerPort = random.nextInt(600) + 7000;
        this.standaloneProxyPort = random.nextInt(600) + 8000;
        this.secondPlayerPort = random.nextInt(600) + 9000;
        this.secondProxyPort = random.nextInt(600) + 10000;
    }

    public static NavyApiClient newClient(int port) {
        Retrofit retrofit = new Retrofit.Builder()
            .baseUrl("http://localhost:" + port + '/')
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(JacksonConverterFactory.create(new ObjectMapper().setDefaultLeniency(true)))
            .build();
        return retrofit.create(NavyApiClient.class);
    }
}
