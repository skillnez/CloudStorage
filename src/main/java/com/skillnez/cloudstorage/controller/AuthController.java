package com.skillnez.cloudstorage.controller;

import com.skillnez.cloudstorage.dto.UserRegistrationRequestDto;
import com.skillnez.cloudstorage.dto.UserRegistrationResponseDto;
import com.skillnez.cloudstorage.dto.UserSignInRequestDto;
import com.skillnez.cloudstorage.entity.CustomUserDetails;
import com.skillnez.cloudstorage.service.FileSystemService;
import com.skillnez.cloudstorage.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final FileSystemService fileSystemService;

    @Autowired
    public AuthController(AuthenticationManager authenticationManager, UserService userService, SecurityContextRepository securityContextRepository, FileSystemService fileSystemService) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.securityContextRepository = securityContextRepository;
        this.fileSystemService = fileSystemService;
    }

    @PostMapping("/sign-in")
    public ResponseEntity<Map<String, String>> signIn(@RequestBody @Valid UserSignInRequestDto userSignInRequestDto,
                                    HttpServletRequest request, HttpServletResponse response) {
        authenticateUser(userSignInRequestDto.getUsername(), userSignInRequestDto.getPassword(), request, response);
        return ResponseEntity.ok().body(Map.of("username", userSignInRequestDto.getUsername()));
    }

    @PostMapping("/sign-up")
    public ResponseEntity<Map<String, String>> signUp(
                                                @RequestBody @Valid UserRegistrationRequestDto registrationRequestDto,
                                                HttpServletRequest request,
                                                HttpServletResponse response) {
        UserRegistrationResponseDto user = userService.registerUser(registrationRequestDto);
        authenticateUser(registrationRequestDto.getUsername(),
                registrationRequestDto.getPassword(), request, response);
        fileSystemService.createRootFolder(user.getId());
        return ResponseEntity.status(201).body(Map.of("username", registrationRequestDto.getUsername()));
    }

    private void authenticateUser(String username, String password, HttpServletRequest request,
                                               HttpServletResponse response) {
        Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(username, password));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }


}
