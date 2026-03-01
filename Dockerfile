FROM maven:3.8.5-openjdk-17
WORKDIR /app
COPY . .
# Önce bütün kodları derle ve hazırla
RUN mvn clean compile
EXPOSE 8080
# Sonra oyunu başlat
CMD ["mvn", "exec:java", "-Dexec.mainClass=com.vampiroyunu.Main"]
