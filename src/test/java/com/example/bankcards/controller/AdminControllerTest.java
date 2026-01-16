package com.example.bankcards.controller;

import com.example.bankcards.dto.UserDTO;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.Role;
import com.example.bankcards.mapper.UserMapper;
import com.example.bankcards.service.UserService;
import com.example.bankcards.util.ResponseUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @Mock
    private ResponseUtil responseUtil;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private AdminController adminController;

    private ObjectMapper objectMapper;
    private User testUser;
    private UserDTO testUserDTO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminController).build();
        objectMapper = new ObjectMapper();

        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .role(Role.USER)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build();

        testUserDTO = UserDTO.builder()
                .id(1L)
                .username("testuser")
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .role(Role.USER)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllUsers_ShouldReturnUsersList() throws Exception {

        List<User> users = List.of(testUser);
        List<UserDTO> userDTOs = List.of(testUserDTO);

        when(userService.getAllUsers()).thenReturn(users);
        when(userMapper.toResponse(any(User.class))).thenReturn(testUserDTO);

        Map<String, Object> responseBody = Map.of(
                "message", "Users retrieved successfully",
                "data", userDTOs
        );
        when(responseUtil.successResponse(eq("Users retrieved successfully"), any(List.class)))
                .thenReturn(ResponseEntity.ok(responseBody));

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Users retrieved successfully"))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].username").value("testuser"));

        verify(userService).getAllUsers();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUser_ShouldReturnUser() throws Exception {

        when(userService.getUserById(1L)).thenReturn(testUser);
        when(userMapper.toResponse(testUser)).thenReturn(testUserDTO);

        Map<String, Object> responseBody = Map.of(
                "message", "User retrieved successfully",
                "data", testUserDTO
        );
        when(responseUtil.successResponse(eq("User retrieved successfully"), any(UserDTO.class)))
                .thenReturn(ResponseEntity.ok(responseBody));

        mockMvc.perform(get("/api/admin/users/{userId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.username").value("testuser"));

        verify(userService).getUserById(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateUser_ShouldUpdateAndReturnUser() throws Exception {

        String userJson = """
        {
            "username": "testuser",
            "firstName": "Updated",
            "lastName": "Name",
            "email": "updated@example.com"
        }
        """;

        when(userService.updateUser(eq(1L), any(User.class))).thenReturn(testUser);
        when(userMapper.toResponse(testUser)).thenReturn(testUserDTO);

        Map<String, Object> responseBody = Map.of(
                "message", "User updated successfully",
                "data", testUserDTO
        );
        when(responseUtil.successResponse(eq("User updated successfully"), any(UserDTO.class)))
                .thenReturn(ResponseEntity.ok(responseBody));

        mockMvc.perform(put("/api/admin/users/{userId}", 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User updated successfully"));

        verify(userService).updateUser(eq(1L), any(User.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void disableUser_ShouldDisableUser() throws Exception {

        doNothing().when(userService).disableUser(1L);

        Map<String, Object> responseBody = Map.of(
                "message", "User disabled successfully"
        );
        when(responseUtil.successResponse(eq("User disabled successfully"), isNull()))
                .thenReturn(ResponseEntity.ok(responseBody));

        mockMvc.perform(post("/api/admin/users/{userId}/disable", 1L)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User disabled successfully"));

        verify(userService).disableUser(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void enableUser_ShouldEnableUser() throws Exception {

        doNothing().when(userService).enableUser(1L);

        Map<String, Object> responseBody = Map.of(
                "message", "User enabled successfully"
        );
        when(responseUtil.successResponse(eq("User enabled successfully"), isNull()))
                .thenReturn(ResponseEntity.ok(responseBody));

        mockMvc.perform(post("/api/admin/users/{userId}/enable", 1L)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User enabled successfully"));

        verify(userService).enableUser(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void checkUsername_ShouldReturnTrue_WhenUsernameExists() throws Exception {

        when(userService.existsByUsername("existinguser")).thenReturn(true);

        Map<String, Object> responseBody = Map.of(
                "message", "Username check completed",
                "data", true
        );
        when(responseUtil.successResponse(eq("Username check completed"), eq(true)))
                .thenReturn(ResponseEntity.ok(responseBody));

        mockMvc.perform(get("/api/admin/users/check-username/{username}", "existinguser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Username check completed"))
                .andExpect(jsonPath("$.data").value(true));

        verify(userService).existsByUsername("existinguser");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void checkUsername_ShouldReturnFalse_WhenUsernameNotExists() throws Exception {

        when(userService.existsByUsername("nonexistinguser")).thenReturn(false);

        Map<String, Object> responseBody = Map.of(
                "message", "Username check completed",
                "data", false
        );
        when(responseUtil.successResponse(eq("Username check completed"), eq(false)))
                .thenReturn(ResponseEntity.ok(responseBody));

        mockMvc.perform(get("/api/admin/users/check-username/{username}", "nonexistinguser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Username check completed"))
                .andExpect(jsonPath("$.data").value(false));

        verify(userService).existsByUsername("nonexistinguser");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void checkEmail_ShouldReturnTrue_WhenEmailExists() throws Exception {

        when(userService.existsByEmail("existing@example.com")).thenReturn(true);

        Map<String, Object> responseBody = Map.of(
                "message", "Email check completed",
                "data", true
        );
        when(responseUtil.successResponse(eq("Email check completed"), eq(true)))
                .thenReturn(ResponseEntity.ok(responseBody));

        mockMvc.perform(get("/api/admin/users/check-email/{email}", "existing@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email check completed"))
                .andExpect(jsonPath("$.data").value(true));

        verify(userService).existsByEmail("existing@example.com");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void checkEmail_ShouldReturnFalse_WhenEmailNotExists() throws Exception {

        when(userService.existsByEmail("nonexisting@example.com")).thenReturn(false);

        Map<String, Object> responseBody = Map.of(
                "message", "Email check completed",
                "data", false
        );
        when(responseUtil.successResponse(eq("Email check completed"), eq(false)))
                .thenReturn(ResponseEntity.ok(responseBody));

        mockMvc.perform(get("/api/admin/users/check-email/{email}", "nonexisting@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email check completed"))
                .andExpect(jsonPath("$.data").value(false));

        verify(userService).existsByEmail("nonexisting@example.com");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateUser_WithEmptyBody_ShouldCallService() throws Exception {

        User emptyUser = User.builder().build();
        when(userService.updateUser(eq(1L), any(User.class))).thenReturn(testUser);
        when(userMapper.toResponse(testUser)).thenReturn(testUserDTO);

        Map<String, Object> responseBody = Map.of(
                "message", "User updated successfully",
                "data", testUserDTO
        );
        when(responseUtil.successResponse(eq("User updated successfully"), any(UserDTO.class)))
                .thenReturn(ResponseEntity.ok(responseBody));

        mockMvc.perform(put("/api/admin/users/{userId}", 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        verify(userService).updateUser(eq(1L), any(User.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void checkUsername_WithEmptyUsername_ShouldCallService() throws Exception {
        mockMvc.perform(get("/api/admin/users/check-username/"))
                .andExpect(status().isBadRequest());
    }
}