package com.example.bankcards.service;

import com.example.bankcards.dto.AuthDTO;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.Role;
import com.example.bankcards.exception.UserOperationException;
import com.example.bankcards.repository.UserRepository;
import com.example.bankcards.util.ValidationUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ValidationUtil validationUtil;

    @InjectMocks
    private UserService userService;

    private AuthDTO.RegisterRequest registerRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        registerRequest = new AuthDTO.RegisterRequest();
        registerRequest.setUsername("newuser");
        registerRequest.setPassword("password123");
        registerRequest.setFirstName("John");
        registerRequest.setLastName("Doe");
        registerRequest.setEmail("john@example.com");

        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .password("encodedPassword")
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .role(Role.USER)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void registerUser_ShouldRegisterUserSuccessfully() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(validationUtil.isValidUsername("newuser")).thenReturn(true);
        when(validationUtil.isValidName("John")).thenReturn(true);
        when(validationUtil.isValidName("Doe")).thenReturn(true);
        when(validationUtil.isValidEmail("john@example.com")).thenReturn(true);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User result = userService.registerUser(registerRequest);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("testuser", result.getUsername());
        verify(userRepository).existsByUsername("newuser");
        verify(userRepository).existsByEmail("john@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_ShouldThrowException_WhenUsernameExists() {
        when(userRepository.existsByUsername("newuser")).thenReturn(true);

        assertThrows(UserOperationException.class, () ->
                userService.registerUser(registerRequest));
    }

    @Test
    void registerUser_ShouldThrowException_WhenEmailExists() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThrows(UserOperationException.class, () ->
                userService.registerUser(registerRequest));
    }

    @Test
    void registerUser_ShouldThrowException_WhenInvalidUsername() {
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(validationUtil.isValidUsername("newuser")).thenReturn(false);

        assertThrows(UserOperationException.class, () ->
                userService.registerUser(registerRequest));
    }

    @Test
    void registerUser_ShouldThrowException_WhenInvalidEmail() {
        // Arrange
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(validationUtil.isValidUsername("newuser")).thenReturn(true);
        when(validationUtil.isValidName("John")).thenReturn(true);
        when(validationUtil.isValidName("Doe")).thenReturn(true);
        when(validationUtil.isValidEmail("john@example.com")).thenReturn(false);


        assertThrows(UserOperationException.class, () ->
                userService.registerUser(registerRequest));
    }

    @Test
    void getUserById_ShouldReturnUser() {
        when(userRepository.findActiveById(1L)).thenReturn(Optional.of(testUser));

        User result = userService.getUserById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(userRepository).findActiveById(1L);
    }

    @Test
    void getUserById_ShouldThrowException_WhenUserNotFound() {
        when(userRepository.findActiveById(1L)).thenReturn(Optional.empty());

        assertThrows(UserOperationException.class, () ->
                userService.getUserById(1L));
    }

    @Test
    void getAllUsers_ShouldReturnAllUsers() {
        List<User> users = List.of(testUser);
        when(userRepository.findAll()).thenReturn(users);

        List<User> result = userService.getAllUsers();

        assertEquals(1, result.size());
        verify(userRepository).findAll();
    }

    @Test
    void updateUser_ShouldUpdateUser() {
        User updatedUserData = User.builder()
                .firstName("Updated")
                .lastName("Name")
                .email("updated@example.com")
                .role(Role.ADMIN)
                .build();

        when(userRepository.findActiveById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User result = userService.updateUser(1L, updatedUserData);

        assertNotNull(result);
        verify(userRepository).findActiveById(1L);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void disableUser_ShouldDisableUser() {
        when(userRepository.findActiveById(1L)).thenReturn(Optional.of(testUser));

        userService.disableUser(1L);

        verify(userRepository).findActiveById(1L);
        verify(userRepository).save(argThat(user -> !user.isEnabled()));
    }

    @Test
    void enableUser_ShouldEnableUser() {
        User disabledUser = User.builder()
                .id(1L)
                .enabled(false)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(disabledUser));

        userService.enableUser(1L);

        verify(userRepository).findById(1L);
        verify(userRepository).save(argThat(user -> user.isEnabled()));
    }

    @Test
    void enableUser_ShouldThrowException_WhenUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(UserOperationException.class, () ->
                userService.enableUser(1L));
    }

    @Test
    void getUserByUsername_ShouldReturnUser() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        User result = userService.getUserByUsername("testuser");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void getUserByUsername_ShouldThrowException_WhenUserNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(UserOperationException.class, () ->
                userService.getUserByUsername("unknown"));
    }

    @Test
    void existsByUsername_ShouldReturnTrue_WhenUsernameExists() {
        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        boolean result = userService.existsByUsername("existinguser");

        assertTrue(result);
        verify(userRepository).existsByUsername("existinguser");
    }

    @Test
    void existsByUsername_ShouldReturnFalse_WhenUsernameNotExists() {
        when(userRepository.existsByUsername("nonexisting")).thenReturn(false);

        boolean result = userService.existsByUsername("nonexisting");

        assertFalse(result);
        verify(userRepository).existsByUsername("nonexisting");
    }

    @Test
    void existsByEmail_ShouldReturnTrue_WhenEmailExists() {
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        boolean result = userService.existsByEmail("existing@example.com");

        assertTrue(result);
        verify(userRepository).existsByEmail("existing@example.com");
    }

    @Test
    void existsByEmail_ShouldReturnFalse_WhenEmailNotExists() {
        when(userRepository.existsByEmail("nonexisting@example.com")).thenReturn(false);

        boolean result = userService.existsByEmail("nonexisting@example.com");

        assertFalse(result);
        verify(userRepository).existsByEmail("nonexisting@example.com");
    }
}