# Стадия сборки
FROM openjdk:21-jdk as builder

WORKDIR /app

# Копируем файлы для сборки
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
COPY webModule/pom.xml webModule/pom.xml
COPY paymentModule/pom.xml paymentModule/pom.xml

# Копируем спецификации API
COPY webModule/api-spec.yaml webModule/api-spec.yaml
COPY paymentModule/api-spec.yaml paymentModule/api-spec.yaml

# Даем права на выполнение mvnw
RUN chmod +x mvnw

# Скачиваем зависимости
RUN ./mvnw dependency:go-offline -B

# Копируем исходный код
COPY webModule/src webModule/src
COPY paymentModule/src paymentModule/src

# Собираем оба модуля
RUN ./mvnw clean package -DskipTests

# Финальный образ для webModule
FROM openjdk:21-jdk as web-app
COPY --from=builder /app/webModule/target/webModule-*.jar /app/web-app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/web-app.jar"]

# Финальный образ для paymentModule
FROM openjdk:21-jdk as payment-app
COPY --from=builder /app/paymentModule/target/paymentModule-*.jar /app/payment-app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/payment-app.jar"]