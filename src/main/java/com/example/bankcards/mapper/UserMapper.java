package com.example.bankcards.mapper;

import com.example.bankcards.dto.UserDTO;
import com.example.bankcards.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserDTO toResponse(User user){
        if (user == null)return null;

        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole())
                .enabled(user.isAccountNonLocked())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
