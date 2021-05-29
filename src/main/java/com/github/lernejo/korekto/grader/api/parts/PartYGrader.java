package com.github.lernejo.korekto.grader.api.parts;

import com.github.lernejo.korekto.grader.api.LaunchingContext;
import com.github.lernejo.korekto.toolkit.Exercise;
import com.github.lernejo.korekto.toolkit.GradePart;
import com.github.lernejo.korekto.toolkit.GradingConfiguration;
import com.github.lernejo.korekto.toolkit.thirdparty.git.GitContext;
import com.github.lernejo.korekto.toolkit.thirdparty.pmd.PmdExecutor;
import com.github.lernejo.korekto.toolkit.thirdparty.pmd.PmdReport;
import com.github.lernejo.korekto.toolkit.thirdparty.pmd.Rule;
import com.github.lernejo.korekto.toolkit.thirdparty.pmd.rules.ExcessiveClassLengthRule;
import com.github.lernejo.korekto.toolkit.thirdparty.pmd.rules.ExcessiveMethodLengthRule;
import com.github.lernejo.korekto.toolkit.thirdparty.pmd.rules.FieldMandatoryModifiersRule;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class PartYGrader implements PartGrader {

    @Override
    public String name() {
        return "Coding style";
    }

    @Override
    public double minGrade() {
        return -4.0D;
    }

    @Override
    public GradePart grade(GradingConfiguration configuration, Exercise exercise, LaunchingContext context, GitContext gitContext) {
        Optional<PmdReport> pmdReport = new PmdExecutor().runPmd(exercise,
            new Rule(
                ExcessiveClassLengthRule.class,
                "Class has {0} lines, exceeding the maximum of 90",
                Map.of("minimum", 92)
            ),
            new Rule(
                ExcessiveMethodLengthRule.class,
                "Method has {0} lines, exceeding the maximum of 15",
                Map.of("minimum", 17)
            ),
            new Rule(FieldMandatoryModifiersRule.class)
        );
        if (pmdReport.isEmpty()) {
            return new GradePart(name(), 0.0D, null, List.of("No analysis can be performed"));
        }
        long violations = pmdReport.get().getFileReports().stream().mapToLong(fr -> fr.getViolations().size()).sum();
        List<String> messages = pmdReport.get().getFileReports()
            .stream()
            .map(fr -> fr.getName() + fr.getViolations().stream().map(v -> "L." + v.getBeginLine() + ": " + v.getMessage().trim()).collect(Collectors.joining("\n            * ", "\n            * ", "")))
            .collect(Collectors.toList());
        if (messages.isEmpty()) {
            messages.add("OK");
        }
        return new GradePart(name(), Math.max(violations * minGrade() / 8, minGrade()), null, messages);
    }
}
