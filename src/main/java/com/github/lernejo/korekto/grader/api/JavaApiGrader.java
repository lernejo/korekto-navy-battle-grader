package com.github.lernejo.korekto.grader.api;

import com.github.lernejo.korekto.toolkit.*;
import com.github.lernejo.korekto.toolkit.thirdparty.git.GitContext;
import com.github.lernejo.korekto.toolkit.thirdparty.git.GitNature;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class JavaApiGrader implements Grader {

    @Override
    public void run(GradingConfiguration gradingConfiguration, GradingContext context) {
        Optional<GitNature> optionalGitNature = context.getExercise().lookupNature(GitNature.class);
        if (optionalGitNature.isEmpty()) {
            context.getGradeDetails().getParts().add(new GradePart("exercise", 0D, 12D, List.of("Not a Git project")));
        } else {
            GitNature gitNature = optionalGitNature.get();
            context.getGradeDetails().getParts().addAll(gitNature.withContext(c -> grade(gradingConfiguration, c, context.getExercise())));
        }
    }

    private Collection<? extends GradePart> grade(GradingConfiguration configuration, GitContext git, Exercise exercise) {
        return List.of();
    }

    @Override
    public Instant deadline(GradingContext context) {
        return Instant.parse("2021-05-28T23:59:00.00Z");
    }

    @Override
    public String slugToRepoUrl(String slug) {
        return "https://github.com/" + slug + "/java_api_training";
    }
}
