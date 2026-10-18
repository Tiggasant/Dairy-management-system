package com.ghunsai.dairy.security;

import com.ghunsai.dairy.entity.User;
import com.ghunsai.dairy.repository.UserRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Turns a verified JWT into an Authentication. The user is re-read from the database on every
 * request, so deactivating a worker, changing a role or a password takes effect immediately
 * (token version check) instead of waiting for the token to expire.
 */
@Component
public class JwtAuthConverter {

    private final UserRepository userRepository;

    public JwtAuthConverter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UsernamePasswordAuthenticationToken convert(Jwt jwt) {
        Long userId;
        try {
            userId = Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException ex) {
            throw new InvalidBearerTokenException("Invalid token subject");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidBearerTokenException("User no longer exists"));
        if (!user.isActive()) {
            throw new InvalidBearerTokenException("User is disabled");
        }

        Object tokenVersion = jwt.getClaim(JwtTokenService.CLAIM_VERSION);
        if (!(tokenVersion instanceof Number version) || version.intValue() != user.getTokenVersion()) {
            throw new InvalidBearerTokenException("Token has been revoked, please log in again");
        }

        AppUserPrincipal principal =
                new AppUserPrincipal(user.getId(), user.getUsername(), user.getFullName(), user.getRole());
        return new UsernamePasswordAuthenticationToken(
                principal, jwt.getTokenValue(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
    }
}
