# Stock Control API

![Java](https://img.shields.io/badge/Java-17-red)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.x-brightgreen)
![JPA](https://img.shields.io/badge/JPA-Hibernate-blue)
![SQL Server](https://img.shields.io/badge/Database-SQL%20Server-lightgrey)
![Docker](https://img.shields.io/badge/Docker-Ready-blue)
![Security](https://img.shields.io/badge/Security-Firebase-orange)
![Build](https://img.shields.io/github/actions/workflow/status/JJOELJOAQUIN/stock-control-api/ci.yml?label=build)
![Coverage](https://img.shields.io/badge/Coverage-JaCoCo-yellowgreen)
![Quality](https://img.shields.io/badge/Code%20Quality-SonarQube-informational)

Backend profesional para gestión de inventario y caja con arquitectura limpia, seguridad basada en Firebase y preparado para producción.

---

## 🚀 Qué resuelve

- Inventario multi-contexto (`LOCAL` / `CONSULTORIO`)
- Movimientos de stock auditables
- Gestión de caja con retenciones automáticas
- Registro estructurado de gastos
- Operaciones comerciales transaccionales
- Autenticación JWT (Firebase) con roles persistidos

---

## 🏗 Arquitectura

Arquitectura en capas:

Controllers → Services → Repositories → Database

Principios aplicados:

- Separación dominio / infraestructura
- Concurrencia optimista (`@Version`)
- Anti-corruption layer
- Stateless security
- Transacciones consistentes
- Bounded Contexts (Inventory / Finance / Auth)

---

## 🧰 Stack Tecnológico

- Java 17  
- Spring Boot 3.4  
- Spring Data JPA  
- SQL Server  
- H2 (testing)  
- Firebase Authentication  
- Docker  
- JaCoCo (coverage)  
- GitHub Actions (CI/CD)  

---

## 🔐 Seguridad

- Firebase ID Token
- Roles persistidos (`ADMIN`, `USER`, `COSMETOLOGA`)
- Stateless
- Configurable por profile

Header requerido:

```bash
Authorization: Bearer <firebase_token>
```

---

## ▶️ Ejecutar Localmente

### Requisitos

- Java 17
- Maven 3.9+
- SQL Server activo

### Variables de entorno

```bash
SPRING_DATASOURCE_URL=jdbc:sqlserver://localhost:1433;databaseName=stock_control;encrypt=false;trustServerCertificate=true
SPRING_DATASOURCE_USERNAME=stock_user
SPRING_DATASOURCE_PASSWORD=TU_PASSWORD
SPRING_JPA_HIBERNATE_DDL_AUTO=update
SECURITY_FIREBASE_ENABLED=true
```

### Comando para correr el proyecto

```pruebas locales con profiles
mvn clean spring-boot:run -Dspring-boot.run.profiles=local


```bash
mvn clean spring-boot:run
```

Swagger disponible en:

```
http://localhost:8080/swagger-ui/index.html
```

---

## 🧪 Testing & Coverage

Perfil `test` utiliza H2 en memoria.

Ejecutar tests:

```bash
mvn test
```

Generar reporte de cobertura JaCoCo:

```bash
mvn clean verify
```

Reporte generado en:

```
target/site/jacoco/index.html
```

---

## ⚙️ CI/CD – GitHub Actions

El proyecto está preparado para integración continua mediante GitHub Actions.

Ejemplo de workflow (`.github/workflows/ci.yml`):

```yaml
name: CI Pipeline

on:
  push:
    branches: [ "main" ]
  pull_request:

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4
      - name: Set up Java 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
      - name: Build
        run: mvn clean verify
```

---

## 📦 Modelo de Dominio

### Product
Entidad raíz del inventario.  
Define alcance (`LOCAL`, `CONSULTORIO`, `BOTH`) y stock mínimo.

### StockEntity
Stock actual por producto y contexto.  

Restricción crítica:

```sql
UNIQUE(product_id, context)
```

Incluye control de concurrencia optimista.

### StockMovement
Historial auditable de movimientos (`INIT`, `IN`, `OUT`, `ADJUST`).

### CashMovement
Registro financiero asociado a operaciones.  
Calcula automáticamente retención y monto neto.

### Expense
Registro independiente de gastos operativos.

### AppUser
Usuario autenticado vía Firebase con rol persistido en base.

---

## 🐳 Docker

Estructura recomendada:

```
/docker
  /mssql-init
    01-init.sql
docker-compose.yml
Dockerfile
.env
```

Levantar entorno completo:

```bash
docker compose up --build
```

API disponible en:

```
http://localhost:8080
```

---

## 📊 Métricas y Calidad

El proyecto soporta integración con:

- JaCoCo (cobertura de tests)
- SonarQube / SonarCloud (análisis estático)
- GitHub Actions (pipeline CI)

Métricas recomendadas:

- Cobertura mínima: 70%+
- Sin vulnerabilidades críticas
- Sin code smells bloqueantes
- Sin duplicación > 5%

---

## 📡 Endpoints Principales

### Productos

| Método | Endpoint |
|--------|----------|
| POST   | /api/products |
| GET    | /api/products |
| GET    | /api/products/{id} |
| PATCH  | /api/products/{id} |
| DELETE | /api/products/{id} |
| GET    | /api/products/scan/{barcode} |

---

### Stock

| Método | Endpoint |
|--------|----------|
| POST | /api/stock/{productId}/init |
| GET  | /api/stock/{productId} |
| POST | /api/stock/{productId}/in |
| POST | /api/stock/{productId}/out |
| GET  | /api/stock/below-minimum |

---

### Operaciones Comerciales

| Método | Endpoint |
|--------|----------|
| POST | /api/business/sell |
| POST | /api/business/purchase |
| POST | /api/business/sell-by-barcode |

---

### Caja

| Método | Endpoint |
|--------|----------|
| POST | /api/cash-movements |
| GET  | /api/cash-movements |

---

### Gastos

| Método | Endpoint |
|--------|----------|
| POST | /api/expenses |
| GET  | /api/expenses |

---

## 🔄 Flujo de Venta

1. Buscar producto  
2. Validar alcance (`scope`)  
3. Verificar stock disponible  
4. Registrar movimiento OUT  
5. Registrar movimiento de caja IN  
6. Aplicar retención si corresponde  

Todo se ejecuta dentro de una única transacción.

---

## 🏢 Deploy Recomendado

Arquitectura sugerida:

```
VPS
 ├── Docker
 │    ├── stock_api
 │    └── sql_server
 └── Nginx (reverse proxy + SSL)
```

Variables productivas:

```bash
SECURITY_FIREBASE_ENABLED=true
SPRING_JPA_HIBERNATE_DDL_AUTO=validate
```

---

## 📈 Estado del Proyecto

✔ Arquitectura limpia  
✔ Inventario multi-contexto  
✔ Caja integrada  
✔ Movimientos auditables  
✔ Seguridad basada en roles  
✔ Dockerizable  
✔ CI/CD Ready  
✔ Cobertura con JaCoCo  

---