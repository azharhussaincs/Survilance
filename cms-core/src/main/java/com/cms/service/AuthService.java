package com.cms.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.cms.model.User;
import com.cms.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Handles user authentication and session management.
 */
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private static AuthService instance;
    private final UserRepository userRepository;
    private User currentUser;

    private AuthService() {
        this.userRepository = new UserRepository();
    }

    public static synchronized AuthService getInstance() {
        if (instance == null) instance = new AuthService();
        return instance;
    }

    /**
     * Authenticate user with username and password.
     * @return AuthResult with success flag and message
     */
    public AuthResult authenticate(String username, String password) {
        if (username == null || username.isBlank()) {
            return AuthResult.failure("Username is required");
        }
        if (password == null || password.isBlank()) {
            return AuthResult.failure("Password is required");
        }

        Optional<User> userOpt = userRepository.findByUsername(username.trim());
        if (userOpt.isEmpty()) {
            logger.warn("Login attempt with unknown username: {}", username);
            return AuthResult.failure("Invalid username or password");
        }

        User user = userOpt.get();
        BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), user.getPasswordHash());
        if (!result.verified) {
            logger.warn("Failed login attempt for user: {}", username);
            return AuthResult.failure("Invalid username or password");
        }

        currentUser = user;
        userRepository.updateLastLogin(user.getId());
        logger.info("User '{}' logged in successfully with role: {}", username, user.getRole());
        return AuthResult.success(user);
    }

    public void logout() {
        if (currentUser != null) {
            logger.info("User '{}' logged out", currentUser.getUsername());
        }
        currentUser = null;
    }

    public User getCurrentUser() { return currentUser; }

    public boolean isLoggedIn() { return currentUser != null; }

    public boolean isAdmin() {
        return currentUser != null && currentUser.getRole() == User.UserRole.ADMIN;
    }

    public record AuthResult(boolean success, String message, User user) {
        public static AuthResult success(User user) {
            return new AuthResult(true, "Login successful", user);
        }
        public static AuthResult failure(String message) {
            return new AuthResult(false, message, null);
        }
    }
}
