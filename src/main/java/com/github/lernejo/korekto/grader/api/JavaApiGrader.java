package com.github.lernejo.korekto.grader.api;

import com.github.lernejo.korekto.grader.api.parts.*;
import com.github.lernejo.korekto.toolkit.Grader;
import com.github.lernejo.korekto.toolkit.GradingConfiguration;
import com.github.lernejo.korekto.toolkit.PartGrader;
import com.github.lernejo.korekto.toolkit.partgrader.*;
import com.github.lernejo.korekto.toolkit.thirdparty.pmd.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

public class JavaApiGrader implements Grader<LaunchingContext> {

    private final Logger logger = LoggerFactory.getLogger(JavaApiGrader.class);

    @Override
    public String name() {
        return "⛵ Navy Battle project";
    }

    @Override
    public LaunchingContext gradingContext(GradingConfiguration configuration) {
        return new LaunchingContext(configuration);
    }

    public Collection<PartGrader<LaunchingContext>> graders() {
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
            new PmdPartGrader<>("Coding style", -20.0D, -1.0D,
                Rule.buildExcessiveClassLengthRule(90),
                Rule.buildExcessiveMethodLengthRule(15),
                Rule.buildFieldMandatoryModifierRule(0, "private", "final", "!static"),
                Rule.buildClassNamingConventionsRule(),
                Rule.buildMethodNamingConventionsRule(),
                Rule.buildDependencyInversionRule(),
                Rule.buildUnusedPrivateMethodRule(),
                Rule.buildUnusedPrivateFieldRule(),
                Rule.buildUnusedLocalVariableRule(),
                Rule.buildEmptyControlStatementRule()
            )
        );
    }

    @Override
    public String slugToRepoUrl(String slug) {
        return "https://github.com/" + slug + "/navy_battle_project";
    }

    @Override
    public boolean needsWorkspaceReset() {
        return true;
    }
}
