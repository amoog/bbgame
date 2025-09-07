This is Bigbank scripting test assignment solution. (https://dragonsofmugloar.com/)

Java 21 SDK and access to Maven Central repository is required to build this application.

In the repository root, 

To build application:

./gradlew clean build

To run tests:

./gradlew test

To run application

./gradlew run

Application has a few configurable parameters that can be changed and are described in file bbgame.properties.

Application gathers game history in file gamehistory.json and uses this information to select tasks to attempt. If this file is empty or very small then game scores can be low. After a few runs game scores will improve considerably.

