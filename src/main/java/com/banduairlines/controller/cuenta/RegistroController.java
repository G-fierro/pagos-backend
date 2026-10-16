package com.banduairlines.controller.cuenta;

import com.banduairlines.dto.cuenta.RespuestaUsuario;
import com.banduairlines.dto.cuenta.SolicitudRegistro;
import com.banduairlines.exception.ApiError;
import com.banduairlines.service.cuenta.RegistroPasajeroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Cuentas", description = "Registro y gestión de la cuenta autenticada")
public class RegistroController {

    private final RegistroPasajeroService registroPasajeroService;

    public RegistroController(RegistroPasajeroService registroPasajeroService) {
        this.registroPasajeroService = registroPasajeroService;
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Registrar un pasajero",
            description = "Acceso publico. Crea una cuenta PASAJERO con nivel BASICO."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = @Content(
                    schema = @Schema(implementation = SolicitudRegistro.class),
                    examples = @ExampleObject(value = """
                            {"nombres":"Valentina","apellidos":"Rojas","correo":"valentina@example.com","contrasena":"Clave-2026","confirmacionContrasena":"Clave-2026"}
                            """)
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Pasajero creado",
                    content = @Content(
                            schema = @Schema(implementation = RespuestaUsuario.class),
                            examples = @ExampleObject(value = """
                                    {"idUsuario":1,"nombres":"Valentina","apellidos":"Rojas","correo":"valentina@example.com","rol":"PASAJERO","nivel":"BASICO"}
                                    """)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "VALIDACION", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "CORREO_YA_REGISTRADO", content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public RespuestaUsuario registrar(@Valid @RequestBody SolicitudRegistro solicitud) {
        return registroPasajeroService.registrar(solicitud);
    }
}
