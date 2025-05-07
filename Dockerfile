FROM openjdk:21-jdk

WORKDIR /app

# Копируем Maven Wrapper (обязательно!)
COPY .mvn .mvn
COPY mvnw .
COPY pom.xml .

# Даем права на выполнение mvnw
RUN chmod +x mvnw

# Скачиваем зависимости отдельно для кэширования
RUN ./mvnw dependency:go-offline -B

# Копируем исходный код
COPY src src

# Собираем приложение (используем mvnw вместо mvn)
RUN ./mvnw clean package -DskipTests

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "target/myShop-0.0.1-SNAPSHOT.jar"]