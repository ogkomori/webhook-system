FROM eclipse-temurin:25-jdk-jammy
WORKDIR /api
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml ./
RUN ./mvnw dependency:go-offline
COPY src ./src
EXPOSE 8080
CMD ["./mvnw","spring-boot:run"]