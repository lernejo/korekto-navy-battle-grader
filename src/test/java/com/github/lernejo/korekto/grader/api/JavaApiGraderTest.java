package com.github.lernejo.korekto.grader.api;

import com.github.lernejo.korekto.toolkit.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled("using external Maven commands does not seems to work in GitHub CI, to investigate")
class JavaApiGraderTest {

    @BeforeEach
    void setUp() {
        String maven_home = System.getenv("MAVEN_HOME");
        if (maven_home != null && System.getProperty("maven.home") == null) {
            System.out.println("Setting ${maven.home}");
            System.setProperty("maven.home", maven_home);
        }
    }

    @Test
    void nominal_project() {
        Grader grader = Grader.Companion.load();
        String repoUrl = grader.slugToRepoUrl("lernejo");
        GradingConfiguration configuration = new GradingConfiguration(repoUrl, "", "", Paths.get("target/repositories"));

        AtomicReference<GradingContext> contextHolder = new AtomicReference<>();
        new GradingJob()
            .addCloneStep()
            .addStep("grading", grader)
            .addStep("report", (conf, context) -> contextHolder.set(context))
            .run(configuration);

        assertThat(contextHolder)
            .as("Grading context")
            .hasValueMatching(c -> c != null, "is present");

        assertThat(contextHolder.get().getGradeDetails().getParts())
            .containsExactly(
                new GradePart("Part 1 - Compilation & Tests", 4, 4.0D, List.of()),
                new GradePart("Part 2 - CI", 1, 1.0D, List.of()),
                new GradePart("Part 3 - Test Coverage", 2.4, 3.0D, List.of("Code coverage: 63.98%, expected: > 90% with `mvn verify`")),
                new GradePart("Part 4 - Ping", 2, 2.0D, List.of()),
                new GradePart("Part 5 - Start Game API (server)", 3, 3.0D, List.of()),
                new GradePart("Part 6 - Start Game API (client)", 2, 2.0D, List.of()),
                new GradePart("Part 7 - Fire API (client)", 4, 4.0D, List.of()),
                new GradePart("Part 8 - Fire API (server)", 2, 2.0D, List.of()),
                new GradePart("Part 9 - Game self convergence", 5, 5.0D, List.of()),
                new GradePart("Git (proper descriptive messages)", -0.0D, null, List.of("OK")),
                new GradePart("Coding style", -0.0D, null, List.of("OK"))
            );
    }
}
