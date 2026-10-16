package com.banduairlines.service.busqueda;

import com.banduairlines.dto.busqueda.FiltroBusquedaRequest;

import java.util.List;

public interface FiltroBusquedaService {

    // --- Operaciones de Filtrado y Ordenamiento ---

    List<Object> filtrarYOrdenar(FiltroBusquedaRequest request);
}