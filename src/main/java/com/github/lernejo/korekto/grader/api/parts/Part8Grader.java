package com.github.lernejo.korekto.grader.api.parts;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.lernejo.korekto.grader.api.LaunchingContext;
import com.github.lernejo.korekto.grader.api.NavyApiClient;
import com.github.lernejo.korekto.toolkit.GradePart;
import com.github.lernejo.korekto.toolkit.PartGrader;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.List;

public record Part8Grader(String name, Double maxGrade) implements PartGrader<LaunchingContext> {

    private static final Logger logger = LoggerFactory.getLogger(Part8Grader.class);

    @Override
    public GradePart grade(LaunchingContext context) {
        try {
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
            if (response.body() == null) {
                return result(List.of("Expecting a body, but none was returned"), 0);
            }
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
        } catch(RuntimeException e) {
            logger.warn("Unknown error", e);
            return result(List.of("Unknown error: " + ExceptionUtils.getStackTrace(e)), 0.0D);
        }
    }
}
