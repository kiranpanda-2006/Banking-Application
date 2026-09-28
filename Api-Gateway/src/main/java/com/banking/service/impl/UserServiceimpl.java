package com.banking.service.impl;

import com.banking.dto.LoginDto;
import com.banking.dto.registerDto;
import com.banking.entity.User;
import com.banking.repository.UserRepository;
import com.banking.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceimpl implements UserService {

    private UserRepository userRepository;

    private Map<String,String> db = new HashMap<>();


    @Override
    public void registerUser(registerDto registerUser) {
        db.put("email", registerUser.getEmail());
        db.put("password", registerUser.getPassword());
    }

    @Override
    public void loginUser(LoginDto loginUser) {
        String userName = db.get("email");
        String password = db.get("password");

        if (!loginUser.getUsername().equals(userName) || !loginUser.getPassword().equals(password)){
            throw new RuntimeException("invalid User.");
        }
        log.info("user Logged in Successfully: {}", loginUser.getUsername());
    }
}
