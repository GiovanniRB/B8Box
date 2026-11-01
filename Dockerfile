# Imagem do B8Box para o deploy (Render).
# Etapa 1: compila o projeto com Maven + Java 21.
# Etapa 2: roda o .jar numa imagem menor, só com o Java.
# Os segredos NÃO entram na imagem: vêm das variáveis de ambiente do Render.

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# baixa as dependências primeiro (fica em cache enquanto o pom.xml não mudar)
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
# O plano gratuito do Render tem 512 MB: limita o heap a ~70% disso.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Xss512k"
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
