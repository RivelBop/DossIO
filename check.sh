# Cleans the project builds to allow error-prone to run every time
./gradlew clean

# Checks the code style of the whole project (v12.0.1)
java -jar checkstyle.jar -c /google_checks.xml ./src/main/java/

# Runs error-prone
./gradlew compileJava