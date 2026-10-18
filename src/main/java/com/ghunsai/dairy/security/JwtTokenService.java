package com.ghunsai.dairy.security;

import com.ghunsai.dairy.config.AppProperties;
import com.ghunsai.dairy.entity.User;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/** Issues signed (HS256) access tokens. Verification happens in the resource-server filter. */
@Component
public class JwtTokenService {

    public static final String CLAIM_VERSION = "tv";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_USERNAME = "username";

    public record IssuedToken(String value, long expiresInSeconds) {
    }

    private final JwtEncoder encoder;
    private final AppProperties.Jwt props;

    public JwtTokenService(JwtEncoder encoder, AppProperties props) {
        this.encoder = encoder;
        this.props = props.jwt();
    }

    public IssuedToken issue(User user) {
        Instant now = Instant.now();
        Duration ttl = Duration.ofMinutes(props.expirationMinutes());
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(props.issuer())
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .subject(String.valueOf(user.getId()))
                .claim(CLAIM_USERNAME, user.getUsername())
                .claim(CLAIM_ROLE, user.getRole().name())
                .claim(CLAIM_VERSION, user.getTokenVersion())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(token, ttl.toSeconds());
    }
}
