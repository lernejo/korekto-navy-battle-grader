# Korekto Navy Battle grader

🆕 Korekto grader for Navy Battle project

[![Build](https://github.com/lernejo/korekto-navy-battle-grader/actions/workflows/ci.yml/badge.svg)](https://github.com/lernejo/korekto-navy-battle-grader/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/lernejo/korekto-navy-battle-grader/branch/main/graph/badge.svg?token=A6kYtPT5DX)](https://codecov.io/gh/lernejo/korekto-navy-battle-grader)
![License](https://img.shields.io/badge/License-Elastic_License_v2-blue)

Exercise subject: [here](EXERCISE_fr.adoc)

# How to launch
You will need these 2 env vars:
* `GH_LOGIN` your GitHub login
* `GH_TOKEN` a [**P**ersonal **A**ccess **T**oken](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens#creating-a-personal-access-token-classic) with permissions:
    * (classic) : `repo` and `user`
    * (fine-grained):
        * Repository permissions:
            * **Actions**: `Read-only`
            * **Contents**: `Read-only`

```bash
git clone git@github.com:lernejo/korekto-navy-battle-grader.git
cd korekto-navy-battle-grader
./mvnw compile exec:java -Dexec.args="-s=$GH_LOGIN" -Dgithub_token="$GH_TOKEN"
```

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
