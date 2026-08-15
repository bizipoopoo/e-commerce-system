package com.aurora.commerce.identity;

import com.aurora.commerce.shared.error.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
class IdentityService {

    private final UserAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokenService;

    IdentityService(
            UserAccountRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService tokenService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Transactional
    AuthResult register(String email, String password, String displayName) {
        String normalizedEmail = normalizeEmail(email);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new BusinessException("EMAIL_ALREADY_REGISTERED", "该邮箱已经注册", HttpStatus.CONFLICT);
        }
        UserAccount user = userRepository.save(new UserAccount(
                normalizedEmail,
                passwordEncoder.encode(password),
                displayName.trim(),
                UserRole.CUSTOMER
        ));
        return toAuthResult(user);
    }

    @Transactional(readOnly = true)
    AuthResult login(String email, String password) {
        UserAccount user = userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(this::invalidCredentials);
        if (!user.active() || !passwordEncoder.matches(password, user.passwordHash())) {
            throw invalidCredentials();
        }
        return toAuthResult(user);
    }

    @Transactional(readOnly = true)
    UserView currentUser(String email) {
        return userRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .map(this::toUserView)
                .orElseThrow(() -> new BusinessException("USER_NOT_FOUND", "用户不存在", HttpStatus.NOT_FOUND));
    }

    @Transactional
    void ensureAdmin(String email, String password) {
        String normalizedEmail = normalizeEmail(email);
        if (!userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            userRepository.save(new UserAccount(
                    normalizedEmail,
                    passwordEncoder.encode(password),
                    "Aurora Admin",
                    UserRole.ADMIN
            ));
        }
    }

    private AuthResult toAuthResult(UserAccount user) {
        JwtTokenService.Token token = tokenService.issue(user);
        return new AuthResult(token.value(), token.expiresInSeconds(), toUserView(user));
    }

    private UserView toUserView(UserAccount user) {
        return new UserView(user.id(), user.email(), user.displayName(), user.role().name());
    }

    private BusinessException invalidCredentials() {
        return new BusinessException("INVALID_CREDENTIALS", "邮箱或密码错误", HttpStatus.UNAUTHORIZED);
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    record AuthResult(String accessToken, long expiresIn, UserView user) {
    }

    record UserView(Long id, String email, String displayName, String role) {
    }
}
