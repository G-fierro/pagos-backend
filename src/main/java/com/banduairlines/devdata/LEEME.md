# Datos de Prueba Locales (devdata)

Este paquete (`DataSeeder.java`) carga datos iniciales para desarrollo local:
- 6 aeropuertos de Chile (SCL, ANF, CJC, CCP, PMC, PUQ).
- 12 rutas en ambos sentidos.
- 2 aeronaves (Airbus A320neo y A321neo) con cabinas y asientos configurados.
- Packs tarifarios (Light, Plus, Top).
- Vuelos para los próximos 14 días con sus respectivas tarifas.

## Activación (Solo Local)
El seeder está inactivo por defecto y **NO corre en Docker ni en servidor**.
Para activarlo en local, usa una de estas opciones:
1. Crear `config/application.properties` en la raíz del repo con: `app.seed.enabled=true`
2. Exportar la variable de entorno: `APP_SEED_ENABLED=true`

## Cómo Borrarlo (Cuando lleguen los primeros módulos a develop y antes de promover a main)
Se debe eliminar cuando lleguen los primeros módulos a `develop`, y siempre antes de promover a `main`:
1. Eliminar la carpeta: `git rm -r src/main/java/com/banduairlines/devdata`
2. Quitar del `README.md` el bloque "Datos de prueba (temporal)".
3. Crear el commit: `git commit -m "chore(core): elimina datos de prueba locales"`
