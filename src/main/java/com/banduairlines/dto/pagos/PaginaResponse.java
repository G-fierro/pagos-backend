package com.banduairlines.dto.pagos;

import java.util.List;

public record PaginaResponse<T>(
    List<T> contenido,
    Integer pagina,
    Integer tamano,
    Long totalElementos,
    Integer totalPaginas
) {}
