package com.github.lernejo.korekto.grader.api;

import com.github.lernejo.korekto.grader.api.parts.*;
import com.github.lernejo.korekto.toolkit.*;
import com.github.lernejo.korekto.toolkit.misc.HumanReadableDuration;
import com.github.lernejo.korekto.toolkit.thirdparty.git.GitContext;
import com.github.lernejo.korekto.toolkit.thirdparty.git.GitNature;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class JavaApiGrader implements Grader {

    private final Logger logger = LoggerFactory.getLogger(JavaApiGrader.class);

    @Override
    public void run(GradingConfiguration gradingConfiguration, GradingContext context) {
        Optional<GitNature> optionalGitNature = context.getExercise().lookupNature(GitNature.class);
        if (optionalGitNature.isEmpty()) {
            context.getGradeDetails().getParts().add(new GradePart("exercise", 0D, 12D, List.of("Not a Git project")));
        } else {
            GitNature gitNature = optionalGitNature.get();
            context.getGradeDetails().getParts().addAll(gitNature.withContext(c -> grade(gradingConfiguration, context.getExercise(), c)));
        }
    }

    private Collection<? extends GradePart> grade(GradingConfiguration configuration, Exercise exercise, GitContext gitContext) {
        LaunchingContext context = buildLaunchingContext();
        return graders().stream()
            .map(g -> applyPartGrader(configuration, exercise, gitContext, context, g))
            .collect(Collectors.toList());
    }

    private GradePart applyPartGrader(GradingConfiguration configuration, Exercise exercise, GitContext gitContext, LaunchingContext context, PartGrader g) {
        long startTime = System.currentTimeMillis();
        try {
            return g.grade(configuration, exercise, context, gitContext);
        } finally {
            logger.debug(g.name() + " in " + HumanReadableDuration.toString(System.currentTimeMillis() - startTime));
        }
    }

    private Collection<? extends PartGrader> graders() {
        return List.of(
            new Part1Grader(),
            new Part2Grader(),
            new Part3Grader(),
            new Part4Grader(),
            new Part5Grader(),
            new Part6Grader(),
            new Part7Grader(),
            new Part8Grader(),
            new Part9Grader(),

            new PartXGrader(),
            new PartYGrader()
        );
    }

    @NotNull
    private LaunchingContext buildLaunchingContext() {
        LaunchingContext context = new LaunchingContext();
        return context;
    }

    @Override
    public Instant deadline(GradingContext context) {
        return Instant.parse("2021-07-11T19:59:00.00Z");
    }

    @Override
    public String slugToRepoUrl(String slug) {
        return "https://github.com/" + slug + "/java_api_training";
    }
}
