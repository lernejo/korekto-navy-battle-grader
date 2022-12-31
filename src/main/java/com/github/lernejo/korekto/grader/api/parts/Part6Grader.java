package com.github.lernejo.korekto.grader.api.parts;

import com.github.lernejo.korekto.grader.api.LaunchingContext;
import com.github.lernejo.korekto.grader.api.NavyApiClient;
import com.github.lernejo.korekto.toolkit.Exercise;
import com.github.lernejo.korekto.toolkit.GradePart;
import com.github.lernejo.korekto.toolkit.GradingConfiguration;
import com.github.lernejo.korekto.toolkit.PartGrader;
import com.github.lernejo.korekto.toolkit.misc.Ports;
import com.github.lernejo.korekto.toolkit.thirdparty.git.GitContext;
import com.github.lernejo.korekto.toolkit.thirdparty.maven.MavenExecutionHandle;
import com.github.lernejo.korekto.toolkit.thirdparty.maven.MavenExecutor;
import org.awaitility.core.ConditionTimeoutException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

public record Part6Grader(String name, Double maxGrade) implements PartGrader<LaunchingContext> {

    @Override
    public GradePart grade(LaunchingContext context) {
        if (context.httpClientFailed) {
            return result(List.of("Not trying to check due to previous errors"), 0.0D);
        }
        String standaloneUrl = "http://localhost:" + context.standaloneProxyPort;
        String serverLaunchInClientModeCli = "org.codehaus.mojo:exec-maven-plugin:3.0.0:java -Dexec.mainClass='fr.lernejo.navy_battle.Launcher' -Dexec.args='"
            + context.secondPlayerPort + " " + standaloneUrl + "'";

        try (NavyProxy navyProxy = NavyProxy.createStarted(context).noForwardMode()) {
            Ports.waitForPortToBeListenedTo(context.standaloneProxyPort, TimeUnit.SECONDS, LaunchingContext.serverStartTime());
            try (MavenExecutionHandle secondPlayerHandle = MavenExecutor.executeGoalAsync(context.getExercise(), context.getConfiguration().getWorkspace(), serverLaunchInClientModeCli)) {
                Ports.waitForPortToBeListenedTo(context.secondPlayerPort, TimeUnit.SECONDS, LaunchingContext.serverStartTime());
                try {
                    await().atMost(LaunchingContext.serverStartTime() / 3, TimeUnit.SECONDS).until(() -> !navyProxy.toStandaloneExchanges.isEmpty());
                    HttpEx.Request request = navyProxy.toStandaloneExchanges.get(0).request();
                    double grade = maxGrade();
                    List<String> errors = new ArrayList<>();
                    if (!request.url().contains(LaunchingContext.START_ENDPOINT)) {
                        grade -= maxGrade() / 3;
                        errors.add("Expecting a request on **" + LaunchingContext.START_ENDPOINT + "** but was on `" + request.url() + "`");
                    }
                    Optional<NavyApiClient.GameServerInfo> gameServerInfo = NavyApiClient.GameServerInfo.parse(request.body());
                    if (gameServerInfo.isEmpty()) {
                        grade -= maxGrade() / 2;
                        errors.add("Bad client request body, expecting a JSON matching given schema but found:\n```\n" + request.body() + "\n```");
                    } else if (!gameServerInfo.get().url().contains(String.valueOf(context.secondPlayerPort))) {
                        grade -= maxGrade() / 3;
                        errors.add("Expecting request body to contain a self referenced URL (on **" + context.secondPlayerPort + "**), but found: `" + gameServerInfo.get().url() + "`");
                    }
                    return result(errors, grade);
                } catch (ConditionTimeoutException e) {
                    return result(List.of("No request made to instance (@" + context.standaloneProxyPort + ") when passing a second parameter: `" + standaloneUrl + "`"), 0.0D);
                }
            } catch(CancellationException e) {
                context.httpServerFailed = true;
                return result(List.of("Second player (@" + context.secondPlayerPort + ") failed to start within " + LaunchingContext.serverStartTime() + " sec."), 0.0D);
            } finally {
                Ports.waitForPortToBeFreed(context.secondPlayerPort, TimeUnit.SECONDS, 3L);
            }
        }  catch(CancellationException e) {
            context.httpServerFailed = true;
            return result(List.of("First player (@" + context.standalonePlayerPort + ") failed to start within " + LaunchingContext.serverStartTime() + " sec."), 0.0D);
        }
    }
}
