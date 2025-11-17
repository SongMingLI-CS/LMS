# 1. 使用一个轻量级的 Java 21 镜像作为基础
FROM openjdk:21-jdk-slim

# 2. 暴露 Spring Boot 默认的 8080 端口
EXPOSE 8080

# 3. 设定容器内的工作目录
WORKDIR /app

# 4. (关键) 复制您的 JAR 包到镜像中
#    这个 ARG JAR_FILE 是为了在构建时接收 JAR 包的路径。
#    *.jar 确保它能匹配到 target 目录下打包出的 JAR 文件，
#    如 lms-backend-1.0.jar 或 lms-backend-0.0.1-SNAPSHOT.jar。
ARG JAR_FILE=target/lms-backend-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar

# 5. 定义容器启动时运行的命令
ENTRYPOINT ["java", "-jar", "app.jar"]