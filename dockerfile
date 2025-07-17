# Usa una imagen base de OpenJDK para Java 17
# alpine es una distribución Linux ligera, ideal para imágenes Docker pequeñas
FROM openjdk:17-jdk-slim

# Establece el directorio de trabajo dentro del contenedor
WORKDIR /app

# Copia el archivo JAR de tu aplicación al contenedor
# Asume que el JAR se llamará "urbanback-0.0.1-SNAPSHOT.jar" después de hacer `mvn package`
# Debes reemplazar "urbanback-0.0.1-SNAPSHOT.jar" con el nombre real de tu JAR
COPY target/urban-back-0.0.1-SNAPSHOT.jar app.jar

# Expone el puerto en el que tu aplicación Spring Boot escuchará
EXPOSE 8080

# Comando para ejecutar la aplicación cuando se inicie el contenedor
# Usa un comando más simple para Docker sin opciones extra de Spring Boot
CMD ["java", "-jar", "app.jar"]

# Si necesitas pasar perfiles de Spring u otras propiedades:
# CMD ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
