package com.teamchallenge.bookti.security.jwt;

import com.teamchallenge.bookti.dto.authorization.TokenPair;
import com.teamchallenge.bookti.exception.RefreshTokenAlreadyRevokedException;
import com.teamchallenge.bookti.security.AuthorizedUser;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class TokenManager {

    @Getter
    private final Map<String, UUID> revokedTokens = new ConcurrentHashMap<>();
    private final JwtEncoder accessTokenEncoder;
    @Qualifier("jwtRefreshTokenEncoder")
    private final JwtEncoder refreshTokenEncoder;
    private final String issuer;

    public TokenManager(JwtEncoder accessTokenEncoder,
                        JwtEncoder refreshTokenEncoder,
                        @Value("${spring.application.name}") String issuer) {
        this.accessTokenEncoder = accessTokenEncoder;
        this.refreshTokenEncoder = refreshTokenEncoder;
        this.issuer = issuer;
    }

    public TokenPair generateTokenPair(Authentication authentication) throws BadCredentialsException {
        var principal = authentication.getPrincipal();
        if (!(principal instanceof AuthorizedUser user)) {
            throw new BadCredentialsException(
                    MessageFormat.format("Principal <{0}> is not of <{1}> type",
                            principal.getClass(),
                            AuthorizedUser.class
                    )
            );
        }
        return TokenPair
                .builder()
                .userId(String.valueOf(user.getId()))
                .accessToken(generateAccessToken(authentication))
                .refreshToken(validateRefreshToken(authentication))
                .build();
    }

    public void revokeToken(Authentication authentication) throws BadCredentialsException, RefreshTokenAlreadyRevokedException {
        var credentials = authentication.getCredentials();
        if (!(credentials instanceof Jwt jwt)) {
            throw new BadCredentialsException(
                    MessageFormat.format("Credentials <{0}> is not of <{1}> type",
                            credentials.getClass(),
                            Jwt.class
                    )
            );
        }
        String token = jwt.getTokenValue();
        if (isRefreshTokenRevoked(token)) {
            throw new RefreshTokenAlreadyRevokedException(MessageFormat.format("Token <{0}> is already revoked", token));
        }
        revokedTokens.put(token, UUID.fromString(jwt.getSubject()));
    }

    private String validateRefreshToken(Authentication authentication) {
        if (authentication.getCredentials() instanceof Jwt jwt) {
            Instant now = Instant.now();
            Instant expiresAt = jwt.getExpiresAt();
            if (Duration.between(now, expiresAt).toDays() > 4) {
                return jwt.getTokenValue();
            }
            revokedTokens.put(jwt.getTokenValue(), UUID.fromString(jwt.getSubject()));
        }
        return generateRefreshToken(authentication);
    }

    public boolean isRefreshTokenRevoked(String token) {
        return revokedTokens.containsKey(token);
    }

    private String generateAccessToken(Authentication authentication) {
        AuthorizedUser user = (AuthorizedUser) authentication.getPrincipal();
        Instant now = Instant.now();
        // TODO: Include user scopes, roles, not include sensitive info
        String scope = user
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(", "));
        JwtClaimsSet claimsSet = JwtClaimsSet
                .builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(now.plus(30, ChronoUnit.MINUTES))
                .subject(user.getId().toString())
                .claim("scope", scope)
                .build();
        return accessTokenEncoder
                .encode(JwtEncoderParameters.from(claimsSet))
                .getTokenValue();
    }

    private String generateRefreshToken(Authentication authentication) {
        AuthorizedUser user = (AuthorizedUser) authentication.getPrincipal();
        Instant now = Instant.now();
        JwtClaimsSet claimsSet = JwtClaimsSet
                .builder()
                .issuer(issuer)
                .issuedAt(now)
                .expiresAt(now.plus(7, ChronoUnit.DAYS))
                .subject(user.getId().toString())
                .build();
        return refreshTokenEncoder
                .encode(JwtEncoderParameters.from(claimsSet))
                .getTokenValue();
    }
}
