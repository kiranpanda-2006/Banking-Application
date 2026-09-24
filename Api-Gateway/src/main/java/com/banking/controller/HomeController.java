package com.banking.controller;

import com.banking.dto.LoginDto;
import com.banking.dto.registerDto;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/user/v1")
public class HomeController {

    @GetMapping
    public String register(Model model){
        model.addAttribute("register",new registerDto());
        return "register";
    }
    @PostMapping("/register")
    public String getUser(@ModelAttribute("register") registerDto register, Model model){
        System.out.println(register.getName()+register.getDateOfBirth());
        return "redirect:/api/user/v1/login";
    }
    @GetMapping("/login")
    public String login(Model model){
        model.addAttribute("login", new LoginDto());
        return "login";
    }

    @PostMapping("/login-info")
    public String loginInfo(@ModelAttribute("login") LoginDto login){
        System.out.println(login.getUsername() + login.getPassword());
        return "home";
    }
    @GetMapping("/home")
    public String home(){
        return "home";
    }
}
