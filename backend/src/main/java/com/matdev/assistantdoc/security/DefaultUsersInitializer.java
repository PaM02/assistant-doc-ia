package com.matdev.assistantdoc.security;

import java.security.SecureRandom;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crée au premier démarrage un compte administrateur et un compte lecteur.
 * Les mots de passe viennent de ADMIN_PASSWORD / USER_PASSWORD ; sinon un mot de passe aléatoire
 * est généré et affiché une seule fois dans les logs (comme le fait Spring Boot par défaut).
 */
@Component
public class DefaultUsersInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DefaultUsersInitializer.class);

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String adminPassword;
    private final String userPassword;

    public DefaultUsersInitializer(AppUserRepository users, PasswordEncoder passwordEncoder,
                                   @Value("${app.security.admin-password:}") String adminPassword,
                                   @Value("${app.security.user-password:}") String userPassword) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.adminPassword = adminPassword;
        this.userPassword = userPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        createIfMissing("admin", adminPassword, Role.ADMIN);
        createIfMissing("lecteur", userPassword, Role.USER);
    }

    private void createIfMissing(String username, String configuredPassword, Role role) {
        if (users.existsByUsername(username)) {
            return;
        }
        String password = configuredPassword;
        if (password.isBlank()) {
            byte[] random = new byte[12];
            new SecureRandom().nextBytes(random);
            password = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
            log.warn("Compte « {} » ({}) créé avec le mot de passe généré : {}  (à noter, il ne sera plus affiché)",
                    username, role, password);
        } else {
            log.info("Compte « {} » ({}) créé", username, role);
        }
        users.save(new AppUser(username, passwordEncoder.encode(password), role));
    }
}
