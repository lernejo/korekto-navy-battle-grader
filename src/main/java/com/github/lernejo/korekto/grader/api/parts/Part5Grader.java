package com.github.lernejo.korekto.grader.api.parts;

import com.github.lernejo.korekto.grader.api.LaunchingContext;
import com.github.lernejo.korekto.grader.api.NavyApiClient;
import com.github.lernejo.korekto.toolkit.Exercise;
import com.github.lernejo.korekto.toolkit.GradePart;
import com.github.lernejo.korekto.toolkit.GradingConfiguration;
import com.github.lernejo.korekto.toolkit.misc.Ports;
import com.github.lernejo.korekto.toolkit.thirdparty.git.GitContext;
import com.github.lernejo.korekto.toolkit.thirdparty.maven.MavenExecutionHandle;
import com.github.lernejo.korekto.toolkit.thirdparty.maven.MavenExecutor;
import retrofit2.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Part5Grader implements PartGrader {
    @Override
    public String name() {
        return "Part 5 - Start Game API (server)";
    }

    @Override
    public Double maxGrade() {
        return 3.0D;
    }

    @Override
    public GradePart grade(GradingConfiguration configuration, Exercise exercise, LaunchingContext context, GitContext gitContext) {
        if (context.httpServerFailed) {
            return result(List.of("Not trying to check due to previous errors"), 0.0D);
        }
        try
            (MavenExecutionHandle ignored = MavenExecutor.executeGoalAsync(exercise, configuration.getWorkspace(),
                "org.codehaus.mojo:exec-maven-plugin:3.0.0:java -Dexec.mainClass='fr.lernejo.navy_battle.Launcher' -Dexec.arguments='"
                    + context.standalonePlayerPort + "'")) {
            Ports.waitForPortToBeListenedTo(context.standalonePlayerPort, TimeUnit.SECONDS, LaunchingContext.SERVER_START_TIMEOUT);

            NavyApiClient client = LaunchingContext.newClient(context.standalonePlayerPort);
            double grade = maxGrade();
            List<String> errors = new ArrayList<>();
            try {
                Response<NavyApiClient.GameServerInfo> response = client.startGame(NavyApiClient.GameServerInfo.self(context.secondProxyPort)).execute();
                if (response.code() != 202) {
                    grade -= maxGrade() / 3;
                    errors.add("Expecting `/api/game/start` to respond with a **202** code, but found: `" + response.code() + "`");
                }
                if (response.body().url() == null || !response.body().url().contains(String.valueOf(context.standalonePlayerPort))) {
                    grade -= maxGrade() / 2;
                    errors.add("Expecting `/api/game/start` response body to contains the server URL **http://localhost:"
                        + context.standalonePlayerPort + "** but found: `" + response.body().url() + "`");
                }
            } catch (RuntimeException e) {
                grade /= 2;
                errors.add("Bad response payload: " + e.getMessage());
            }
            try {
                context.fireResponse = client.fire("I3").execute();
            } catch (RuntimeException e) {
                // do nothing, it's a greedy call for part 8
            }
            return result(errors, grade);
        } catch (RuntimeException e) {
            context.httpServerFailed = true;
            return result(List.of("Server (standalone) failed to start within " + LaunchingContext.SERVER_START_TIMEOUT + " sec."), 0.0D);
        } catch (IOException e) {
            context.httpServerFailed = true;
            return result(List.of("Fail to call server: " + e.getMessage()), 0.0D);
        }
    }
}
