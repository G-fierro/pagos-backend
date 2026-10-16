package com.banduairlines.security;

public final class SecurityRoutes {

    private SecurityRoutes() {
    }

    public static final String API = "/api/v1";

    /** Públicas para cualquier método HTTP. */
    public static final String[] PUBLICAS = {
            API + "/health",                  // INTEGRA: endpoint de salud
            "/actuator/health",               // INTEGRA: healthcheck del despliegue (Actuator)
            "/actuator/health/**",
            API + "/auth/**",                 // CUENTA: login y registro
            API + "/pagos/notificaciones",    // PAGOS: callback servidor-a-servidor de Flow (no trae token)
            API + "/reservas",                // RESERVA: POST inicia la compra; sin JWT es de invitado y recibe compraToken
            API + "/reservas/*/pasajeros",    // RESERVA: endpoints 3, 4 y 6 (invitado con X-Compra-Token)
            API + "/reservas/*/contacto",
            API + "/reservas/*/asientos",
            API + "/reservas/*/servicios",    // RESERVA: el invitado entra con X-Compra-Token; ReservaAcceso exige token o JWT del dueño
            API + "/reservas/*/promocion",
            API + "/reservas/*/pago",
            API + "/reservas/*/cancelacion"
    };

    /** Públicas solo para GET. */
    public static final String[] PUBLICAS_GET = {
            API + "/busqueda/**",             // BÚSQUEDA: el visitante busca vuelos sin cuenta (ruta propuesta)
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            API + "/reservas/*",              // RESERVA: endpoint 2 (invitado con X-Compra-Token); ReservaAcceso exige token o JWT
            API + "/reservas/*/tramos/*/asientos" // RESERVA: endpoint 5
            // PENDIENTE: rutas de invitado de RESERVA, PAGOS y CHECKIN. Cada equipo las propone por PR (D-03).
    };

    /** Solo rol ADMINISTRADOR. */
    public static final String[] SOLO_ADMIN = {
            API + "/admin/**"                 // ADMIN y REPORTES bajo /admin/**; gestión en /admin/tracking/** (Tracking de pasajero en API + "/tracking/**", requiere autenticación, NO pública)
    };
}
