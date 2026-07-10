package dn.questenginev2.auth.controller;

import dn.questenginev2.auth.dto.LoginResponse;
import dn.questenginev2.auth.dto.RegisterRequest;
import dn.questenginev2.auth.service.RegisterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = RegisterController.class)
@Import(dn.questenginev2.config.test.TestSecurityConfig.class)
class RegisterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegisterService registerService;

    @BeforeEach
    void setUp() {
        when(registerService.register(any(RegisterRequest.class)))
                .thenReturn(new LoginResponse("Test User", "test-jwt-token"));
    }

    @Test
    void register_returnsCreatedUser_whenRequestIsValid() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"testuser\",\"email\":\"test@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.publicName").value("Test User"))
                .andExpect(jsonPath("$.token").value("test-jwt-token"));
    }
}
