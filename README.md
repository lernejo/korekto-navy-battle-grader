# Korekto Java API grader

Korekto grader for Java Api exercise

[![Build](https://github.com/lernejo/korekto-java-api-grader/actions/workflows/build.yml/badge.svg)](https://github.com/lernejo/korekto-java-api-grader/actions)
[![codecov](https://codecov.io/gh/lernejo/korekto-java-api-grader/branch/main/graph/badge.svg?token=A6kYtPT5DX)](https://codecov.io/gh/lernejo/korekto-java-api-grader)

## Launch locally

To launch the tool locally, run `com.github.lernejo.korekto.toolkit.launcher.GradingJobLauncher` with the
argument `-s=mySlug`

### With Maven

```bash
mvn compile exec:java -Dexec.mainClass="com.github.lernejo.korekto.toolkit.launcher.GradingJobLauncher" -Dexec.args="-s=mySlug"
```

### With intelliJ

![Demo Run Configuration](https://raw.githubusercontent.com/lernejo/korekto-toolkit/main/docs/demo_run_configuration.png)


### Possible errors & workaround

##### Retrofit illegal access occurring on JDK16-openJ9 (curiously, not occurring with hotspot).
Related issues:
* https://github.com/square/retrofit/issues/3448
* https://github.com/square/retrofit/issues/3535
* https://github.com/square/retrofit/issues/3557

Symptoms :
```
[WARNING]
java.lang.ExceptionInInitializerError
    at java.lang.J9VMInternals.ensureError (J9VMInternals.java:184)
    at java.lang.J9VMInternals.recordInitializationFailure (J9VMInternals.java:173)
    at retrofit2.Retrofit$Builder.<init> (Retrofit.java:441)
    at com.github.lernejo.korekto.grader.api.LaunchingContext.newClient (LaunchingContext.java:48)
...
Caused by: java.lang.reflect.InaccessibleObjectException: Unable to make java.lang.invoke.MethodHandles$Lookup(java.lang.Class,int) accessible: module java.base does not "opens java.lang.invoke" to unnamed module @4f772fe7
    at java.lang.reflect.AccessibleObject.checkCanSetAccessible (AccessibleObject.java:357)
    at java.lang.reflect.AccessibleObject.checkCanSetAccessible (AccessibleObject.java:297)
    at java.lang.reflect.Constructor.checkCanSetAccessible (Constructor.java:188)
    at java.lang.reflect.Constructor.setAccessible (Constructor.java:181)
    at retrofit2.Platform.<init> (Platform.java:59)
```

Workaround :

* launch with the `--add-opens=java.base/java.lang.invoke=ALL-UNNAMED` JVM option
* with Maven : `(export MAVEN_OPTS="--add-opens=java.base/java.lang.invoke=ALL-UNNAMED" && mvn compile exec:java -Dexec.mainClass="com.github.lernejo.korekto.toolkit.launcher.GradingJobLauncher" -Dexec.args="-s=mySlug")`
