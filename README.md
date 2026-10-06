# BanduAirlines · Backend

API de BanduAirlines: Spring Boot (Java, JDK 21, Maven) sobre PostgreSQL, con autenticación JWT e integración de pagos con Flow.

Proyecto semestral de Ingeniería de Software (ICC712), 2° semestre 2026 — Universidad de La Frontera.

BanduAirlines es una plataforma web para buscar, reservar, pagar y gestionar pasajes de una aerolínea nacional de bajo costo.

## Repositorios

| Repositorio | Contenido |
|-------------|-----------|
| [banduairlines-backend](https://github.com/ISOFT-UFRO/banduairlines-backend) | API — Spring Boot (Java 21, Maven), PostgreSQL, JWT, Flow |
| [banduairlines-frontend](https://github.com/ISOFT-UFRO/banduairlines-frontend) | Aplicación web — Vue.js |
| [banduairlines-infra](https://github.com/ISOFT-UFRO/banduairlines-infra) | Infraestructura, CI/CD y despliegue |

La documentación del proyecto (enunciado, modelado, entregas) se mantiene en Google Drive.

## Módulos

| # | Módulo | Nemotécnico | Team |
|---|--------|-------------|------|
| 1 | Integración | INTEGRA | `@ISOFT-UFRO/integra` |
| 2 | Cuentas y Fidelización | CUENTA | `@ISOFT-UFRO/cuenta` |
| 3 | Administración de Vuelos y Oferta Comercial | ADMIN | `@ISOFT-UFRO/admin-vuelos` |
| 4 | Búsqueda y Disponibilidad | BÚSQUEDA | `@ISOFT-UFRO/busqueda` |
| 5 | Compra y Reserva | RESERVA | `@ISOFT-UFRO/reserva` |
| 6 | Pagos y Transacciones | PAGOS | `@ISOFT-UFRO/pagos` |
| 7 | Gestión del Viaje y Check-in | CHECKIN | `@ISOFT-UFRO/checkin` |
| 8 | Reportes y Estadísticas | REPORTES | `@ISOFT-UFRO/reportes` |
| 9 | Tracking | TRACKING | `@ISOFT-UFRO/tracking` |

## Flujo de trabajo

1. Todo trabajo parte de un issue con la etiqueta de su módulo.
2. Se trabaja en una rama propia; no se hace push directo a `main`.
3. Los cambios entran a `main` mediante pull request, revisado por al menos un integrante del módulo.
4. Si un cambio afecta a otro módulo, se abre un issue de tipo *Solicitud entre módulos* y se coordina antes de fusionar.

La estructura del código, las convenciones de ramas y commits y el pipeline de CI/CD los define el módulo **INTEGRA** y quedarán documentados en este repositorio.
