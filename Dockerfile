FROM openjdk:21-jdk

WORKDIR /app

# Копируем
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
COPY webModule/pom.xml webModule/pom.xml

# Даем права на выполнение mvnw
RUN chmod +x mvnw

# Скачиваем зависимости отдельно для кэширования
RUN ./mvnw dependency:go-offline -B

# Копируем исходный код
COPY webModule/src webModule/src

# Собираем приложение (используем mvnw вместо mvn)
RUN ./mvnw clean package -DskipTests

WORKDIR /app/webModule

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "target/webModule-0.0.1-SNAPSHOT.jar"]