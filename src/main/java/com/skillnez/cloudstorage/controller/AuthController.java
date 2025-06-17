package com.skillnez.cloudstorage.controller;

import com.skillnez.cloudstorage.dto.UserRegistrationRequestDto;
import com.skillnez.cloudstorage.dto.UserSignInRequestDto;
import com.skillnez.cloudstorage.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;

    @Autowired
    public AuthController(AuthenticationManager authenticationManager, UserService userService) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
    }

    @PostMapping("/sign-in")
    public ResponseEntity<?> signIn(@RequestBody UserSignInRequestDto userSignInRequestDto, HttpServletRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(userSignInRequestDto.getUsername(), userSignInRequestDto.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        request.getSession(true);
        //todo добавь @Valid и обработку ошибок из DTO
        return ResponseEntity.ok(Map.of("username", userSignInRequestDto.getUsername()));
    }

    @PostMapping("/sign-up")
    public ResponseEntity<?> signUp(@RequestBody UserRegistrationRequestDto registrationRequestDto, HttpServletRequest request ) {
        userService.registerUser(registrationRequestDto);
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(registrationRequestDto.getUsername(), registrationRequestDto.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        request.getSession(true);
        request.getSession().setAttribute("username", registrationRequestDto.getUsername());
        return ResponseEntity.ok(Map.of("username", registrationRequestDto.getUsername()));
        //todo добавь @Valid и обработку ошибок из DTO и exception
    }


}
