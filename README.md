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

| Módulo | Carpeta (`controller/`, `dto/`) | Scope de commit |
|---|---|---|
| Integración | `integra` | `integracion`, `core` |
| Cuentas y Fidelización | `cuenta` | `cuentas` |
| Administración | `admin` | `administracion` |
| Búsqueda y Disponibilidad | `busqueda` | `busqueda` |
| Compra y Reserva | `reserva` | `reservas` |
| Pagos y Transacciones | `pagos` | `pagos` |
| Gestión del Viaje (Check-in) | `checkin` | `checkin` |
| Estadísticas (Reportes) | `reportes` | `reportes` |
| Seguimiento Operativo (Tracking) | `tracking` | `tracking` |

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

## 9. Arquitectura de Módulos y Entidades (Modelos Spring Boot)

El desarrollo del backend está organizado en 9 módulos lógicos. A continuación se detalla la responsabilidad de cada uno y las entidades (`@Entity`) que soportan su persistencia en la base de datos:

**Módulo 1: INTEGRA (Integración):** Base del proyecto. Coordina convenciones de código, configuraciones globales (incluido CORS para desarrollo local en `CorsConfig`; en producción no hace falta porque frontend y API comparten dominio), CI/CD, esquemas de seguridad base y despliegue.

**Módulo 2: CUENTA (Cuentas y Fidelización):** Maneja la seguridad JWT, la gestión de perfiles y el programa de lealtad. *Modelos base:*
* **Usuario:** Credenciales, roles y datos personales.
* **CuentaFidelizacion:** Extensión del usuario (`@MapsId`) para controlar saldo y nivel (Básico, Plata, Oro, Platino).
* **MovimientoMilla:** Bitácora que registra acumulaciones por vuelos volados, canjes o expiración de millas.

**Módulo 3: ADMIN (Administración):** Backoffice para la definición estática de la aerolínea (física y comercial). *Modelos base:*
* **Infraestructura:** `Aeropuerto`, `Avion` (aeronave), `Asiento` (mapeo del fuselaje) y `Ruta` (conexiones origen-destino).
* **Comercial:** `PackTarifario` (reglas de tarifas) y `Extra` (catálogo de servicios adicionales).

**Módulo 4: BUSQUEDA (Búsqueda y Disponibilidad):** Motor principal que cruza rutas, fechas y asientos disponibles. *Modelos base:*
* **Vuelo:** Instancia de una `Ruta` operada por un `Avion` en un horario específico.
* **TarifaVuelo:** Tabla pivote que cruza un `Vuelo` específico con un `PackTarifario` y una `ClaseCabina`, estableciendo el precio dinámico.

**Módulo 5: RESERVA (Compra y Reserva):** Gestión del carrito de compras, asignación de pasajeros a tramos y cálculo total. *Modelos base:*
* **Reserva:** Entidad cabecera (código alfanumérico único de 6 caracteres) que consolida subtotales, descuentos y el estado del carrito.
* **Pasajero:** Detalles legales y etarios de cada viajero asociado a la reserva.
* **Boleto:** Asocia un `pasajero` a un tramo (`Vuelo`), asignando su `tarifa` y, opcionalmente, su asiento.
* **BoletoExtra:** Entidad intermedia (con clave compuesta `@EmbeddedId`) que registra los servicios adicionales comprados (ej. 2 maletas extra).
* **Promocion:** Códigos de descuento validados durante el proceso.

**Módulo 6: PAGOS (Pagos y Transacciones):** Integración exclusiva con *Flow.cl*. Recibe el requerimiento de pago desde el módulo `RESERVA`. *Modelos base:*
* **Pago:** Bitácora transaccional que almacena el token de *Flow*, la orden, el medio de pago utilizado y guarda el payload crudo (JSON) de la respuesta del banco para auditoría.

**Módulo 7: CHECKIN (Gestión del Viaje):** Permite buscar reservas confirmadas, validar restricciones de tiempo (48h a 1h) y emitir la documentación de vuelo. *Modelos base:*
* **TarjetaEmbarque:** Relación estricta 1 a 1 (`@MapsId`) con un `Boleto`. Genera el código QR/alfanumérico final y registra la puerta de embarque.

**Módulo 8: REPORTES (Estadísticas):** Panel de métricas del negocio (ocupación de vuelos, ingresos por rutas, ventas de extras). *Modelos base:*
* **Módulo de lectura (Read-Only):** Realiza consultas complejas (proyecciones y agregaciones DTO) sobre Reservas, Pagos, Vuelos y Boletos.
* **Implementación:** Utiliza los repositorios de `Reserva`, `Pago`, `Vuelo` y `Boleto` inyectados en su capa de servicio. Ejecuta consultas JPQL o nativas (`GROUP BY`, `SUM`, `COUNT`) utilizando las marcas de tiempo existentes (`created_at`, `fecha_pago`, `salida_programada`) para generar agrupaciones temporales (diarias, semanales, mensuales, anuales). Los resultados se mapean directamente a objetos **DTO** para ser consumidos por el frontend.

**Módulo 9: TRACKING (Seguimiento Operativo):** Monitoreo de la operación de la aerolínea. Los pasajeros consultan el estado de sus vuelos en `/api/v1/tracking/**` (requiere autenticación) y los administradores actualizan los estados (*Embarcando*, *En Vuelo*, *Retrasado*) en `/api/v1/admin/tracking/**`. *Modelos base:*
* **SeguimientoVuelo:** Historial inmutable que registra qué administrador cambió el estado de un vuelo, la marca de tiempo y las observaciones (ej. "Retraso por clima").