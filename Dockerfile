FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY build/libs/*.jar app.jar
# агент нужен для работы дебага
ENTRYPOINT ["java", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5004", "-jar", "app.jar"]
#EXPOSE 8080
USER nobody