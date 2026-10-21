package com.matdev.assistantdoc.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import com.jayway.jsonpath.JsonPath;
import com.matdev.assistantdoc.document.DocumentController;
import com.matdev.assistantdoc.document.DocumentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Vérifie les règles d'accès de l'API (sans base de données : les services sont simulés). */
@WebMvcTest({DocumentController.class, AuthController.class})
@Import({SecurityConfig.class, TokenService.class})
class SecurityRulesTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private DocumentService documentService;

    @MockitoBean
    private AppUserRepository users;

    private static final MockMultipartFile FILE =
            new MockMultipartFile("file", "a.pdf", "application/pdf", new byte[] {1});

    @Test
    void requestWithoutTokenIsRejected() throws Exception {
        mvc.perform(get("/api/documents")).andExpect(status().isUnauthorized());
    }

    @Test
    void userCanListDocuments() throws Exception {
        given(documentService.findAll()).willReturn(List.of());
        mvc.perform(get("/api/documents").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isOk());
    }

    @Test
    void userCannotUploadDocuments() throws Exception {
        mvc.perform(multipart("/api/documents").file(FILE)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanUploadDocuments() throws Exception {
        given(documentService.importDocument(any())).willReturn(null);
        mvc.perform(multipart("/api/documents").file(FILE)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isCreated());
    }

    @Test
    void loginReturnsATokenAcceptedByTheApi() throws Exception {
        given(users.findByUsername("admin"))
                .willReturn(Optional.of(new AppUser("admin", passwordEncoder.encode("secret-123"), Role.ADMIN)));

        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"secret-123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("ADMIN"))
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(body, "$.token");

        // Le jeton émis est validé par le vrai décodeur JWT (signature + expiration)
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.roles[0]").value("ADMIN"));
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        given(users.findByUsername("admin"))
                .willReturn(Optional.of(new AppUser("admin", passwordEncoder.encode("secret-123"), Role.ADMIN)));

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"mauvais\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Identifiant ou mot de passe incorrect"));
    }

    @Test
    void tamperedTokenIsRejected() throws Exception {
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhZG1pbiJ9.faux"))
                .andExpect(status().isUnauthorized());
    }
}
