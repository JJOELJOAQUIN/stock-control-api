Stock Control API

Backend profesional para gestión de inventario y caja con arquitectura limpia y seguridad basada en Firebase.

---


1. Introducción

Stock Control API es una aplicación backend diseñada para la gestión integral de inventario y operaciones financieras en entornos multi-contexto.

El sistema integra control de stock, registro auditable de movimientos, gestión de caja y autenticación segura basada en Firebase.

---

2. Objetivos del Sistema

Garantizar consistencia transaccional en operaciones comerciales.

Permitir inventario independiente por contexto.

Registrar auditoría completa de movimientos.

Integrar flujo financiero con control de retenciones.

Implementar seguridad stateless y control de roles.


---

3. Arquitectura
3.1 Arquitectura en Capas

Controllers → Services → Repositories → Database

Controllers

Exponen endpoints REST.

Services

Contienen lógica de negocio y validaciones de dominio.

Repositories

Adaptadores JPA desacoplados del dominio.

3.2 Bounded Contexts
Inventory Context

Product

StockEntity

StockMovement

Finance Context

CashMovement

Expense

Authentication Context

AppUser

Role

---

4. Modelo de Dominio
4.1 Product

Entidad raíz del agregado Inventory.

Responsabilidades:

Definir alcance del producto.

Mantener stock mínimo.

Soportar código de barras único.

Permitir precio de costo opcional.

4.2 StockEntity

Representa estado actual del inventario por producto y contexto.

Restricción crítica:

UNIQUE(product_id, context)

Incluye control de concurrencia optimista.

4.3 StockMovement

Historial auditable de movimientos.

Tipos soportados:

INIT

IN

OUT

ADJUST

4.4 CashMovement

Modelo financiero vinculado a operaciones comerciales.

Funcionalidades:

Cálculo automático de retención.

Cálculo de monto neto.

Asociación opcional a producto.

4.5 Expense

Entidad independiente para gastos operativos.

Soporta gastos recurrentes.

---


5. Seguridad
5.1 Modelo

Autenticación con Firebase ID Token

Validación mediante filtro personalizado

Persistencia local de rol

Stateless

5.2 Control de Acceso

Basado en roles:

ADMIN

USER

COSMETOLOGA

---


6. Consistencia Transaccional

Las operaciones comerciales ejecutan:

Validación de producto

Validación de contexto

Modificación de stock

Registro de movimiento

Registro de movimiento financiero

Todo dentro de una única transacción.

---

7. Concurrencia

Se utiliza @Version en StockEntity para prevenir:

Escrituras concurrentes inconsistentes

Condiciones de carrera

---

8. Testing Strategy

Perfil test con H2 en memoria.

create-drop para aislamiento completo.

Seguridad desactivada en tests.

---

9. Deploy Strategy

Entorno recomendado:

VPS
└── Docker
├── stock_api
├── sql_server
└── Nginx (reverse proxy + SSL)

Configuración productiva:

SECURITY_FIREBASE_ENABLED=true

SPRING_JPA_HIBERNATE_DDL_AUTO=validate

---

10. Conclusión Técnica

El sistema implementa:

Modelado de dominio coherente

Arquitectura desacoplada

Persistencia robusta

Seguridad profesional

Escalabilidad horizontal vía Docker

Se encuentra preparado para:

Producción

Escalabilidad

Integración futura con frontend o microservicios