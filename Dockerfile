# Bước 1: Build với Maven 3.9 (bản mới hơn để tránh lỗi plugin)
FROM maven:3.9.5-amazoncorretto-17 AS build
COPY . .
# Thêm các cờ để bỏ qua lỗi annotation processor nếu có
RUN mvn clean package -DskipTests -Dmaven.compiler.proc=none

# Bước 2: Chạy ứng dụng
FROM amazoncorretto:17-al2-jdk
COPY --from=build /target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]