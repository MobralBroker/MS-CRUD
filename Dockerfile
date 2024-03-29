# Estágio 1: Construção do JAR usando Maven
FROM maven:3.8.4-openjdk-17 AS builder

WORKDIR /ms-crud

# Copia apenas o arquivo POM para obter as dependências
COPY pom.xml .

# Baixa as dependências do Maven
RUN mvn dependency:go-offline

# Copia o resto do código e realiza o build
COPY src src
RUN mvn package -DskipTests

# Estágio 2: Criação da imagem final com o JAR construído
FROM openjdk:17-jdk-alpine

WORKDIR /ms-crud

# Copia apenas o JAR construído do estágio anterior
COPY --from=builder /ms-crud/target/*.jar app.jar

# Expondo a porta 8081
EXPOSE 8081

# Comando para executar a aplicação quando o contêiner for iniciado
CMD ["java", "-jar", "app.jar"]
