# 1. 使用轻量级 JRE 21 基础镜像（openjdk:* 官方镜像已停止维护）
FROM eclipse-temurin:21-jre-jammy

# 2. 以非 root 用户运行
RUN groupadd --system --gid 1001 lms \
    && useradd --system --uid 1001 --gid lms --create-home lms

WORKDIR /app

# 3. 复制构建产物（由 mvn package 生成）
ARG JAR_FILE=target/lms-backend-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar

RUN chown -R lms:lms /app
USER lms

# 4. 暴露 Spring Boot 默认端口
EXPOSE 8080

# 5. 启动（容器内存感知 + 无颜色输出便于日志采集）
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
