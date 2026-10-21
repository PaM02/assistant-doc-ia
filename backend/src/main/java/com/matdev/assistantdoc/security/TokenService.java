package com.matdev.assistantdoc.security;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Génère les jetons JWT remis après une connexion réussie. */
@Service
public class TokenService {

    static final String ISSUER = "assistant-doc-ia";

    private final JwtEncoder encoder;
    private final Duration validity;

    public TokenService(JwtEncoder encoder, @Value("${app.security.token-validity:8h}") Duration validity) {
        this.encoder = encoder;
        this.validity = validity;
    }

    public IssuedToken issue(Authentication authentication) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(validity);
        // Les autorisations Spring sont « ROLE_ADMIN » : on stocke juste « ADMIN » dans le jeton
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.replaceFirst("^ROLE_", ""))
                .toList();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(authentication.getName())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim(SecurityConfig.ROLES_CLAIM, roles)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(token, expiresAt, authentication.getName(), roles);
    }

    public record IssuedToken(String token, Instant expiresAt, String username, List<String> roles) {
    }
}
