# BanduAirlines · Backend

BanduAirlines es una plataforma web para buscar, reservar, pagar y gestionar pasajes de una aerolínea nacional de bajo costo, desarrollada como proyecto semestral de Ingeniería de Software (ICC712), 2° semestre 2026, en la Universidad de La Frontera. Este repositorio contiene el backend: Spring Boot (Java, JDK 21, Maven) sobre PostgreSQL, con autenticación JWT e integración de pagos con Flow.

## 1. Puesta en Marcha Local
1. Copiar entorno: `cp .env.example .env` (ajustar variables si es necesario).
2. Levantar base de datos: `docker compose up -d db`
3. Ejecutar backend: `./mvnw spring-boot:run` o `mvn spring-boot:run`

## Datos de prueba (temporal)
Activar creando `config/application.properties` en la raíz con `app.seed.enabled=true` (o variable `APP_SEED_ENABLED=true`).
Solo para desarrollo local: no se incluye ni ejecuta en Docker ni en entornos de servidor.
Se elimina cuando lleguen los primeros módulos a `develop`, y siempre antes de promover a `main` (instrucciones detalladas en `src/main/java/com/banduairlines/devdata/LEEME.md`).

## 2. Convenciones de Desarrollo
* **Ramas:** Crear ramas `feature/<nombre>` desde `develop` y abrir Pull Request hacia `develop`.
* **Commits convencionales:** Formato obligatorio `<tipo>(<scope>): <descripción>` (`commitlint.config.js`).
  * Tipos: `feat`, `fix`, `docs`, `chore`, `refactor`, `test`.
  * Scopes: `checkin`, `reservas`, `pagos`, `tracking`, `administracion`, `reportes`, `cuentas`, `busqueda`, `integracion`, `core`.
* **Estructura:** Código en `com.banduairlines`. Controladores y DTOs van en subcarpetas por módulo (`controller/<modulo>/`, `dto/<modulo>/`; módulos: admin, busqueda, checkin, cuenta, integra, pagos, reportes, reserva, tracking); `service/`, `service/impl/` y `repository/` sin subcarpetas. Ejemplo de referencia en `controller/integra/AeropuertoController`.

## 3. Seguridad y Rutas
* **Solicitar ruta pública:** Enviar un Pull Request modificando `SecurityRoutes.java` (`PUBLICAS` o `PUBLICAS_GET`).
* **Tracking:** Consulta de pasajero en `/api/v1/tracking/**` (requiere autenticación JWT, no es pública). Gestión en `/api/v1/admin/tracking/**` (solo `ADMINISTRADOR`).
* **Documentación (Swagger):** Rutas públicas disponibles en `/swagger-ui/index.html` y `/v3/api-docs`.
* **Method Security:** `@PreAuthorize` solo se evalúa con `app.security.enabled=true`.
* **Salud del servicio:** `GET /api/v1/health` (usar para verificar despliegue).

## 4. Manejo de Errores
### Las 3 Reglas de Errores
1. **Los servicios lanzan excepciones comunes:** Nunca construir respuestas de error a mano en los controladores ni servicios.
2. **Ningún módulo crea su propio manejador:** Todas las respuestas de error las gestiona centralizadamente `GlobalExceptionHandler`.
3. **No silenciar excepciones:** No usar `catch` vacíos ni atrapar errores para ocultarlos; deben escalar o relanzarse.

### Formato de Error
Todas las respuestas de error siguen esta estructura JSON:
```json
{
  "timestamp": "2026-10-11T02:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "code": "VALIDACION",
  "message": "Los datos enviados no son válidos",
  "path": "/api/v1/ejemplo/aeropuertos",
  "details": [
    { "campo": "codigoIata", "mensaje": "el código IATA es obligatorio" }
  ]
}
```

### Tabla de Códigos por Defecto
| HTTP | Código (`code`) | Excepción / Causa |
|---|---|---|
| 400 | `VALIDACION` | `@Valid`, `ConstraintViolationException` (detalles en `details`) |
| 400 | `PETICION_MALFORMADA` | `HttpMessageNotReadableException`, tipo de dato incorrecto |
| 401 | `NO_AUTENTICADO` | `AuthenticationException`, token ausente o inválido |
| 403 | `ACCESO_DENEGADO` | `AccessDeniedException`, rol o permisos insuficientes |
| 404 | `RECURSO_NO_ENCONTRADO` | `RecursoNoEncontradoException` |
| 409 | `CONFLICTO` | `ConflictoException` |
| 409 | `RESTRICCION_DE_DATOS` | `DataIntegrityViolationException` |
| 409 | `CONFLICTO_CONCURRENCIA` | `OptimisticLockingFailureException` |
| 422 | `REGLA_NEGOCIO` | `ReglaNegocioException` |
| 500 | `ERROR_INTERNO` | Error no controlado |
| 502 | `SERVICIO_EXTERNO` | `ServicioExternoException` (falla con Flow, etc.) |

* **Regla de códigos propios:** Los módulos pueden personalizar el código lanzando excepciones comunes con `.conCodigo("CODIGO_ESPECIFICO")` (siempre en `MAYUSCULAS_CON_GUION_BAJO`), sin crear manejadores de excepción propios.

## 5. Reparto de Responsabilidades
| Área | Responsabilidad |
|---|---|
| **Integración** | Configuración de seguridad general (`SecurityConfig`), filtro global, rutas públicas/protegidas, formato 401/403. |
| **Cuentas** | Registro, login, emisión/validación de tokens JWT, gestión de usuarios y roles. |
| **Cada Módulo** | Reglas finas de negocio y autorización (ej. un pasajero solo accede a sus reservas). |
| **Integración + Cuentas** | Contrato del JWT y helper para obtener el usuario autenticado (propuesta en sección 7). |

## 6. Pendientes de Decisión del Equipo
1. **Revisión de Cuentas del contrato JWT (sección 7)**
2. **Activar `SECURITY_ENABLED=true` en producción cuando Cuentas entregue el filtro JWT**

## 7. Autenticación (propuesta de Integración, pendiente de revisión de Cuentas)
* **Header:** `Authorization: Bearer <token>` en cada petición protegida.
* **Claims del JWT:**
  * `sub`: ID numérico del usuario (`Long`).
  * `email`: correo electrónico del usuario (`String`).
  * `rol`: rol del usuario (`PASAJERO` | `ADMINISTRADOR`).
  * `iat` y `exp`: emisión y expiración (según `app.jwt.expiration-ms`).
* **Principal en SecurityContext:** Instancia de `UsuarioAutenticado(id, email, rol)`.
* **Uso desde un servicio:**
```java
@Autowired private UsuarioActual usuarioActual;
Long usuarioId = usuarioActual.exigirId(); // lanza 401 si no hay usuario autenticado
UsuarioAutenticado usuario = usuarioActual.exigir();
```
* **Responsabilidad de Cuentas:** La emisión de tokens en `/api/v1/auth/**`, la validación en `JwtAuthenticationFilter` y los endpoints de autenticación corresponden al módulo Cuentas.

## 8. Primer Administrador
* **Variables de entorno:** `ADMIN_EMAIL` y `ADMIN_PASSWORD` en el `.env` del servidor.
* **Comportamiento:** Se crea automáticamente al arrancar la aplicación si el usuario no existe con rol `ADMINISTRADOR` (nunca sobrescribe credenciales de usuarios existentes ni registra contraseñas en los logs).
* **Despliegue:** El Compose de producción (`banduairlines-infra`) debe pasar estas variables de entorno al contenedor backend.

