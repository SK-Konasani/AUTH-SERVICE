package com.revconnect.authservice.controller;

import com.revconnect.authservice.dto.*;
import com.revconnect.authservice.model.User;
import com.revconnect.authservice.repository.UserRepository;
import com.revconnect.authservice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService service;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Auth Service is healthy and reachable!");
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        String response = service.requestRegistrationOtp(request);
        return switch (response) {
            case "OTP_SENT" -> ResponseEntity.ok("OTP sent to your email. Verify to complete registration.");
            case "PASSWORD_TOO_SHORT" -> ResponseEntity.badRequest().body("Password must be at least 8 characters long");
            case "PASSWORD_MISMATCH" -> ResponseEntity.badRequest().body("Passwords do not match");
            case "USERNAME_EXISTS" -> ResponseEntity.badRequest().body("Username already taken");
            case "EMAIL_EXISTS" -> ResponseEntity.badRequest().body("Email already registered");
            case "EMAIL_REQUIRED" -> ResponseEntity.badRequest().body("Email is required");
            case "USERNAME_REQUIRED" -> ResponseEntity.badRequest().body("Username is required");
            case "ROLE_REQUIRED" -> ResponseEntity.badRequest().body("Role is required");
            case "OTP_SEND_FAILED" -> ResponseEntity.status(500).body("Unable to send OTP right now.");
            default -> ResponseEntity.badRequest().body(response);
        };
    }

    @PostMapping("/register/request")
    public ResponseEntity<?> requestRegisterOtp(@RequestBody RegisterRequest request) {
        return register(request);
    }

    @PostMapping("/register/verify")
    public ResponseEntity<?> verifyRegisterOtp(@RequestBody VerifyOtpRequestDTO request) {
        String response = service.verifyRegistrationOtp(request.getEmail(), request.getOtp());
        return switch (response) {
            case "SUCCESS" -> ResponseEntity.ok("User Registered Successfully");
            case "EMAIL_REQUIRED" -> ResponseEntity.badRequest().body("Email is required");
            case "OTP_REQUIRED" -> ResponseEntity.badRequest().body("OTP is required");
            case "OTP_INVALID" -> ResponseEntity.status(401).body("Invalid OTP");
            case "OTP_INVALID_OR_EXPIRED" -> ResponseEntity.status(401).body("OTP is invalid or expired");
            case "OTP_TOO_MANY_ATTEMPTS" -> ResponseEntity.status(429).body("Too many attempts. Request a new OTP.");
            case "USERNAME_EXISTS" -> ResponseEntity.badRequest().body("Username already taken");
            case "EMAIL_EXISTS" -> ResponseEntity.badRequest().body("Email already registered");
            default -> ResponseEntity.badRequest().body(response);
        };
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO request) {
        String token = service.verify(request);
        if ("ACCOUNT_DEACTIVATED".equals(token))
            return ResponseEntity.status(403).body("Your account is deactivated.");
        if (token == null || token.equals("FAIL"))
            return ResponseEntity.status(401).body("Invalid username or password");

        User user = service.findByUsername(request.getUsername());
        if (user == null)
            return ResponseEntity.status(401).body("User not found after login");

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("user", user);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        return ResponseEntity.ok("Logged out successfully");
    }

    @PostMapping("/reactivate")
    public ResponseEntity<?> reactivate(@RequestBody LoginRequestDTO request) {
        String response = service.reactivateAccount(
                request != null ? request.getUsername() : null,
                request != null ? request.getPassword() : null);
        return switch (response) {
            case "ACCOUNT_REACTIVATED" -> ResponseEntity.ok("Account reactivated successfully");
            case "ACCOUNT_ALREADY_ACTIVE" -> ResponseEntity.badRequest().body("Account is already active");
            case "USERNAME_REQUIRED" -> ResponseEntity.badRequest().body("Username or email is required");
            case "PASSWORD_REQUIRED" -> ResponseEntity.badRequest().body("Password is required");
            case "INVALID_CREDENTIALS" -> ResponseEntity.status(401).body("Invalid credentials");
            default -> ResponseEntity.badRequest().body(response);
        };
    }

    @PostMapping("/forgot-password/request")
    public ResponseEntity<?> requestForgotPasswordOtp(@RequestBody ForgotPasswordRequestDTO request) {
        String response = service.requestPasswordResetOtp(request);
        return switch (response) {
            case "OTP_SENT" -> ResponseEntity.ok("If this email is registered, an OTP has been sent.");
            case "EMAIL_REQUIRED" -> ResponseEntity.badRequest().body("Email is required");
            case "OTP_SEND_FAILED" -> ResponseEntity.status(500).body("Unable to send OTP right now.");
            default -> ResponseEntity.badRequest().body(response);
        };
    }

    @PostMapping("/forgot-password/verify")
    public ResponseEntity<?> verifyForgotPasswordOtp(@RequestBody VerifyOtpRequestDTO request) {
        String response = service.verifyPasswordResetOtp(request.getEmail(), request.getOtp());
        return switch (response) {
            case "OTP_VERIFIED" -> ResponseEntity.ok("OTP verified successfully");
            case "EMAIL_REQUIRED" -> ResponseEntity.badRequest().body("Email is required");
            case "OTP_REQUIRED" -> ResponseEntity.badRequest().body("OTP is required");
            case "OTP_INVALID" -> ResponseEntity.status(401).body("Invalid OTP");
            case "OTP_INVALID_OR_EXPIRED" -> ResponseEntity.status(401).body("OTP is invalid or expired");
            case "OTP_TOO_MANY_ATTEMPTS" -> ResponseEntity.status(429).body("Too many attempts.");
            default -> ResponseEntity.badRequest().body(response);
        };
    }

    @PostMapping("/forgot-password/reset")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequestDTO request) {
        String response = service.resetPasswordWithOtp(request);
        return switch (response) {
            case "PASSWORD_RESET_SUCCESS" -> ResponseEntity.ok("Password reset successfully");
            case "EMAIL_REQUIRED" -> ResponseEntity.badRequest().body("Email is required");
            case "OTP_REQUIRED" -> ResponseEntity.badRequest().body("OTP is required");
            case "PASSWORD_TOO_SHORT" -> ResponseEntity.badRequest().body("Password must be at least 8 characters");
            case "PASSWORD_MISMATCH" -> ResponseEntity.badRequest().body("Passwords do not match");
            case "OTP_INVALID" -> ResponseEntity.status(401).body("Invalid OTP");
            case "OTP_INVALID_OR_EXPIRED" -> ResponseEntity.status(401).body("OTP is invalid or expired");
            case "OTP_TOO_MANY_ATTEMPTS" -> ResponseEntity.status(429).body("Too many attempts.");
            default -> ResponseEntity.badRequest().body(response);
        };
    }

    @GetMapping("/search")
    public ResponseEntity<List<User>> searchUsers(@RequestParam("query") String query) {
        return ResponseEntity.ok(
                userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(query, query));
    }

    @GetMapping("/allUsers")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(service.getAllUsers());
    }

    @DeleteMapping("/me")
    public ResponseEntity<?> deleteAccount(@RequestBody(required = false) AccountActionRequestDTO request) {
        String response = service.deleteCurrentUser(request != null ? request.getPassword() : null);
        return response.equals("Account deleted successfully")
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
    }
}