package com.banking.controller;

import com.banking.client.AccountserviceClient;
import com.banking.dto.AccountDto;
import com.banking.dto.AccountResponseDto;
import com.banking.dto.LoginDto;
import com.banking.dto.registerDto;
import com.banking.entity.User;
import com.banking.exception.PasswordMismatchException;
import com.banking.repository.UserRepository;
import com.banking.service.UserService;
import feign.FeignException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/user/v1")
public class HomeController {


    private final UserService userService;

    private final UserRepository userRepository;

    private final AccountserviceClient accountserviceClient;


    public HomeController(UserService userService,
                          UserRepository userRepository,
                          AccountserviceClient accountserviceClient){
        this.userService = userService;
        this.userRepository = userRepository;
        this.accountserviceClient = accountserviceClient;
    }

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
        return "welcome";
    }
    @GetMapping("/welcome")
    public String welcome(
            Authentication authentication,
            Model model) {

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found"));

        model.addAttribute("user", user);

        try {

            AccountResponseDto account =
                    accountserviceClient.findAccountByUserId(user.getId());

            model.addAttribute("account", account);
            model.addAttribute("hasAccount", true);

        } catch (FeignException.NotFound e) {

            model.addAttribute("hasAccount", false);
        }

        return "welcome";
    }

    @GetMapping("/home")
    public String home() {
        return "home";
    }
}
