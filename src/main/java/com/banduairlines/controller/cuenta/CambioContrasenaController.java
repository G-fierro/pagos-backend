package com.banduairlines.controller.cuenta;

import com.banduairlines.dto.cuenta.SolicitudCambioContrasena;
import com.banduairlines.exception.ApiError;
import com.banduairlines.service.cuenta.CambioContrasenaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cuentas/actual")
@PreAuthorize("hasAnyRole('PASAJERO', 'ADMINISTRADOR')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Cuentas", description = "Registro y gestión de la cuenta autenticada")
public class CambioContrasenaController {

    private final CambioContrasenaService cambioContrasenaService;

    public CambioContrasenaController(CambioContrasenaService cambioContrasenaService) {
        this.cambioContrasenaService = cambioContrasenaService;
    }

    @PutMapping("/contrasena")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Cambiar contraseña",
            description = "Acceso PASAJERO o ADMINISTRADOR. Valida la contraseña actual y almacena un nuevo resumen BCrypt."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    schema = @Schema(implementation = SolicitudCambioContrasena.class),
                    examples = @ExampleObject(value = "{\"contrasenaActual\":\"Clave-2025\",\"nuevaContrasena\":\"Clave-2026\",\"confirmacionContrasena\":\"Clave-2026\"}")
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Contraseña actualizada"),
            @ApiResponse(responseCode = "400", description = "VALIDACION", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "NO_AUTENTICADO", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "ACCESO_DENEGADO", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "422", description = "CONTRASENA_ACTUAL_INCORRECTA", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public void cambiar(@Valid @RequestBody SolicitudCambioContrasena solicitud) {
        cambioContrasenaService.cambiar(solicitud);
    }
}
