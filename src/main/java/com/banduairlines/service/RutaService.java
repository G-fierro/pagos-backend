package com.banduairlines.service;

import com.banduairlines.model.Aeropuerto;
import com.banduairlines.model.Ruta;
import com.banduairlines.repository.AeropuertoRepository;
import com.banduairlines.repository.RutaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RutaService {

    private final RutaRepository rutaRepository;
    private final AeropuertoRepository aeropuertoRepository;

    @Transactional(readOnly = true)
    public List<Ruta> listarRutas() {
        return rutaRepository.findAll();
    }

    @Transactional
    public Ruta crearRuta(Long origenId, Long destinoId, Integer distanciaKm, Integer duracionMin) {
        Aeropuerto origen = aeropuertoRepository.findById(origenId)
                .orElseThrow(() -> new RuntimeException("Aeropuerto de origen no encontrado"));

        Aeropuerto destino = aeropuertoRepository.findById(destinoId)
                .orElseThrow(() -> new RuntimeException("Aeropuerto de destino no encontrado"));

        if (origenId.equals(destinoId)) {
            throw new RuntimeException("El origen y el destino no pueden ser el mismo aeropuerto");
        }

        Ruta ruta = Ruta.builder()
                .origen(origen)
                .destino(destino)
                .distanciaKm(distanciaKm)
                .duracionMin(duracionMin)
                .activa(true)
                .build();

        return rutaRepository.save(ruta);
    }

    @Transactional
    public Ruta cambiarEstadoRuta(Long id, Boolean activa) {
        Ruta ruta = rutaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ruta no encontrada"));
        ruta.setActiva(activa);
        return rutaRepository.save(ruta);
    }
}
