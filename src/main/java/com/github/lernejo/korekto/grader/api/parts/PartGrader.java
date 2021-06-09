package com.github.lernejo.korekto.grader.api.parts;

import com.github.lernejo.korekto.grader.api.LaunchingContext;
import com.github.lernejo.korekto.toolkit.Exercise;
import com.github.lernejo.korekto.toolkit.GradePart;
import com.github.lernejo.korekto.toolkit.GradingConfiguration;
import com.github.lernejo.korekto.toolkit.thirdparty.git.GitContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.TimeUnit;

public interface PartGrader {

    Logger LOGGER = LoggerFactory.getLogger(PartGrader.class);

    String name();

    default Double maxGrade() {
        return null;
    }

    default double minGrade() {
        return 0.0D;
    }

    GradePart grade(GradingConfiguration configuration, Exercise exercise, LaunchingContext context, GitContext gitContext);

    default GradePart result(List<String> explanations, double grade) {
        return new GradePart(name(), Math.min(Math.max(minGrade(), grade), maxGrade()), maxGrade(), explanations);
    }

    static void waitForPortToBeFreed(int port) {
        do {
            if (!isListened(port)) {
                return;
            }
            try {
                TimeUnit.MILLISECONDS.sleep(50L);
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
        } while (true);
    }

    static boolean isListened(int port) {
        try (Socket so = new Socket((String) null, port)) {
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }
}
