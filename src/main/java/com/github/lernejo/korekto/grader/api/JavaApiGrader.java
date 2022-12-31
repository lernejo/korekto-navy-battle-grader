package com.github.lernejo.korekto.grader.api;

import com.github.lernejo.korekto.grader.api.parts.*;
import com.github.lernejo.korekto.toolkit.*;
import com.github.lernejo.korekto.toolkit.PartGrader;
import com.github.lernejo.korekto.toolkit.misc.HumanReadableDuration;
import com.github.lernejo.korekto.toolkit.partgrader.*;
import com.github.lernejo.korekto.toolkit.thirdparty.git.GitContext;
import com.github.lernejo.korekto.toolkit.thirdparty.git.GitNature;
import com.github.lernejo.korekto.toolkit.thirdparty.pmd.Rule;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class JavaApiGrader implements Grader<LaunchingContext> {

    private final Logger logger = LoggerFactory.getLogger(JavaApiGrader.class);

    @Override
    public LaunchingContext gradingContext(GradingConfiguration configuration) {
        return new LaunchingContext(configuration);
    }

    @Override
    public void run(LaunchingContext context) {
        context.getGradeDetails().getParts().addAll(grade(context));
    }

    private Collection<? extends GradePart> grade(LaunchingContext context) {
        return graders().stream()
            .map(g -> applyPartGrader(context, g))
            .collect(Collectors.toList());
    }

    private GradePart applyPartGrader(LaunchingContext context, PartGrader<LaunchingContext> g) {
        long startTime = System.currentTimeMillis();
        try {
            return g.grade(context);
        } finally {
            logger.debug(g.name() + " in " + HumanReadableDuration.toString(System.currentTimeMillis() - startTime));
        }
    }

    private Collection<? extends PartGrader<LaunchingContext>> graders() {
        return List.of(
            new MavenCompileAndTestPartGrader<>(
                "Part 1 - Compilation & Tests",
                4.0D),
            new GitHubActionsPartGrader<>("Part 2 - CI", 1.0D),
            new JacocoCoveragePartGrader<>("Part 3 - Code Coverage", 3.0D, 0.8D),
            new Part4Grader("Part 4 - Ping", 2.0D),
            new Part5Grader("Part 5 - Start Game API (server)", 3.0D),
            new Part6Grader("Part 6 - Start Game API (client)", 2.0D),
            new Part7Grader("Part 7 - Fire API (client)", 4.0D),
            new Part8Grader("Part 8 - Fire API (server)", 2.0D),
            new Part9Grader("Part 9 - Game self convergence", 5.0D),
            new GitHistoryPartGrader<>("Git (proper descriptive messages)", -4.0D),
            new PmdPartGrader<>("Coding style", -4.0D,
                Rule.buildExcessiveClassLengthRule(92),
                Rule.buildExcessiveMethodLengthRule(17),
                Rule.buildFieldMandatoryModifierRule(0, "private", "final", "!static")
            )
        );
    }

    @Override
    public String slugToRepoUrl(String slug) {
        return "https://github.com/" + slug + "/java_api_training";
    }

    @Override
    public boolean needsWorkspaceReset() {
        return true;
    }
}
