package com.github.lernejo.korekto.grader.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface NavyApiClient {

    ObjectMapper om = new ObjectMapper();

    @GET("/ping")
    Call<String> getPing();

    @POST("/api/game/start")
    @Headers({
        "Accept: application/json",
        "Content-Type: application/json"
    })
    Call<GameServerInfo> startGame(@Body GameServerInfo self);

    @GET("/api/game/fire")
    @Headers({
        "Accept: application/json"
    })
    Call<String> fire(@Query("cell") String cell);

    record GameServerInfo(@JsonProperty("id") String id,
                          @JsonProperty("url") String url,
                          @JsonProperty("message") String message) {


        public GameServerInfo withPort(int destPort) {
            return new GameServerInfo(id, "http://localhost:" + destPort, message);
        }

        @SuppressWarnings("unchecked")
        public static Optional<Map<String, Object>> parseAsMap(String body) {
            try {
                return Optional.of(om.readValue(body, Map.class));
            } catch (JsonProcessingException e) {
                return Optional.empty();
            }
        }

        public static Optional<GameServerInfo> parse(String body) {
            try {
                return Optional.of(om.readValue(body, NavyApiClient.GameServerInfo.class));
            } catch (JsonProcessingException e) {
                return Optional.empty();
            }
        }

        public static GameServerInfo self(int port) {
            return new GameServerInfo(
                UUID.randomUUID().toString(),
                "http://localhost:" + port,
                "You may win this battle, but I shall win the war"
            );
        }

        public String toJson() {
            try {
                return om.writeValueAsString(this);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Unable to serialize " + GameServerInfo.class, e);
            }
        }
    }

    record FireResult(@JsonProperty("consequence") String consequence,
                      @JsonProperty("shipLeft") boolean shipLeft) {
        public static Optional<FireResult> parse(String body) {
            try {
                return Optional.of(om.readValue(body, FireResult.class));
            } catch (JsonProcessingException e) {
                return Optional.empty();
            }
        }

        public enum Consequence {
            miss, hit, sunk;
        }
    }

}
