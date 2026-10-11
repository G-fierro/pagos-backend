package com.example.demo.security;

public final class SecurityRoutes {

    private SecurityRoutes() {
    }

    public static final String API = "/api/v1";

    /** Públicas para cualquier método HTTP. */
    public static final String[] PUBLICAS = {
            API + "/ping/**",                 // diagnóstico de INTEGRA (eliminar antes de producción)
            API + "/auth/**",                 // CUENTA: login y registro
            API + "/pagos/notificaciones"     // PAGOS: callback servidor-a-servidor de Flow (no trae token)
    };

    /** Públicas solo para GET. */
    public static final String[] PUBLICAS_GET = {
            API + "/busqueda/**"              // BÚSQUEDA: el visitante busca vuelos sin cuenta (ruta propuesta)
            // PENDIENTE: rutas de invitado de RESERVA, PAGOS y CHECKIN. Cada equipo las propone por PR (D-03).
    };

    /** Solo rol ADMINISTRADOR. */
    public static final String[] SOLO_ADMIN = {
            API + "/admin/**"                 // ADMIN, REPORTES y TRAKING viven bajo /admin
    };
}
