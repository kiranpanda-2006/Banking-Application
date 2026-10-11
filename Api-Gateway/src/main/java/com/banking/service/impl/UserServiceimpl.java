package com.banking.service.impl;

import com.banking.dto.LoginDto;
import com.banking.dto.registerDto;
import com.banking.entity.User;
import com.banking.repository.UserRepository;
import com.banking.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceimpl implements UserService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;



    @Override
    public void registerUser(registerDto registerUser) {

        User user = new User();

        user.setName(registerUser.getName());
        user.setDateOfBirth(registerUser.getDateOfBirth());
        user.setEmail(registerUser.getEmail());
        user.setPassword(passwordEncoder.encode(registerUser.getPassword()));
        user.setCreatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    public void loginUser(LoginDto loginUser) {

        User user = userRepository.findByEmail(loginUser.getUsername())
                .orElseThrow(() ->
                        new RuntimeException("Invalid email or password"));

        if (!passwordEncoder.matches(
                loginUser.getPassword(),
                user.getPassword())) {

            throw new RuntimeException("Invalid email or password");
        }

        log.info("User logged in successfully: {}",
                loginUser.getUsername());
    }

    @Override
    public User getUserId(String email) {
        User user =
                userRepository.findByEmail(email)
                        .orElseThrow(()-> new UsernameNotFoundException("no user found."));
        return user;
    }
}
