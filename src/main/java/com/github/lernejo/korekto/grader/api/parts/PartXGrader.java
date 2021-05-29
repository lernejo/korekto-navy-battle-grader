package com.github.lernejo.korekto.grader.api.parts;

import com.github.lernejo.korekto.grader.api.LaunchingContext;
import com.github.lernejo.korekto.toolkit.Exercise;
import com.github.lernejo.korekto.toolkit.GradePart;
import com.github.lernejo.korekto.toolkit.GradingConfiguration;
import com.github.lernejo.korekto.toolkit.thirdparty.git.GitContext;
import com.github.lernejo.korekto.toolkit.thirdparty.git.MeaninglessCommit;

import java.util.List;
import java.util.stream.Collectors;

public class PartXGrader implements PartGrader {

    @Override
    public String name() {
        return "Git (proper descriptive messages)";
    }

    @Override
    public double minGrade() {
        return -4.0D;
    }

    @Override
    public GradePart grade(GradingConfiguration configuration, Exercise exercise, LaunchingContext context, GitContext gitContext) {
        List<MeaninglessCommit> meaninglessCommits = gitContext.meaninglessCommits();
        List<String> messages = meaninglessCommits.stream()
            .map(mc -> '`' + mc.getShortId() + "` " + mc.getMessage() + " --> " + mc.getReason())
            .collect(Collectors.toList());
        if (messages.isEmpty()) {
            messages.add("OK");
        }
        return new GradePart(name(), Math.max(meaninglessCommits.size() * minGrade() / 8, minGrade()), null, messages);
    }
}
