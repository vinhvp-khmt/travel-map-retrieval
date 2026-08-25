package com.travelmap.api.auth;

import com.travelmap.api.auth.dto.AuthResponse;
import com.travelmap.api.auth.service.AuthenticationService;
import com.travelmap.api.auth.service.JwtClaims;
import com.travelmap.api.auth.service.TokenService;
import com.travelmap.api.auth.model.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthApiSecurityTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean AuthenticationService authenticationService;
    @MockitoBean TokenService tokenService;

    @Test
    void invalidRegisterRequestReturnsFieldValidationErrors() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("{\"email\":\"bad\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fields.email").exists())
                .andExpect(jsonPath("$.fields.password").exists());
    }

    @Test
    void loginIsPublic() throws Exception {
        when(authenticationService.login(any())).thenReturn(
                new AuthResponse("Bearer", "access", 1800, "refresh", 604800));
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"Password1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access"));
    }

    @Test
    void userRoleCannotAccessAdminRoute() throws Exception {
        when(tokenService.parseAccessToken("user-token")).thenReturn(
                new JwtClaims(UUID.randomUUID(), "user@example.com", UserRole.USER,
                        Instant.now().plusSeconds(60).getEpochSecond()));
        mockMvc.perform(get("/api/v1/admin/probe").header("Authorization", "Bearer user-token"))
                .andExpect(status().isForbidden());
    }
}
