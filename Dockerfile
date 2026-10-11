# Etapa 1: Compilación con Maven y Java 21
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Copiar el archivo de dependencias para aprovechar la caché de Docker
COPY pom.xml ./
RUN mvn dependency:go-offline -B

# Copiar el código fuente y empaquetar la aplicación
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa 2: Imagen ligera de ejecución con Java 21 JRE (Alpine)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copiar el JAR generado desde la etapa de compilación
COPY --from=builder /app/target/*.jar app.jar

# Exponer el puerto por defecto de Spring Boot
EXPOSE 8080

# Comando de ejecución
ENTRYPOINT ["java", "-jar", "app.jar"]