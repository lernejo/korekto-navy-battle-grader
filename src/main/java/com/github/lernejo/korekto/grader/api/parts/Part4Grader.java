package com.github.lernejo.korekto.grader.api.parts;

import com.github.lernejo.korekto.grader.api.LaunchingContext;
import com.github.lernejo.korekto.toolkit.Exercise;
import com.github.lernejo.korekto.toolkit.GradePart;
import com.github.lernejo.korekto.toolkit.GradingConfiguration;
import com.github.lernejo.korekto.toolkit.PartGrader;
import com.github.lernejo.korekto.toolkit.misc.Ports;
import com.github.lernejo.korekto.toolkit.thirdparty.git.GitContext;
import com.github.lernejo.korekto.toolkit.thirdparty.maven.MavenExecutionHandle;
import com.github.lernejo.korekto.toolkit.thirdparty.maven.MavenExecutor;
import retrofit2.Response;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

public record Part4Grader(String name, Double maxGrade) implements PartGrader<LaunchingContext> {

    @Override
    public GradePart grade(LaunchingContext context) {
        if (context.hasCompilationFailed()) {
            context.httpServerFailed = true;
            context.httpClientFailed = true;
            return result(List.of("Not trying to start server as compilation failed"), 0.0D);
        }
        Path launcherPath = context.getExercise().getRoot().resolve("src/main/java/fr/lernejo/navy_battle/Launcher.java");
        if (!Files.exists(launcherPath)) {
            context.httpServerFailed = true;
            context.httpClientFailed = true;
            return result(List.of("Class `fr.lernejo.navy_battle.Launcher` does not exists."), 0.0D);
        } else if (!readContent(launcherPath).contains("fr.lernejo.navy_battle")) {
            context.httpServerFailed = true;
            context.httpClientFailed = true;
            return result(List.of("File **fr/lernejo/navy_battle/Launcher.java** does not have the correct package."), 0.0D);
        }
        try
            (MavenExecutionHandle handle = MavenExecutor.executeGoalAsync(context.getExercise(), context.getConfiguration().getWorkspace(),
                "org.codehaus.mojo:exec-maven-plugin:3.0.0:java -Dexec.mainClass='fr.lernejo.navy_battle.Launcher' -Dexec.arguments='"
                    + context.standalonePlayerPort + "'")) {
            Ports.waitForPortToBeListenedTo(context.standalonePlayerPort, TimeUnit.SECONDS, LaunchingContext.serverStartTime());

            Response<String> pingResponse = LaunchingContext.newClient(context.standalonePlayerPort).getPing().execute();
            if (pingResponse.isSuccessful()) {
                String body = pingResponse.body();
                if ("OK".equals(body)) {
                    return result(List.of(), maxGrade());
                } else {
                    return result(List.of("Expected /ping to respond **OK** but was: `" + body + "`"), maxGrade() / 2);
                }
            } else {
                return result(List.of("/ping response code is " + pingResponse.code()), 0.0D);
            }
        } catch (RuntimeException e) {
            context.httpServerFailed = true;
            return result(List.of("Server (standalone) failed to start within " + LaunchingContext.serverStartTime() + " sec."), 0.0D);
        } catch (IOException e) {
            return result(List.of("Fail to call server: " + e.getMessage()), 0.0D);
        } finally {
            Ports.waitForPortToBeFreed(context.standalonePlayerPort, TimeUnit.SECONDS, 3L);
        }
    }

    private String readContent(Path launcherPath) {
        try {
            return Files.readString(launcherPath);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to read file: " + launcherPath, e);
        }
    }
}
