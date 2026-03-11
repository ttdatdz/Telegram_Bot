# Bước 1: Build project với Maven dùng Amazon Corretto (ổn định hơn)
FROM maven:3.8.4-amazoncorretto-17 AS build
COPY . .
RUN mvn clean package -DskipTests

# Bước 2: Chạy ứng dụng dùng Amazon Corretto 17
FROM amazoncorretto:17-al2-jdk
COPY --from=build /target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]