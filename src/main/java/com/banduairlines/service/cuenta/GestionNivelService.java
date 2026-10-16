package com.banduairlines.service.cuenta;

import com.banduairlines.model.ConfiguracionNivel;
import com.banduairlines.model.enums.Nivel;

public interface GestionNivelService {

    Nivel determinarNivel(long millas);

    ConfiguracionNivel obtenerConfiguracion(Nivel nivel);
}
