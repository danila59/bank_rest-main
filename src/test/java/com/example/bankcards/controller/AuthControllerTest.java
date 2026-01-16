package com.example.bankcards.controller;

import com.example.bankcards.dto.AuthDTO;
import com.example.bankcards.dto.UserDTO;
import com.example.bankcards.entity.User;
import com.example.bankcards.mapper.UserMapper;
import com.example.bankcards.service.AuthService;
import com.example.bankcards.service.UserService;
import com.example.bankcards.util.ResponseUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @Mock
    private ResponseUtil responseUtil;

    @Mock
    private UserService userService;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AuthController authController;

    private ObjectMapper objectMapper;
    private AuthDTO.LoginRequest loginRequest;
    private AuthDTO.RegisterRequest registerRequest;
    private AuthDTO.JwtResponse jwtResponse;
    private User testUser;
    private UserDTO testUserDTO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
        objectMapper = new ObjectMapper();

        loginRequest = new AuthDTO.LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        registerRequest = new AuthDTO.RegisterRequest();
        registerRequest.setUsername("newuser");
        registerRequest.setPassword("password123");
        registerRequest.setFirstName("John");
        registerRequest.setLastName("Doe");
        registerRequest.setEmail("john@example.com");

        jwtResponse = new AuthDTO.JwtResponse(
                "jwt.token.here",
                1L,
                "testuser",
                "test@example.com",
                "USER"
        );

        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .build();

        testUserDTO = UserDTO.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .build();
    }

    @Test
    void login_ShouldReturnJwtToken() throws Exception {

        when(authService.authenticateUser(any(AuthDTO.LoginRequest.class))).thenReturn(jwtResponse);

        Map<String, Object> responseBody = Map.of(
                "message", "User authenticated successfully",
                "data", jwtResponse
        );
        when(responseUtil.successResponse(eq("User authenticated successfully"), any(AuthDTO.JwtResponse.class)))
                .thenReturn(ResponseEntity.ok(responseBody));

        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User authenticated successfully"))
                .andExpect(jsonPath("$.data.token").value("jwt.token.here"));

        verify(authService).authenticateUser(any(AuthDTO.LoginRequest.class));
    }

    @Test
    void login_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        String invalidJson = "{}";

        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_WithBlankUsername_ShouldReturnBadRequest() throws Exception {

        AuthDTO.LoginRequest invalidRequest = new AuthDTO.LoginRequest();
        invalidRequest.setUsername("");
        invalidRequest.setPassword("password");

        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_ShouldReturnJwtToken() throws Exception {

        when(authService.registerUser(any(AuthDTO.RegisterRequest.class))).thenReturn(jwtResponse);

        Map<String, Object> responseBody = Map.of(
                "message", "User registered successfully",
                "data", jwtResponse
        );
        when(responseUtil.createdResponse(eq("User registered successfully"), any(AuthDTO.JwtResponse.class)))
                .thenReturn(ResponseEntity.status(201).body(responseBody));

        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.data.username").value("testuser"));

        verify(authService).registerUser(any(AuthDTO.RegisterRequest.class));
    }

    @Test
    void register_WithInvalidEmail_ShouldReturnBadRequest() throws Exception {

        String invalidJson = """
            {
                "username": "newuser",
                "password": "password123",
                "firstName": "John",
                "lastName": "Doe",
                "email": "invalid-email"
            }
            """;

        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_WithShortPassword_ShouldReturnBadRequest() throws Exception {

        AuthDTO.RegisterRequest invalidRequest = new AuthDTO.RegisterRequest();
        invalidRequest.setUsername("newuser");
        invalidRequest.setPassword("123");
        invalidRequest.setFirstName("John");
        invalidRequest.setLastName("Doe");
        invalidRequest.setEmail("john@example.com");


        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCurrentUser_ShouldReturnUserInfo_WhenAuthenticated() throws Exception {

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "testuser",
                null,
                java.util.Collections.emptyList()
        );

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(userMapper.toResponse(testUser)).thenReturn(testUserDTO);

        Map<String, Object> responseBody = Map.of(
                "message", "Current user retrieved",
                "data", testUserDTO
        );
        when(responseUtil.successResponse(eq("Current user retrieved"), any(UserDTO.class)))
                .thenReturn(ResponseEntity.ok(responseBody));

        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Current user retrieved"))
                .andExpect(jsonPath("$.data.username").value("testuser"));

        verify(userService).getUserByUsername("testuser");
    }

    @Test
    void getCurrentUser_ShouldReturnUnauthorized_WhenNotAuthenticated() throws Exception {

        SecurityContextHolder.clearContext();

        Map<String, Object> responseBody = Map.of(
                "message", "User not authenticated",
                "error", "Please login first"
        );
        when(responseUtil.errorResponse(eq(HttpStatus.UNAUTHORIZED), eq("User not authenticated"), eq("Please login first")))
                .thenReturn(ResponseEntity.status(401).body(responseBody));

        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("User not authenticated"));
    }

    @Test
    void getCurrentUser_ShouldReturnUnauthorized_WhenAnonymousUser() throws Exception {

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "anonymousUser",
                null,
                java.util.Collections.emptyList()
        );

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        Map<String, Object> responseBody = Map.of(
                "message", "User not authenticated",
                "error", "Please login first"
        );
        when(responseUtil.errorResponse(eq(HttpStatus.UNAUTHORIZED), eq("User not authenticated"), eq("Please login first")))
                .thenReturn(ResponseEntity.status(401).body(responseBody));

        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("User not authenticated"));
    }

    @Test
    void login_ShouldCallAuthServiceWithCorrectParameters() throws Exception {

        when(authService.authenticateUser(any(AuthDTO.LoginRequest.class))).thenReturn(jwtResponse);

        Map<String, Object> responseBody = Map.of(
                "message", "User authenticated successfully",
                "data", jwtResponse
        );
        when(responseUtil.successResponse(eq("User authenticated successfully"), any(AuthDTO.JwtResponse.class)))
                .thenReturn(ResponseEntity.ok(responseBody));


        mockMvc.perform(post("/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)));

        verify(authService).authenticateUser(argThat(request ->
                request.getUsername().equals("testuser") &&
                        request.getPassword().equals("password123")
        ));
    }
}