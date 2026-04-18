package com.shiftmate.controller;

import com.shiftmate.dto.LoginRequest;
import com.shiftmate.security.ShiftMateUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Handles authentication endpoints for the React SPA.
 *
 * <p>Uses Spring Security's {@link AuthenticationManager} to authenticate credentials
 * and stores the resulting security context in the HTTP session so subsequent requests
 * are automatically recognised as authenticated.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;

    /**
     * Authenticates the user and creates a session. Returns basic profile information
     * that the frontend stores in its auth context.
     *
     * @param request     the login credentials
     * @param httpRequest the current HTTP request, used to create the session
     * @return 200 with employee info on success, 401 on bad credentials or inactive account
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request,
                                   HttpServletRequest httpRequest) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(auth);

            HttpSession session = httpRequest.getSession(true);
            session.setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    SecurityContextHolder.getContext()
            );

            ShiftMateUserDetails principal = (ShiftMateUserDetails) auth.getPrincipal();
            return ResponseEntity.ok(Map.of(
                    "employeeId", principal.getEmployeeId(),
                    "email",      principal.getEmail(),
                    "manager",    principal.isManager()
            ));

        } catch (DisabledException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Your account is inactive."));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid email or password."));
        }
    }

    /**
     * Invalidates the current session and clears the security context.
     *
     * @param request the current HTTP request
     * @return 200 with a confirmation message
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of("message", "Logged out successfully."));
    }

    /**
     * Returns the currently authenticated user's profile. Used by the React app
     * on startup to restore session state without requiring a re-login.
     *
     * @return 200 with employee info if authenticated, 401 otherwise
     */
    @GetMapping("/me")
    public ResponseEntity<?> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated."));
        }
        ShiftMateUserDetails principal = (ShiftMateUserDetails) auth.getPrincipal();
        return ResponseEntity.ok(Map.of(
                "employeeId", principal.getEmployeeId(),
                "email",      principal.getEmail(),
                "manager",    principal.isManager()
        ));
    }
}
