package com.github.lernejo.korekto.grader.api.parts;

import com.github.lernejo.korekto.grader.api.LaunchingContext;
import com.github.lernejo.korekto.grader.api.NavyApiClient;
import com.github.lernejo.korekto.toolkit.Exercise;
import com.github.lernejo.korekto.toolkit.GradePart;
import com.github.lernejo.korekto.toolkit.GradingConfiguration;
import com.github.lernejo.korekto.toolkit.PartGrader;
import com.github.lernejo.korekto.toolkit.thirdparty.git.GitContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public record Part9Grader(String name, Double maxGrade) implements PartGrader<LaunchingContext> {

    @Override
    public GradePart grade(LaunchingContext context) {
        if (context.toSecondExchanges.isEmpty() || !context.fireApiOk) {
            return result(List.of("Not trying to check due to previous errors"), 0.0D);
        }

        List<HttpEx> toSecondFires = getFireEx(context.toSecondExchanges);
        List<HttpEx> toStandaloneFires = getFireEx(context.toStandaloneExchanges);
        if (toStandaloneFires.isEmpty()) {
            return result(List.of("No fire call sent from the client player recorded"), 0.0D);
        }
        Optional<NavyApiClient.FireResult> toSecondLastFireResult = NavyApiClient.FireResult.parse(toSecondFires.get(toSecondFires.size() - 1).response().body());
        Optional<NavyApiClient.FireResult> toStandaloneLastFireResult = NavyApiClient.FireResult.parse(toStandaloneFires.get(toStandaloneFires.size() - 1).response().body());
        if (toSecondLastFireResult.isEmpty() || toStandaloneLastFireResult.isEmpty()) {
            return result(List.of("Bad fire response payload"), 0.0D);
        }

        double grade = maxGrade();
        List<String> errors = new ArrayList<>();
        if (toSecondLastFireResult.get().shipLeft() && toStandaloneLastFireResult.get().shipLeft()) {
            grade -= maxGrade() / 1.25D;
            errors.add("No convergence (end game) after " + (toStandaloneFires.size() + toSecondFires.size()) + " fires exchanged");
        }

        if (toSecondFires.size() > 100 || toStandaloneFires.size() > 100) {
            grade -= maxGrade() / 4D;
            errors.add("Worst than brute force algorithm (more than 100 fires were sent before end game)");
        }

        return result(errors, grade);
    }

    @NotNull
    private List<HttpEx> getFireEx(List<HttpEx> exs) {
        return exs.stream()
            .filter(ex -> ex.request().url().contains("/api/game/fire"))
            .collect(Collectors.toList());
    }
}
