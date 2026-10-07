package com.banking.service;

import com.banking.dto.LoginDto;
import com.banking.dto.registerDto;
import com.banking.entity.User;
import com.banking.repository.UserRepository;

public interface UserService {
    void registerUser(registerDto registerUser);

    void loginUser(LoginDto loginUser);

    User getUserId(String email);

}
