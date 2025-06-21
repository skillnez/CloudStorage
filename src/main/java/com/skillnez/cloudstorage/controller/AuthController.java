package com.skillnez.cloudstorage.controller;

import com.skillnez.cloudstorage.dto.UserRegistrationRequestDto;
import com.skillnez.cloudstorage.dto.UserSignInRequestDto;
import com.skillnez.cloudstorage.service.FileSystemService;
import com.skillnez.cloudstorage.service.UserService;
import com.skillnez.cloudstorage.utils.PathFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
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
    private final SecurityContextRepository securityContextRepository;

    @Autowired
    public AuthController(AuthenticationManager authenticationManager, UserService userService, SecurityContextRepository securityContextRepository) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.securityContextRepository = securityContextRepository;
    }

    //todo сделать обработку ошибок

    @PostMapping("/sign-in")
    public ResponseEntity<?> signIn(@RequestBody UserSignInRequestDto userSignInRequestDto,
                                    HttpServletRequest request, HttpServletResponse response) {
        return authenticateUser(userSignInRequestDto.getUsername(), userSignInRequestDto.getPassword(), request, response);
    }

    @PostMapping("/sign-up")
    public ResponseEntity<?> signUp(@RequestBody UserRegistrationRequestDto registrationRequestDto,
                                    HttpServletRequest request, HttpServletResponse response) {
        userService.registerUser(registrationRequestDto);

        return authenticateUser(registrationRequestDto.getUsername(), registrationRequestDto.getPassword(), request, response);
    }

    private ResponseEntity<?> authenticateUser (String username, String password,
                                                HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        return ResponseEntity.ok(Map.of("username", username));
    }


}
