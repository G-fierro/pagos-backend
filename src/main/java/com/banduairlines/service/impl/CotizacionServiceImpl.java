package com.banduairlines.service.impl;

import com.banduairlines.dto.busqueda.DetalleCotizacionResponse;
import com.banduairlines.dto.busqueda.SeleccionCotizacionRequest;
import com.banduairlines.service.busqueda.CotizacionService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class CotizacionServiceImpl implements CotizacionService {

    // --- Inicialización ---

    public CotizacionServiceImpl() {
    }

    // --- Lógica de Negocio ---

    @Override
    public DetalleCotizacionResponse calcularCotizacion(SeleccionCotizacionRequest request) {
        DetalleCotizacionResponse response = new DetalleCotizacionResponse();

        response.setVueloId(request.getVueloId());
        response.setTarifaId(request.getTarifaId());
        response.setClaseCabina(request.getClaseCabina());

        response.setSubtotalAdultos(BigDecimal.ZERO);
        response.setSubtotalNinos(BigDecimal.ZERO);
        response.setSubtotalBebes(BigDecimal.ZERO);
        response.setImpuestos(BigDecimal.ZERO);
        response.setPrecioTotal(BigDecimal.ZERO);

        return response;
    }
}