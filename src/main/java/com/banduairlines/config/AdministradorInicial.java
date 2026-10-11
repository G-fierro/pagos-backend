package com.banduairlines.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.banduairlines.model.Usuario;
import com.banduairlines.model.enums.RolUsuario;
import com.banduairlines.repository.UsuarioRepository;

@Component
public class AdministradorInicial implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdministradorInicial.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public AdministradorInicial(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.email:}") String adminEmail,
            @Value("${app.admin.password:}") String adminPassword) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail != null ? adminEmail.trim() : "";
        this.adminPassword = adminPassword != null ? adminPassword : "";
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        boolean emailVacio = adminEmail.isBlank();
        boolean passwordVacio = adminPassword.isBlank();

        if (emailVacio && passwordVacio) {
            log.info("Inicialización de administrador omitida: app.admin.email y app.admin.password no configurados.");
            return;
        }

        if (emailVacio || passwordVacio) {
            log.warn("Inicialización de administrador omitida: se requiere configurar tanto email como password.");
            return;
        }

        String emailNormalizado = adminEmail.toLowerCase();

        if (adminPassword.length() < 12) {
            log.error("Inicialización de administrador rechazada: la contraseña configurada debe tener al menos 12 caracteres.");
            return;
        }

        if (usuarioRepository.existsByEmail(emailNormalizado)) {
            log.info("El administrador con email '{}' ya existe. No se realizaron cambios.", emailNormalizado);
            return;
        }

        Usuario admin = Usuario.builder()
                .email(emailNormalizado)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .rol(RolUsuario.ADMINISTRADOR)
                .nombre("Administrador")
                .apellido("Sistema")
                .activo(true)
                .build();

        usuarioRepository.save(admin);
        log.info("Administrador inicial creado exitosamente con email '{}'.", emailNormalizado);
    }
}
