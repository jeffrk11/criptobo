ARG CONFIG_FILE="config-test.yaml"

# Use uma imagem base do Java
FROM maven:3.9.0-eclipse-temurin-17 AS build

# Defina o diretório de trabalho no container
WORKDIR /app

COPY . .

RUN mvn clean package

FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY --from=build  /app/target/criptobo-1.0-SNAPSHOT.jar app.jar

# Comando para rodar a aplicação
ENTRYPOINT ["java", "-cp", "app.jar", "com.jeff.cripto.Main"]