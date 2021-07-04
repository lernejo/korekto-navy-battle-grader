package com.github.lernejo.korekto.grader.api.parts;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.lernejo.korekto.grader.api.LaunchingContext;
import com.github.lernejo.korekto.grader.api.NavyApiClient;
import com.github.lernejo.korekto.toolkit.Exercise;
import com.github.lernejo.korekto.toolkit.GradePart;
import com.github.lernejo.korekto.toolkit.GradingConfiguration;
import com.github.lernejo.korekto.toolkit.thirdparty.git.GitContext;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.List;

public class Part8Grader implements PartGrader {

    @Override
    public String name() {
        return "Part 8 - Fire API (server)";
    }

    @Override
    public Double maxGrade() {
        return 2.0D;
    }

    @Override
    public GradePart grade(GradingConfiguration configuration, Exercise exercise, LaunchingContext context, GitContext gitContext) {
        if (!context.attemptFireRequest) {
            return result(List.of("Not trying to check due to previous errors"), 0.0D);
        }
        Response<String> response = context.fireResponse;
        if (response == null) {
            return result(List.of("No response obtained within " + LaunchingContext.clientSocketTimeout() + " secs."), 0.0D);
        }

        if (!response.isSuccessful()) {
            return result(List.of("Expecting a successful response (2XX), but was: `" + response.code() + "`"), 0);
        }
        double grade = maxGrade();
        List<String> errors = new ArrayList<>();
        try {
            NavyApiClient.om.readValue(response.body(), NavyApiClient.FireResult.class);
            context.fireApiOk = true;
        } catch (JsonProcessingException e) {
            grade -= grade / 2;
            errors.add("Wrong response payload: " + e.getMessage());
        }
        String contentType = response.headers().get("Content-Type");
        String expectedContentType = "application/json";
        if (contentType == null) {
            errors.add("Malformed Fire response sent to client, missing **Content-Type** header");
            grade -= maxGrade() / 4;
        } else if (!contentType.contains(expectedContentType)) {
            grade -= maxGrade() / 4;
            errors.add("Expecting the _Content-Type_ header to contain **" + expectedContentType + "** but found: `" + contentType + "`");
        }

        return result(errors, grade);
    }
}
