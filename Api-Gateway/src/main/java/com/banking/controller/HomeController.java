package com.banking.controller;

import com.banking.client.AccountserviceClient;
import com.banking.dto.AccountDto;
import com.banking.dto.LoginDto;
import com.banking.dto.registerDto;
import com.banking.exception.PasswordMismatchException;
import com.banking.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/user/v1")
public class HomeController {


    private final UserService userService;

    private final AccountserviceClient accountserviceClient;

    public HomeController(AccountserviceClient accountserviceClient,UserService userService){
        this.accountserviceClient = accountserviceClient;
        this.userService = userService;
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
    public String welcome(){
        return "welcome";
    }

    @GetMapping("/home")
    public String home(Model model) {

        model.addAttribute("accountDto", new AccountDto());
        model.addAttribute(
                "accountTypes",
                accountserviceClient.getAccountTypes()
        );

        return "home";
    }

    @PostMapping("/submit")
    public String submitRequest(
            @Valid @ModelAttribute("accountDto") AccountDto accountDto,
            BindingResult result,
            Model model) {

        if (result.hasErrors()) {

            model.addAttribute(
                    "accountTypes",
                    accountserviceClient.getAccountTypes()
            );

            return "home";
        }

        accountserviceClient.createAccount(accountDto);

        return "redirect:/api/user/v1/home";
    }
}
