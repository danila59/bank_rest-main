package com.example.bankcards.service;

import com.example.bankcards.dto.AuthDTO;
import com.example.bankcards.entity.User;
import com.example.bankcards.security.JwtTokenProvider;
import com.example.bankcards.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthService authService;

    private AuthDTO.LoginRequest loginRequest;
    private AuthDTO.RegisterRequest registerRequest;
    private User testUser;
    private Authentication authentication;
    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        loginRequest = new AuthDTO.LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        registerRequest = new AuthDTO.RegisterRequest();
        registerRequest.setUsername("newuser");
        registerRequest.setPassword("password123");
        registerRequest.setFirstName("John");
        registerRequest.setLastName("Doe");
        registerRequest.setEmail("john@example.com");

        testUser = User.builder()
                .id(1L)
                .username("newuser")
                .email("john@example.com")
                .build();

        Collection<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_USER")
        );

        userPrincipal = new UserPrincipal(
                1L,
                "testuser",
                "encodedPassword",
                "test@example.com",
                authorities,
                true
        );

        authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
    }

    @Test
    void authenticateUser_ShouldReturnJwtResponse() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        when(tokenProvider.generateToken(authentication)).thenReturn("jwt.token.here");

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        AuthDTO.JwtResponse result = authService.authenticateUser(loginRequest);

        assertNotNull(result);
        assertEquals("jwt.token.here", result.getToken());
        assertEquals(1L, result.getId());
        assertEquals("testuser", result.getUsername());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("ROLE_USER", result.getRole());

        verify(authenticationManager).authenticate(argThat(token ->
                token.getPrincipal().equals("testuser") &&
                        token.getCredentials().equals("password123")
        ));
        verify(tokenProvider).generateToken(authentication);
        verify(securityContext).setAuthentication(authentication);
    }

    @Test
    void registerUser_ShouldRegisterAndAuthenticateUser() {

        when(userService.registerUser(any(AuthDTO.RegisterRequest.class))).thenReturn(testUser);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);

        when(tokenProvider.generateToken(authentication)).thenReturn("jwt.token.here");

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        AuthDTO.JwtResponse result = authService.registerUser(registerRequest);

        assertNotNull(result);
        assertEquals("jwt.token.here", result.getToken());
        assertEquals(1L, result.getId());

        verify(userService).registerUser(any(AuthDTO.RegisterRequest.class));
        verify(authenticationManager).authenticate(argThat(token ->
                token.getPrincipal().equals("newuser") &&
                        token.getCredentials().equals("password123")
        ));
    }

    @Test
    void getCurrentUserId_ShouldReturnUserId() {

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        Long userId = authService.getCurrentUserId();

        assertEquals(1L, userId);
    }

    @Test
    void getCurrentUserId_WhenAuthenticationIsNull_ShouldThrowException() {
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(securityContext);

        assertThrows(NullPointerException.class, () -> authService.getCurrentUserId());
    }

    @Test
    void authenticateUser_ShouldSetAuthenticationInSecurityContext() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(tokenProvider.generateToken(authentication)).thenReturn("jwt.token.here");

        SecurityContext securityContext = mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);

        authService.authenticateUser(loginRequest);

        verify(securityContext).setAuthentication(authentication);
    }

    @Test
    void authenticateUser_WithInvalidCredentials_ShouldThrowException() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException("Invalid credentials"));

        assertThrows(org.springframework.security.authentication.BadCredentialsException.class,
                () -> authService.authenticateUser(loginRequest));
    }

    @Test
    void registerUser_ShouldCallUserServiceWithCorrectParameters() {
        when(userService.registerUser(any(AuthDTO.RegisterRequest.class))).thenReturn(testUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(tokenProvider.generateToken(authentication)).thenReturn("jwt.token.here");

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        authService.registerUser(registerRequest);

        verify(userService).registerUser(argThat(request ->
                request.getUsername().equals("newuser") &&
                        request.getPassword().equals("password123") &&
                        request.getFirstName().equals("John") &&
                        request.getLastName().equals("Doe") &&
                        request.getEmail().equals("john@example.com")
        ));
    }
}