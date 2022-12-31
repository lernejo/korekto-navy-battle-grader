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
import org.awaitility.core.ConditionTimeoutException;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.awaitility.Awaitility.await;

public record Part7Grader(String name, Double maxGrade) implements PartGrader<LaunchingContext> {

    @Override
    public GradePart grade(LaunchingContext context) {
        if (context.httpServerFailed || context.httpClientFailed) {
            return result(List.of("Not trying to check due to previous errors"), 0.0D);
        }
        List<String> errors = new ArrayList<>();
        double grade = maxGrade();
        try
            (MavenExecutionHandle standaloneHandle = MavenExecutor.executeGoalAsync(context.getExercise(), context.getConfiguration().getWorkspace(),
                "org.codehaus.mojo:exec-maven-plugin:3.0.0:java -Dexec.mainClass='fr.lernejo.navy_battle.Launcher' -Dexec.arguments='"
                    + context.standalonePlayerPort + "'");
             NavyProxy navyProxy = NavyProxy.createStarted(context)
            ) {
            Ports.waitForPortToBeListenedTo(context.standalonePlayerPort, TimeUnit.SECONDS, LaunchingContext.serverStartTime());
            Ports.waitForPortToBeListenedTo(context.standaloneProxyPort, TimeUnit.SECONDS, LaunchingContext.serverStartTime());

            context.toStandaloneExchanges = navyProxy.toStandaloneExchanges;
            context.toSecondExchanges = navyProxy.toSecondExchanges;

            String serverLaunchInClientModeCli = "org.codehaus.mojo:exec-maven-plugin:3.0.0:java -Dexec.mainClass='fr.lernejo.navy_battle.Launcher' -Dexec.args='"
                + context.secondPlayerPort + " http://localhost:" + context.standaloneProxyPort + "'";
            try (MavenExecutionHandle secondPlayerHandle = MavenExecutor.executeGoalAsync(context.getExercise(), context.getConfiguration().getWorkspace(),
                serverLaunchInClientModeCli)
            ) {
                Ports.waitForPortToBeListenedTo(context.secondPlayerPort, TimeUnit.SECONDS, LaunchingContext.serverStartTime());
                try {
                    await().atMost(LaunchingContext.serverStartTime() / 3, TimeUnit.SECONDS).until(() -> !context.toSecondExchanges.isEmpty());
                    HttpEx.Request request = context.toSecondExchanges.get(0).request();

                    URI requestUri = uri(request.url());
                    Map<String, String> requestQuery = query(requestUri);
                    if (!"GET".equals(request.verb())) {
                        errors.add("Malformed Fire request sent to server, expecting method **GET** but was: `" + request.verb() + "`");
                        grade -= maxGrade() / 3;
                    }
                    String firePath = "/api/game/fire";
                    if (!firePath.equals(requestUri.getPath())) {
                        errors.add("Malformed Fire request sent to server, expecting path **" + firePath + "** but was: `" + requestUri.getPath() + "`");
                        grade -= maxGrade() / 3;
                    }
                    String cell = requestQuery.get("cell");
                    if (cell == null || cell.isBlank()) {
                        errors.add("Malformed Fire request sent to server, expecting a cell parameter, but non was found");
                        grade -= maxGrade() / 3;
                    }
                    String acceptHeader = request.headers().get("accept");
                    if (acceptHeader == null) {
                        errors.add("Malformed Fire request sent to server, missing **Accept** header");
                        grade -= maxGrade() / 8;
                    } else if (!acceptHeader.toLowerCase().contains("application/json")) {
                        errors.add("Malformed Fire request sent to server, wrong **Accept** header, expecting **application/json** but was `" + acceptHeader + "`");
                        grade -= maxGrade() / 8;
                    }
                    try {
                        await().atMost(LaunchingContext.serverStartTime(), TimeUnit.SECONDS).until(() -> noShipLeft(navyProxy));
                    } catch (ConditionTimeoutException e) {
                        // not an error immediately, we are using greedy consumption hee to avoid restarting servers in the next parts
                    }
                } catch (ConditionTimeoutException e) {
                    grade = 0;
                    errors.add("No fire call to the client player recorded after start game handshake");
                }
            } catch (RuntimeException e) {
                return result(List.of("Server (in client mode) failed to start within " + LaunchingContext.serverStartTime() + " sec."), 0.0D);
            } finally {
                Ports.waitForPortToBeFreed(context.secondPlayerPort, TimeUnit.SECONDS, 3L);
            }
        } catch (RuntimeException e) {
            return result(List.of("Server (standalone) failed to start within " + LaunchingContext.serverStartTime() + " sec."), 0.0D);
        } finally {
            Ports.waitForPortToBeFreed(context.standalonePlayerPort, TimeUnit.SECONDS, 3L);
        }
        return result(errors, grade);
    }

    private boolean noShipLeft(NavyProxy proxy) {
        return noShipLeft(proxy.toSecondExchanges) || noShipLeft(proxy.toStandaloneExchanges);
    }

    private boolean noShipLeft(List<HttpEx> ex) {
        return ex.get(ex.size() - 1).response().body().contains("false");
    }

    protected Map<String, String> query(URI uri) {
        String rawQuery = uri.getQuery();
        return Arrays.stream(rawQuery.split("&"))
            .filter(q -> !q.isBlank())
            .map(q -> q.split("="))
            .collect(Collectors.toMap(e -> e[0].toLowerCase(), e -> e[1]));
    }

    private URI uri(String rawUri) {
        return URI.create(rawUri);
    }
}
