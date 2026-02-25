Stock Control API

API REST para gestión de:

Inventario por contexto (LOCAL / CONSULTORIO)

Movimientos de stock auditables

Caja (ingresos / egresos / retenciones)

Gastos

Operaciones comerciales

Autenticación vía Firebase + roles persistidos

🏗 Arquitectura

Arquitectura en capas:

Controllers → Services → Repositories → Database
Características técnicas

Java 17

Spring Boot 3.4.x

Spring Data JPA

SQL Server (producción)

H2 (tests)

Seguridad stateless con Firebase

Control de concurrencia optimista (@Version)

Documentación OpenAPI (springdoc)

📦 Dominio
Product

Entidad raíz del inventario.

Maneja categoría, marca, scope (LOCAL / CONSULTORIO / BOTH)

Permite código de barras único

Soporta precio de costo opcional

StockEntity

Representa el stock actual de un producto en un contexto específico.

Restricción:

UNIQUE(product_id, context)
StockMovement

Historial auditable de movimientos.

Tipos:

INIT

IN

OUT

ADJUST

CashMovement

Movimiento de caja asociado a:

Venta

Pago proveedor

Gasto

Ajuste

Calcula automáticamente:

Retención (tarjetas)

Monto neto

Expense

Registro de gasto independiente.

AppUser

Usuario autenticado por Firebase con rol persistido en DB.

Roles:

ADMIN

USER

COSMETOLOGA

🔐 Seguridad

Autenticación mediante Firebase ID Token

Stateless

Roles almacenados en app_users

Filtro: FirebaseAuthenticationFilter

Seguridad desactivable por profile

Header requerido:

Authorization: Bearer <firebase_token>
🚀 Cómo ejecutar local
1️⃣ Requisitos

Java 17

Maven 3.9+

SQL Server 2021+

2️⃣ Variables de entorno
SPRING_DATASOURCE_URL=jdbc:sqlserver://localhost:1433;databaseName=stock_control;encrypt=false;trustServerCertificate=true
SPRING_DATASOURCE_USERNAME=stock_user
SPRING_DATASOURCE_PASSWORD=TU_PASSWORD
SPRING_JPA_HIBERNATE_DDL_AUTO=update
SECURITY_FIREBASE_ENABLED=true
3️⃣ Ejecutar
mvn spring-boot:run

Swagger:

http://localhost:8080/swagger-ui/index.html
🧪 Tests

Perfil test usa H2 en memoria.

Ejecutar:

mvn test

application-test.yml:

spring:
  datasource:
    url: jdbc:h2:mem:testdb
  jpa:
    hibernate:
      ddl-auto: create-drop

security:
  firebase:
    enabled: false
🐳 Docker
Estructura recomendada
/docker
  /mssql-init
    01-init.sql
docker-compose.yml
Dockerfile
.env
Levantar todo
docker compose up --build

API:

http://localhost:8080
📡 Endpoints principales
Productos
Método	Endpoint
POST	/api/products
GET	/api/products
GET	/api/products/{id}
PATCH	/api/products/{id}
DELETE	/api/products/{id}
GET	/api/products/scan/{barcode}
Stock
Método	Endpoint
POST	/api/stock/{productId}/init
GET	/api/stock/{productId}
POST	/api/stock/{productId}/in
POST	/api/stock/{productId}/out
GET	/api/stock/below-minimum
Operaciones comerciales
Método	Endpoint
POST	/api/business/sell
POST	/api/business/purchase
POST	/api/business/sell-by-barcode
Caja
Método	Endpoint
POST	/api/cash-movements
GET	/api/cash-movements
Gastos
Método	Endpoint
POST	/api/expenses
GET	/api/expenses
🔄 Flujo de Venta (Ejemplo)

Buscar producto

Validar scope

Verificar stock

Registrar movimiento OUT

Registrar movimiento de caja IN

Aplicar retención si es tarjeta

Todo en una única transacción.

📈 Características técnicas avanzadas

Concurrencia optimista en Stock

Separación de dominio vs infraestructura

Anti-corruption layer (JpaStockRepositoryAdapter)

Context mapping (CashContext.toStockContext())

Control de retención automática en tarjetas

Validaciones de dominio en Services

🏢 Deploy recomendado (Producción)

Arquitectura sugerida:

VPS
 ├── Docker
 │    ├── stock_api
 │    └── sql_server
 └── Nginx (reverse proxy + SSL)

Variables productivas:

SECURITY_FIREBASE_ENABLED=true

SPRING_JPA_HIBERNATE_DDL_AUTO=validate

📊 Estado del proyecto

✔ Inventario multi-contexto
✔ Caja integrada
✔ Movimientos auditables
✔ Seguridad por roles
✔ Dockerizable
✔ Documentación OpenAPI