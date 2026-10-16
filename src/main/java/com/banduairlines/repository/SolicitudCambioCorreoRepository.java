package com.banduairlines.repository;

import com.banduairlines.model.SolicitudCambioCorreo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface SolicitudCambioCorreoRepository
        extends JpaRepository<SolicitudCambioCorreo, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SolicitudCambioCorreo> findByIdSolicitudAndUsuario_Id(
            Long idSolicitud,
            Long idUsuario
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<SolicitudCambioCorreo>
    findByUsuario_IdAndFechaConfirmacionIsNullAndFechaCancelacionIsNull(
            Long idUsuario
    );
}
