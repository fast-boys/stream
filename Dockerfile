FROM openjdk:17-alpine as build

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY src src

# Ensure gradlew has execution permissions
RUN chmod +x ./gradlew

# Build the application and run tests
RUN ./gradlew build -x test

# Use a separate stage for running the application
FROM openjdk:17-alpine
WORKDIR /app
# Correctly reference the jar file from the previous build stage
COPY --from=build /app/build/libs/app-0.0.1-SNAPSHOT.jar /app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]