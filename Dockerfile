FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Install Python and pip
RUN apk add --no-cache python3 py3-pip

# Copy the jar
COPY --from=build /app/target/*.jar app.jar

# Copy the AI agent code
COPY ai_agent ./ai_agent

# Install Python dependencies globally in the container (no need for venv inside Docker)
RUN pip install --no-cache-dir requests langgraph beautifulsoup4 feedparser langchain-core --break-system-packages

EXPOSE 8080
ENV SPRING_PROFILE=prod
# Update the path Java uses to find Python inside the container
ENV PYTHON_PATH=python3
ENV PYTHON_SCRIPT=ai_agent/TestAgain.py

ENTRYPOINT ["java", "-jar", "app.jar"]
