package com.banduairlines.exception;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ---------- Excepciones propias del proyecto ----------

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ApiError> recursoNoEncontrado(RecursoNoEncontradoException e, HttpServletRequest req) {
        return responder(HttpStatus.NOT_FOUND, e.getCodigo(), e.getMessage(), req, List.of());
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ApiError> reglaNegocio(ReglaNegocioException e, HttpServletRequest req) {
        return responder(HttpStatus.UNPROCESSABLE_CONTENT, e.getCodigo(), e.getMessage(), req, List.of());
    }

    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<ApiError> conflicto(ConflictoException e, HttpServletRequest req) {
        return responder(HttpStatus.CONFLICT, e.getCodigo(), e.getMessage(), req, List.of());
    }

    @ExceptionHandler(ServicioExternoException.class)
    public ResponseEntity<ApiError> servicioExterno(ServicioExternoException e, HttpServletRequest req) {
        log.warn("Falla de servicio externo en {} {}: {}", req.getMethod(), req.getRequestURI(), e.getMessage(), e);
        return responder(HttpStatus.BAD_GATEWAY, e.getCodigo(), "Falla temporal al comunicarse con un servicio externo", req, List.of());
    }

    // ---------- Validación y peticiones mal formadas (400) ----------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validacion(MethodArgumentNotValidException e, HttpServletRequest req) {
        List<DetalleError> detalles = new ArrayList<>();
        e.getBindingResult().getFieldErrors()
                .forEach(f -> detalles.add(new DetalleError(f.getField(), f.getDefaultMessage())));
        e.getBindingResult().getGlobalErrors()
                .forEach(g -> detalles.add(new DetalleError(g.getObjectName(), g.getDefaultMessage())));
        return responder(HttpStatus.BAD_REQUEST, CodigosError.VALIDACION, "Los datos enviados no son válidos", req, detalles);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> restricciones(ConstraintViolationException e, HttpServletRequest req) {
        List<DetalleError> detalles = e.getConstraintViolations().stream()
                .map(v -> new DetalleError(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return responder(HttpStatus.BAD_REQUEST, CodigosError.VALIDACION, "Los datos enviados no son válidos", req, detalles);
    }

    @ExceptionHandler({ HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class })
    public ResponseEntity<ApiError> peticionMalFormada(Exception e, HttpServletRequest req) {
        return responder(HttpStatus.BAD_REQUEST, CodigosError.PETICION_MALFORMADA,
                "La petición está mal formada o contiene un tipo de dato incorrecto", req, List.of());
    }

    // ---------- Seguridad lanzada desde controladores/servicios ----------

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> noAutenticado(AuthenticationException e, HttpServletRequest req) {
        return responder(HttpStatus.UNAUTHORIZED, CodigosError.NO_AUTENTICADO, "Credenciales inválidas o autenticación requerida", req, List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> accesoDenegado(AccessDeniedException e, HttpServletRequest req) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean anonimo = auth == null || auth instanceof AnonymousAuthenticationToken;
        return anonimo
                ? responder(HttpStatus.UNAUTHORIZED, CodigosError.NO_AUTENTICADO, "Autenticación requerida", req, List.of())
                : responder(HttpStatus.FORBIDDEN, CodigosError.ACCESO_DENEGADO, "No tienes permisos para esta acción", req, List.of());
    }

    // ---------- Base de datos ----------

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> integridad(DataIntegrityViolationException e, HttpServletRequest req) {
        log.warn("Violación de integridad en {} {}: {}", req.getMethod(), req.getRequestURI(), e.getMostSpecificCause().getMessage());
        return responder(HttpStatus.CONFLICT, CodigosError.RESTRICCION_DE_DATOS,
                "La operación viola una restricción de datos (registro duplicado o en uso)", req, List.of());
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> bloqueoOptimista(OptimisticLockingFailureException e, HttpServletRequest req) {
        return responder(HttpStatus.CONFLICT, CodigosError.CONFLICTO_CONCURRENCIA,
                "El recurso fue modificado por otra operación; vuelve a intentarlo", req, List.of());
    }

    // ---------- Errores estándar de Spring MVC + red de seguridad final ----------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> general(Exception e, HttpServletRequest req) {
        // 404 de ruta inexistente, 405, 415, ResponseStatusException, etc.: conservan su código original.
        if (e instanceof ErrorResponse er) {
            String detalle = er.getBody().getDetail();
            String codigoHttp = "ERROR_HTTP_" + er.getStatusCode().value();
            return responder(er.getStatusCode(), codigoHttp, detalle != null ? detalle : "Petición no válida", req, List.of());
        }
        log.error("Error no controlado en {} {}", req.getMethod(), req.getRequestURI(), e);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, CodigosError.ERROR_INTERNO, "Error interno del servidor", req, List.of());
    }

    // ---------- Utilitario ----------

    private ResponseEntity<ApiError> responder(HttpStatusCode status, String codigo, String mensaje,
                                               HttpServletRequest req, List<DetalleError> detalles) {
        HttpStatus http = HttpStatus.resolve(status.value());
        ApiError cuerpo = new ApiError(
                Instant.now(),
                status.value(),
                http != null ? http.getReasonPhrase() : "Error",
                codigo,
                mensaje,
                req.getRequestURI(),
                detalles);
        return ResponseEntity.status(status).body(cuerpo);
    }
}
