FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY . .
# gradlew edited on Windows may have CRLF line endings, which Linux cannot run
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew
# tests need a live MySQL, so skip them inside the image build
RUN ./gradlew --no-daemon bootJar -x test

FROM eclipse-temurin:25-jre
WORKDIR /app
# the "-plain.jar" is not runnable, so match only the Spring Boot jar
COPY --from=build /app/build/libs/*-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
