package com.banduairlines.service.impl;

import com.banduairlines.exception.CodigosError;
import com.banduairlines.exception.ReglaNegocioException;
import com.banduairlines.exception.ValidacionException;
import com.banduairlines.model.ConfiguracionNivel;
import com.banduairlines.model.enums.Nivel;
import com.banduairlines.service.cuenta.GestionNivelService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class GestionNivelServiceImpl implements GestionNivelService {

    private final Long millasMinimasPlata;
    private final Long millasMinimasOro;

    public GestionNivelServiceImpl(
            @Value("${app.fidelizacion.nivel.plata.millas-minimas:}") String plata,
            @Value("${app.fidelizacion.nivel.oro.millas-minimas:}") String oro
    ) {
        this.millasMinimasPlata = convertirUmbral(plata);
        this.millasMinimasOro = convertirUmbral(oro);
    }

    @Override
    public Nivel determinarNivel(long millas) {
        if (millas < 0) {
            throw new ValidacionException("La cantidad de millas no puede ser negativa");
        }
        validarConfiguracion();

        if (millas >= millasMinimasOro) {
            return Nivel.ORO;
        }
        if (millas >= millasMinimasPlata) {
            return Nivel.PLATA;
        }
        return Nivel.BASICO;
    }

    @Override
    public ConfiguracionNivel obtenerConfiguracion(Nivel nivel) {
        if (nivel == null) {
            throw new ValidacionException("El nivel es obligatorio");
        }
        validarConfiguracion();

        return switch (nivel) {
            case BASICO -> new ConfiguracionNivel(Nivel.BASICO, 0L, BigDecimal.ONE);
            case PLATA -> new ConfiguracionNivel(
                    Nivel.PLATA,
                    millasMinimasPlata,
                    new BigDecimal("1.2")
            );
            case ORO -> new ConfiguracionNivel(
                    Nivel.ORO,
                    millasMinimasOro,
                    new BigDecimal("1.5")
            );
            case PLATINO -> throw new ValidacionException(
                    "PLATINO no forma parte de los niveles gestionados por Cuentas"
            );
        };
    }

    private void validarConfiguracion() {
        if (millasMinimasPlata == null
                || millasMinimasOro == null
                || millasMinimasPlata <= 0
                || millasMinimasOro <= millasMinimasPlata) {
            throw new ReglaNegocioException(
                    "Los umbrales de PLATA y ORO deben configurarse y ser crecientes"
            ).conCodigo(CodigosError.CONFIGURACION_NIVELES_INCOMPLETA);
        }
    }

    private static Long convertirUmbral(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(valor.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
