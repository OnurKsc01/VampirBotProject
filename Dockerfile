FROM maven:3.8.5-openjdk-17
WORKDIR /app
COPY . .
EXPOSE 8080
CMD ["mvn", "exec:java", "-Dexec.mainClass=com.vampiroyunu.Main"]
