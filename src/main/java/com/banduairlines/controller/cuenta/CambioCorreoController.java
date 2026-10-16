package com.banduairlines.controller.cuenta;

import com.banduairlines.dto.cuenta.ConfirmacionCambioCorreo;
import com.banduairlines.dto.cuenta.RespuestaSolicitudCambioCorreo;
import com.banduairlines.dto.cuenta.RespuestaUsuario;
import com.banduairlines.dto.cuenta.SolicitudCambioCorreo;
import com.banduairlines.exception.ApiError;
import com.banduairlines.service.cuenta.CambioCorreoService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cuentas/actual/cambio-correo/solicitudes")
@PreAuthorize("hasAnyRole('PASAJERO', 'ADMINISTRADOR')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Cuentas", description = "Registro y gestión de la cuenta autenticada")
public class CambioCorreoController {

    private final CambioCorreoService cambioCorreoService;

    public CambioCorreoController(CambioCorreoService cambioCorreoService) {
        this.cambioCorreoService = cambioCorreoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(
            summary = "Solicitar cambio de correo",
            description = "Acceso PASAJERO o ADMINISTRADOR. Envia un codigo de seis digitos al nuevo correo sin modificar aun la cuenta."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    schema = @Schema(implementation = SolicitudCambioCorreo.class),
                    examples = @ExampleObject(value = "{\"nuevoCorreo\":\"correo.nuevo@example.com\"}")
            )
    )
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "Solicitud creada", content = @Content(schema = @Schema(implementation = RespuestaSolicitudCambioCorreo.class), examples = @ExampleObject(value = "{\"idSolicitud\":12,\"correoEnmascarado\":\"c*****@example.com\",\"fechaExpiracion\":\"2026-10-15T20:30:00Z\"}"))),
            @ApiResponse(responseCode = "400", description = "VALIDACION", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "NO_AUTENTICADO", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "ACCESO_DENEGADO", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "CORREO_YA_REGISTRADO", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public RespuestaSolicitudCambioCorreo solicitar(
            @Valid @RequestBody SolicitudCambioCorreo solicitud
    ) {
        return cambioCorreoService.solicitar(solicitud);
    }

    @PutMapping("/{idSolicitud}")
    @Operation(
            summary = "Confirmar cambio de correo",
            description = "Acceso PASAJERO o ADMINISTRADOR. Verifica el codigo y cambia el correo dentro de una transaccion."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    schema = @Schema(implementation = ConfirmacionCambioCorreo.class),
                    examples = @ExampleObject(value = "{\"codigo\":\"482916\"}")
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Correo actualizado",
                    content = @Content(
                            schema = @Schema(implementation = RespuestaUsuario.class),
                            examples = @ExampleObject(value = """
                                    {"idUsuario":1,"nombres":"Valentina","apellidos":"Rojas","correo":"correo.nuevo@example.com","rol":"PASAJERO","nivel":"BASICO"}
                                    """)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "VALIDACION", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "NO_AUTENTICADO", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "ACCESO_DENEGADO", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "SOLICITUD_NO_ENCONTRADA", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "CORREO_YA_REGISTRADO", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "410", description = "CODIGO_VENCIDO", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "422", description = "CODIGO_INCORRECTO", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public RespuestaUsuario confirmar(
            @PathVariable Long idSolicitud,
            @Valid @RequestBody ConfirmacionCambioCorreo confirmacion
    ) {
        return cambioCorreoService.confirmar(idSolicitud, confirmacion);
    }

    @DeleteMapping("/{idSolicitud}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Cancelar cambio de correo",
            description = "Acceso PASAJERO o ADMINISTRADOR. Cancela la solicitud sin modificar el correo actual."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Solicitud cancelada"),
            @ApiResponse(responseCode = "401", description = "NO_AUTENTICADO", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "ACCESO_DENEGADO", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "SOLICITUD_NO_ENCONTRADA", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public void cancelar(@PathVariable Long idSolicitud) {
        cambioCorreoService.cancelar(idSolicitud);
    }
}
