# Urban Transport Backend

## Descripción del Proyecto

Este proyecto es el backend de una aplicación de gestión de transporte urbano, desarrollado con Spring Boot. Proporciona una API RESTful para la administración de clientes, conductores, vehículos y pedidos, incluyendo funcionalidades de autenticación (JWT), autorización basada en roles, auditoría de cambios y manejo de excepciones. Está diseñado para ser robusto, seguro y fácil de desplegar, utilizando Docker para la contenerización.

## Características Principales

* **Gestión de Usuarios:** Registro y autenticación de usuarios con roles (ADMIN, CONDUCTOR, CLIENTE).
* **Gestión de Clientes:** CRUD completo para perfiles de clientes, con control de estado activo/inactivo y auditoría de cambios.
* **Gestión de Conductores:** CRUD completo para perfiles de conductores, con control de estado activo/inactivo, asignación/desasignación de vehículos (limitado a 3 por conductor) y auditoría de cambios.
* **Gestión de Vehículos:** CRUD completo para vehículos, con control de estado activo/inactivo y auditoría de cambios.
* **Gestión de Pedidos:** Creación, actualización de estado, asignación de conductor y vehículo, y auditoría de pedidos.
* **Seguridad:** Implementación de Spring Security con JSON Web Tokens (JWT) para autenticación y autorización (basada en roles y en propiedad de recursos).
* **Auditoría:** Registro detallado de las operaciones de creación, actualización, eliminación y cambio de estado en entidades clave.
* **Manejo de Excepciones:** Gestión global de errores para proporcionar respuestas consistentes y descriptivas de la API.
* **Paginación:** Soporte para paginación en la recuperación de listas de entidades.
* **Documentación API:** Integración con Swagger/OpenAPI para una documentación interactiva de los endpoints.
* **Contenerización:** Configuración para despliegue con Docker y Docker Compose.

## Tecnologías Utilizadas

### Desarrollo y Backend
* **Spring Boot:** Framework principal para el desarrollo de aplicaciones Java.
* **Spring Security:** Para autenticación y autorización.
* **Spring Data JPA:** Para la capa de persistencia y ORM con bases de datos relacionales.
* **Hibernate:** Implementación de JPA por defecto.
* **JJWT (Java JWT):** Librería para la generación y validación de JSON Web Tokens.
* **MySQL Connector/J:** Driver JDBC para la conexión con MySQL.
* **Lombok:** Para reducir código boilerplate (getters, setters, constructores).
* **Jackson (fasterxml.jackson):** Para serialización y deserialización JSON.
* **Swagger/OpenAPI (Springdoc-openapi):** Para la documentación automática de la API.
* **Maven:** Herramienta de gestión de proyectos y construcción.
* **Java 17:** Lenguaje de programación.

### Pruebas
* **JUnit 5:** Framework de pruebas unitarias.
* **Mockito:** Para la creación de mocks y stubs en pruebas unitarias.
* **Spring Boot Test:** Para pruebas de integración y carga de contexto.
* **Spring Security Test:** Para simular usuarios autenticados en pruebas de controladores.

### Despliegue y Ejecución
* **Docker:** Para la contenerización de la aplicación y la base de datos.
* **Docker Compose:** Para la orquestación de múltiples contenedores (aplicación y base de datos).
* **MySQL 8.0:** Base de datos relacional.

## Requisitos

Para ejecutar este proyecto, necesitas tener instalado lo siguiente:

* **Git:** Para clonar el repositorio.
* **Java Development Kit (JDK) 17 o superior.**
* **Apache Maven 3.6.0 o superior.**
* **Docker Desktop (o Docker Engine y Docker Compose) 20.10.0 o superior.**

## Cómo Empezar

### 1. Clonar el Repositorio

```bash
git clone [https://github.com/fernanvergara/urban-back.git](https://github.com/fernanvergara/urban-back.git)
cd urban-back
```

### 2. Ejecutar con Docker Compose (Recomendado)
Esta es la forma más sencilla de levantar la aplicación junto con su base de datos MySQL.

1. **Construir el archivo JAR de Spring Boot:**
Asegúrate de estar en el directorio raíz del proyecto (urban-back).

```bash
mvn clean package -DskipTests
```
(-DskipTests es opcional, pero acelera el proceso al saltar los tests. Si quieres ejecutar los tests, omítelo.)

2. **Crear un archivo .env (Opcional pero recomendado):**
En el mismo directorio raíz donde se encuentra docker-compose.yml, crea un archivo llamado .env y añade tu secreto JWT. Esto es crucial para la seguridad en entornos de producción.

```
JWT_SECRET=tu_clave_secreta_muy_larga_y_segura_aqui_para_jwt_debe_ser_al_menos_64_caracteres_para_HS512
```
Si no creas este archivo, se usará un valor por defecto que está en docker-compose.yml, pero no es seguro para producción.

3.  **Levantar los contenedores con Docker Compose:**
    ```bash
    docker-compose up --build
    ```
    * `--build`: Reconstruye la imagen de la aplicación si hay cambios en el código o Dockerfile. Úsalo la primera vez o después de hacer cambios.
    * Este comando levantará dos servicios: `db` (MySQL) y `app` (tu aplicación Spring Boot).

4.  **Acceder a la Aplicación:**
    * La API estará disponible en: `http://localhost:8080`
    * La documentación interactiva de Swagger UI estará en: `http://localhost:8080/swagger-ui.html`
    * Puedes conectarte a la base de datos MySQL en `localhost:3306` con usuario `root` y contraseña `admin`.

5.  **Detener los Contenedores:**
    Para detener y eliminar los contenedores, redes y volúmenes creados por Docker Compose:
    ```bash
    docker-compose down
    ```
    Si solo quieres detener los contenedores sin eliminarlos:
    ```bash
    docker-compose stop
    ```

### 3. Configuración del Entorno de Desarrollo Local (sin Docker)

Si prefieres ejecutar la aplicación directamente en tu máquina sin Docker:

1.  **Instalar Requisitos:** Asegúrate de tener JDK 17 y Maven instalados (ver sección "Requisitos").

2.  **Configurar Base de Datos MySQL:**
    * Instala MySQL Server (versión 8.0 recomendada) en tu máquina local.
    * Crea una base de datos llamada `urban_transport_db`.
    * Crea un usuario y contraseña para la base de datos (por ejemplo, `user`/`password` o usa `root`/`admin` si lo prefieres para desarrollo local).

    ```sql
    CREATE DATABASE urban_transport_db;
    CREATE USER 'user'@'localhost' IDENTIFIED BY 'password';
    GRANT ALL PRIVILEGES ON urban_transport_db.* TO 'user'@'localhost';
    FLUSH PRIVILEGES;
    ```
    *(Ajusta el usuario y la contraseña según tu preferencia.)*

3.  **Configurar `application.properties`:**
    Abre el archivo `src/main/resources/application.properties` y asegúrate de que las propiedades de la base de datos coincidan con tu configuración local:

    ```properties
    spring.datasource.url=jdbc:mysql://localhost:3306/urban_transport_db?useSSL=false&serverTimezone=UTC
    spring.datasource.username=user
    spring.datasource.password=password
    spring.jpa.hibernate.ddl-auto=update # O 'none' o 'validate' para producción
    spring.jpa.show-sql=true

    # Configuración de JWT (asegúrate de que sea la misma que en Docker Compose o .env)
    jwt.secret=tu_clave_secreta_muy_larga_y_segura_aqui_para_jwt_debe_ser_al_menos_64_caracteres_para_HS512
    jwt.expiration=3600000
    ```
    **Importante:** Para `jwt.secret`, usa la misma clave que usarías en tu archivo `.env` para Docker Compose.

4.  **Ejecutar la Aplicación Spring Boot:**
    Desde el directorio raíz del proyecto:
    ```bash
    mvn spring-boot:run
    ```
    La aplicación se iniciará y estará disponible en `http://localhost:8080`.

## Consideraciones Importantes y Recomendaciones

* **Problema N+1 de JPA:**
    Aunque la aplicación es funcional, se recomienda optimizar las consultas JPA para evitar el problema N+1. Esto implica el uso estratégico de `JOIN FETCH` o `EntityGraphs` en los repositorios para cargar relaciones de forma eficiente y reducir el número de consultas a la base de datos, mejorando el rendimiento en escenarios de alta carga.

* **Secreto JWT en Producción:**
    La clave secreta JWT (`jwt.secret`) **debe ser una cadena de caracteres muy larga, compleja y generada de forma segura**. Para entornos de producción, **nunca debe estar hardcodeada** ni en `application.properties` ni en `docker-compose.yml`. Se recomienda encarecidamente gestionarla como una variable de entorno del sistema o a través de un servicio de gestión de secretos (como HashiCorp Vault, AWS Secrets Manager, Google Secret Manager, etc.).

* **Configuración de CORS:**
    La configuración de CORS actual en `SecurityConfig.java` permite solicitudes desde `http://localhost:3000` y `http://localhost:8080`. Para un despliegue en producción, asegúrate de **ajustar `setAllowedOrigins()`** para que solo incluya los dominios de tu aplicación frontend real.

* **Contraseñas por Defecto:**
    El inicializador de datos (`DataInitializer`) crea un usuario `admin` con contraseña `admin` por defecto si no existe. **En un entorno de producción, esta contraseña debe cambiarse inmediatamente** o eliminarse la inicialización de usuarios predeterminados.

## Licencia

Este proyecto se distribuye bajo la **Licencia MIT**. Puedes encontrar los detalles completos en el archivo [`LICENSE.md`](./LICENSE.md) en la raíz del repositorio.

## Contacto

Si tienes alguna pregunta o sugerencia, no dudes en contactarme a través de fernanvergara@gmail.com o abriendo un issue en este repositorio.

---
