package com.banking.controller;

import com.banking.dto.LoginDto;
import com.banking.dto.registerDto;
import com.banking.exception.PasswordMismatchException;
import com.banking.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/user/v1")
@RequiredArgsConstructor
public class HomeController {


    private final UserService userService;

    @GetMapping
    public String register(Model model){
        model.addAttribute("register",new registerDto());
        return "register";
    }
    @PostMapping("/register")
    public String getUser(@ModelAttribute("register") registerDto registerDto){
        if( !registerDto.getPassword().equals( registerDto.getConfirmPassword())){
            throw new PasswordMismatchException("Password and Confirm Password not match."+registerDto.getPassword()+ " "
            +registerDto.getConfirmPassword());
        }
        userService.registerUser(registerDto);
        return "redirect:/api/user/v1/login";
    }
    @GetMapping("/login")
    public String login(Model model){
        model.addAttribute("login", new LoginDto());
        return "login";
    }

    @PostMapping("/login-info")
    public String loginInfo(@ModelAttribute("login") LoginDto loginCredential){
        userService.loginUser(loginCredential);
        return "home";
    }
    @GetMapping("/home")
    public String home(){
        return "home";
    }
}
