package com.banduairlines.service.impl;

import com.banduairlines.dto.busqueda.FiltroBusquedaRequest;
import com.banduairlines.service.busqueda.FiltroBusquedaService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FiltroBusquedaServiceImpl implements FiltroBusquedaService {

    // --- Inicialización y Dependencias ---

    public FiltroBusquedaServiceImpl() {
    }

    // --- Lógica de Negocio ---

    @Override
    public List<Object> filtrarYOrdenar(FiltroBusquedaRequest request) {
        List<Object> resultadosFiltrados = obtenerResultadosBase();

        return aplicarFiltrosYOrden(resultadosFiltrados, request);
    }

    // --- Métodos Auxiliares ---

    private List<Object> obtenerResultadosBase() {
        return new ArrayList<>();
    }

    private List<Object> aplicarFiltrosYOrden(List<Object> vuelos, FiltroBusquedaRequest request) {
        return vuelos;
    }
}