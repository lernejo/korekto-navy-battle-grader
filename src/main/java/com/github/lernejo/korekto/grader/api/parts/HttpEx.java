package com.github.lernejo.korekto.grader.api.parts;

import java.util.Map;

public record HttpEx(Request request, Response response) {

    public static record Request(String verb, String url, Map<String, String> headers, String body) {
    }

    public static record Response(int code, Map<String, String> headers, String body) {
    }
}
