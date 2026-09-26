# franquicias-api

API REST **reactiva y funcional** para administrar franquicias, sucursales y sus productos.
Spring Boot 3 + WebFlux + R2DBC + MySQL 8, con tests automatizados, empaquetado Docker e
infraestructura como código con Terraform.

- **Modelo:** `franquicia` → 1..N `sucursales` → 1..N `productos`
- **Persistencia:** MySQL 8 en local/Docker y **RDS MySQL** en la nube (Terraform)
- **Programación reactiva:** WebFlux + R2DBC (`DatabaseClient`), rutas `RouterFunction` (100 % funcional, sin controladores anotados)
- **Contrato de errores:** [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457) `application/problem+json`

---

## 1. Requisitos

| Herramienta | Versión mínima | Notas |
|---|---|---|
| JDK | 17 | Se probó con JDK 22 |
| Maven | 3.9 | — |
| Docker + Docker Compose | 24 / v2 | Solo para la opción "todo en contenedores" |
| Terraform | 1.6 | Solo para desplegar en AWS |

No hace falta instalar MySQL: el perfil por defecto usa **H2 en memoria** y `docker compose` levanta
MySQL 8 por ti.

## 2. Arranque rápido

### Opción A — sin Docker (H2 en memoria)

```bash
mvn spring-boot:run
```

Queda escuchando en <http://localhost:8080>.

### Opción B — Docker Compose (MySQL 8 real)

```bash
docker compose up -d          # MySQL 8 + la API ya conectada
curl http://localhost:8080/actuator/health
docker compose logs -f app
docker compose down           # detener;  docker compose down -v  borra también el volumen
```

MySQL queda expuesto en el puerto **3307** del host (3306 suele estar ocupado por una instalación
local) para que puedas conectarte con un cliente si lo necesitas.

### Opción C — MySQL local

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

Configurable por variables de entorno, sin tocar código:

```bash
MYSQL_HOST=localhost MYSQL_PORT=3306 MYSQL_DATABASE=franquicias \
MYSQL_USER=franquicias MYSQL_PASSWORD=franquicias \
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

### Opción D — build manual

```bash
mvn clean package                                   # genera target/franquicias-api-1.0.0.jar
java -jar target/franquicias-api-1.0.0.jar
```

> En Windows `mvn clean` puede fallar de forma intermitente al borrar `target/` por una carrera de
> handles; por eso el proyecto configura `maven-clean-plugin` con `failOnError=false`. Si aun así
> molesta: `rmdir /s /q target`.

## 3. Pruebas

```bash
mvn test           # 48 pruebas: 33 de API, 8 de reglas de negocio, 7 de normalización
mvn clean verify   # clean + tests + jar
```

Las pruebas de API arrancan la aplicación real contra H2 (`RANDOM_PORT`) y comprueban los criterios
de aceptación de punta a punta. Las de reglas de negocio usan implementaciones en memoria de los
puertos de repositorio: sin Spring, sin base de datos y sin mocks.

## 4. Endpoints

Base: `http://localhost:8080/api`

### Franquicias

| Método | Ruta | Descripción | Respuesta |
|---|---|---|---|
| `POST` | `/api/franquicias` | Crear franquicia | `201` + `Location` |
| `GET` | `/api/franquicias` | Listar franquicias | `200` |
| `GET` | `/api/franquicias/{franquiciaId}` | Detalle con sucursales | `200` |
| `PATCH` | `/api/franquicias/{franquiciaId}` | **Renombrar franquicia** | `200` |

### Sucursales

| Método | Ruta | Descripción | Respuesta |
|---|---|---|---|
| `POST` | `/api/franquicias/{franquiciaId}/sucursales` | Crear sucursal | `201` + `Location` |
| `GET` | `/api/franquicias/{franquiciaId}/sucursales` | Listar sucursales | `200` |
| `GET` | `/api/franquicias/{franquiciaId}/sucursales/{sucursalId}` | Ver sucursal | `200` |
| `PATCH` | `/api/franquicias/{franquiciaId}/sucursales/{sucursalId}` | **Renombrar sucursal** | `200` |
| `DELETE` | `/api/franquicias/{franquiciaId}/sucursales/{sucursalId}` | Eliminar sucursal | `204` |

### Productos

| Método | Ruta | Descripción | Respuesta |
|---|---|---|---|
| `POST` | `/api/franquicias/{id}/sucursales/{id}/productos` | Crear producto | `201` + `Location` |
| `GET` | `/api/franquicias/{id}/sucursales/{id}/productos` | Listar productos | `200` |
| `GET` | `/api/franquicias/{id}/sucursales/{id}/productos/{id}` | Ver producto | `200` |
| `PATCH` | `/api/franquicias/{id}/sucursales/{id}/productos/{id}` | **Renombrar producto** | `200` |
| `DELETE` | `/api/franquicias/{id}/sucursales/{id}/productos/{id}` | **Eliminar producto** | `204` |
| `PATCH` | `/api/franquicias/{id}/sucursales/{id}/productos/{id}/stock` | **Actualizar stock** | `200` |
| `PUT` | `/api/franquicias/{id}/sucursales/{id}/productos/{id}/stock` | Alias de actualizar stock | `200` |
| `GET` | `/api/franquicias/{id}/productos-mayor-stock` | **Producto con más stock por sucursal** | `200` |

`GET /api` devuelve el índice de endpoints. `GET /actuator/health` devuelve la salud.

### Recorrido completo con `curl`

```bash
# 1. Crear franquicia
FR=$(curl -s -X POST localhost:8080/api/franquicias \
  -H 'Content-Type: application/json' -d '{"nombre":"Krispy Kreme"}')
FID=$(echo "$FR" | jq -r .id)

# 2. Crear dos sucursales
CE=$(curl -s -X POST localhost:8080/api/franquicias/$FID/sucursales \
  -H 'Content-Type: application/json' -d '{"nombre":"Centro"}' | jq -r .id)
NO=$(curl -s -X POST localhost:8080/api/franquicias/$FID/sucursales \
  -H 'Content-Type: application/json' -d '{"nombre":"Norte"}' | jq -r .id)

# 3. Crear productos
curl -s -X POST localhost:8080/api/franquicias/$FID/sucursales/$CE/productos \
  -H 'Content-Type: application/json' -d '{"nombre":"Donas glaseadas","stock":10}'
curl -s -X POST localhost:8080/api/franquicias/$FID/sucursales/$CE/productos \
  -H 'Content-Type: application/json' -d '{"nombre":"Cafe","stock":50}'
PID=$(curl -s -X POST localhost:8080/api/franquicias/$FID/sucursales/$NO/productos \
  -H 'Content-Type: application/json' -d '{"nombre":"Empanada","stock":20}' | jq -r .id)

# 4. Actualizar stock
curl -s -X PATCH localhost:8080/api/franquicias/$FID/sucursales/$CE/productos/$PID/stock \
  -H 'Content-Type: application/json' -d '{"stock":5}'

# 5. Producto con más stock de cada sucursal de la franquicia
curl -s localhost:8080/api/franquicias/$FID/productos-mayor-stock

# 6. Eliminar producto
curl -s -o /dev/null -w '%{http_code}\n' -X DELETE \
  localhost:8080/api/franquicias/$FID/sucursales/$NO/productos/$PID    # 204
```

Respuesta del criterio 7 (un producto por sucursal, incluyendo el nombre de la sucursal):

```json
[
  { "idSucursal": "…", "sucursal": "Centro", "idProducto": "…", "producto": "Cafe", "stock": 50 },
  { "idSucursal": "…", "sucursal": "Norte",  "idProducto": "…", "producto": "Empanada", "stock": 20 }
]
```

Si dos productos de la misma sucursal empatan con el máximo, ambos aparecen. Orden:
`stock` descendente, luego nombre de sucursal y nombre de producto ascendentes.

Hay una colección de Postman en [`docs/franquicias.postman_collection.json`](docs/franquicias.postman_collection.json)
que encadena las llamadas y guarda los ids automáticamente.

## 5. Reglas de negocio

- **Nombres únicos y normalizados:** `"Krispy Kreme"`, `"krispy  kreme"` y `"KRISPY KREME"` son el
  mismo nombre. La comparación ignora mayúsculas, acentos y espacios sobrantes (columna `name_key`
  + índice único), por lo que un duplicado responde `409`, no `400`.
- **Stock no negativo:** validado en la capa de servicio y reforzado con `CHECK (stock >= 0)`.
- **Integridad referencial:** una sucursal pertenece a una franquicia y un producto a una sucursal.
  Los `id` de la URL deben pertenecer a la cadena indicada; si no, `404` (no se fían ids sueltos).
- **Borrado en cascada:** al eliminar una sucursal se eliminan sus productos; al eliminar una
  franquicia, sus sucursales y productos.
- **Nombres en blanco o JSON inválido:** `400` con el detalle de cada campo.

## 6. Contrato de errores

Todo error responde `application/problem+json` según RFC 9457:

```json
{
  "type": "https://franquicias-api.dev/errors/409",
  "title": "Conflicto",
  "status": 409,
  "detail": "Ya existe una franquicia con el nombre \"Krispy Kreme\"",
  "instance": "/api/franquicias",
  "timestamp": "2026-01-01T00:00:00Z",
  "errors": ["nombre: es obligatorio"]
}
```

| Código | Cuándo |
|---|---|
| `400` | Body ausente, JSON inválido, campo en blanco, stock negativo |
| `404` | Franquicia, sucursal o producto inexistente, o que no pertenece a la ruta indicada |
| `409` | Nombre duplicado o violación de unicidad en la base de datos |
| `405` / `415` | Método o `Content-Type` no soportado |
| `500` | Error no controlado (queda registrado en el log) |

## 7. Decisiones de diseño

- **Rutas funcionales, no anotaciones.** Todo son `RouterFunction` + `HandlerFunction`; el
  `RequestValidator` resuelve el body y aplica Bean Validation antes de tocar el servicio.
- **Programación reactiva de punta a punta.** No hay `block()` en producción: `Mono`/`Flux`,
  `DatabaseClient` y R2DBC. El pool de conexiones se activa con `r2dbc-pool`.
- **Arquitectura hexagonal.** El dominio no sabe que existe R2DBC: los servicios dependen de
  interfaces (`FranchiseRepository`, `BranchRepository`, `ProductRepository`) y los adaptadores
  R2DBC las implementan. Por eso los tests de reglas de negocio corren sin base de datos.
- **IDs generados por la aplicación** (UUID v4) en lugar de `AUTO_INCREMENT`: el mismo esquema
  sirve para H2, MySQL y cualquier motor relacional, y los ids se pueden crear en el servicio
  antes de insertar.
- **Un DDL por motor** (`db/schema-h2.sql` y `db/schema-mysql.sql`): H2 acepta
  `CREATE INDEX IF NOT EXISTS` y MySQL no, así que los índices se declaran dentro del
  `CREATE TABLE` en el DDL de MySQL. El resto del esquema es idéntico.
- **Sin transacciones explícitas:** cada operación es una única sentencia, que en R2DBC es atómica
  por sí sola. La unicidad se garantiza con el índice único, no con una comprobación previa.

## 8. Estructura del proyecto

```
src/main/java/com/franquicias
├── FranquiciasApplication.java
├── api/            # rutas funcionales, DTOs de entrada/salida y validación
├── domain/         # Franchise, Branch, Product, TopStockProduct (records)
├── exception/      # NotFound, Conflict, InvalidRequest y el manejador global RFC 9457
├── repository/     # puertos (interfaces)
│   └── r2dbc/      # adaptadores R2DBC con DatabaseClient
├── service/        # reglas de negocio
└── support/        # normalización de nombres y generación de ids

src/main/resources
├── application.yml           # H2 por defecto
├── application-mysql.yml     # perfil "mysql"
└── db/schema-h2.sql, db/schema-mysql.sql

terraform/          # ECS Fargate + ALB + RDS MySQL + ECR en AWS
docs/               # colección Postman
```

## 9. Despliegue en la nube (AWS con Terraform)

`terraform/` provisiona ECR, un clúster ECS Fargate detrás de un Application Load Balancer, RDS
MySQL (cifrado, con backups) y autoescalado por CPU.

```bash
cd terraform
terraform init
terraform plan  -var 'region=us-east-1'
terraform apply -var 'region=us-east-1'
```

Después, publicar la imagen y actualizar el servicio:

```bash
ACCOUNT=$(aws sts get-caller-identity --query Account --output text)
REGION=us-east-1
aws ecr get-login-password --region $REGION \
  | docker login --username AWS --password-stdin $ACCOUNT.dkr.ecr.$REGION.amazonaws.com

docker build -t $ACCOUNT.dkr.ecr.$REGION.amazonaws.com/franquicias-api:latest -t franquicias-api .
docker push $ACCOUNT.dkr.ecr.$REGION.amazonaws.com/franquicias-api:latest

terraform apply -var "region=$REGION" -var 'image_tag=latest'
```

La URL pública queda en el output `api_base_url`:

```bash
curl "$(terraform output -raw api_base_url)/franquicias"
```

Notas:

- Requiere credenciales AWS válidas (`AWS_PROFILE` o `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY`)
  y los permisos para crear VPC, ALB, ECS, RDS, ECR e IAM.
- La contraseña de MySQL se genera con `random_password` y se guarda en **Secrets Manager**
  (salida `db_secret_arn`); la tarea la recibe por variable de entorno.
- `terraform destroy` elimina todo, incluida la base de datos (hay un snapshot final).
- El esquema lo crea la propia aplicación al arrancar (`SQL_INIT_MODE=always`); para gestionarlo
  con migraciones en producción, poner `SQL_INIT_MODE=never` y adoptar Flyway o Liquibase.

## 10. Configuración por variables de entorno

| Variable | Por defecto | Descripción |
|---|---|---|
| `SERVER_PORT` | `8080` | Puerto HTTP |
| `SPRING_PROFILES_ACTIVE` | — | `mysql` para usar MySQL |
| `MYSQL_HOST` / `MYSQL_PORT` | `localhost` / `3306` | Servidor MySQL (perfil `mysql`) |
| `MYSQL_DATABASE` | `franquicias` | Base de datos |
| `MYSQL_USER` / `MYSQL_PASSWORD` | `franquicias` | Credenciales |
| `R2DBC_URL` | — | URL R2DBC completa (tiene prioridad sobre lo anterior) |
| `R2DBC_USER` / `R2DBC_PASSWORD` | — | Credenciales R2DBC explícitas |
| `R2DBC_POOL_MAX_SIZE` | `20` | Conexiones máximas del pool |
| `SQL_INIT_MODE` | `always` | `never` para no crear el esquema al arrancar |

## 11. Flujo de trabajo con Git

El repositorio es público: <https://github.com/BritneyNi/franquicias-api>

```bash
git clone https://github.com/BritneyNi/franquicias-api.git
git checkout -b mi-feature
# … cambios y pruebas:  mvn clean verify
git add -A && git commit -m "feat: endpoint de products-mayor-stock"
git push origin mi-feature
```

## 12. Licencia

Proyecto de evaluación académica, sin licencia específica.
