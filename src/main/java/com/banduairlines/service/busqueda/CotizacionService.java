package com.banduairlines.service.busqueda;

import com.banduairlines.dto.busqueda.DetalleCotizacionResponse;
import com.banduairlines.dto.busqueda.SeleccionCotizacionRequest;

public interface CotizacionService {

    // --- Operaciones de Cotización ---

    DetalleCotizacionResponse calcularCotizacion(SeleccionCotizacionRequest request);
}