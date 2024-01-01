package com.teamchallenge.bookti.controller;

import com.teamchallenge.bookti.dto.AppResponse;
import com.teamchallenge.bookti.dto.ErrorResponse;
import com.teamchallenge.bookti.dto.authorization.NewUserRegistrationRequest;
import com.teamchallenge.bookti.dto.authorization.TokenPair;
import com.teamchallenge.bookti.dto.authorization.UserLoginRequest;
import com.teamchallenge.bookti.dto.authorization.UserTokenPair;
import com.teamchallenge.bookti.exception.PasswordIsNotMatchesException;
import com.teamchallenge.bookti.exception.RefreshTokenAlreadyRevokedException;
import com.teamchallenge.bookti.exception.UserAlreadyExistsException;
import com.teamchallenge.bookti.security.jwt.TokenManager;
import com.teamchallenge.bookti.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.MessageFormat;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Tag(name = "Authorization mappings", description = "PERMIT_ALL")
@RequestMapping("/api/v1/authorize")
@RestController
public class AuthController {

    private final UserService userService;
    private final TokenManager tokenManager;
    private final AuthenticationManager authenticationManager;
    @Qualifier("jwtRefreshTokenAuthenticationProvider")
    private final JwtAuthenticationProvider refreshTokenProvider;

    public AuthController(UserService userService,
                          TokenManager tokenManager,
                          AuthenticationManager authenticationManager,
                          JwtAuthenticationProvider refreshTokenProvider) {
        this.userService = userService;
        this.tokenManager = tokenManager;
        this.authenticationManager = authenticationManager;
        this.refreshTokenProvider = refreshTokenProvider;
    }

    @Operation(
            summary = "User signup",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "User created successfully",
                            content = {
                                    @Content(
                                            mediaType = APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = TokenPair.class)
                                    )
                            }
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad request or Validation failed",
                            content = {
                                    @Content(
                                            mediaType = APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = ErrorResponse.class))
                            }
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "User already exists",
                            content = {
                                    @Content(
                                            mediaType = APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = ErrorResponse.class))
                            }
                    )
            }
    )
    @PostMapping(path = "/signup")
    public ResponseEntity<TokenPair> signup(@Valid @RequestBody NewUserRegistrationRequest newUserRegistrationRequest) throws UserAlreadyExistsException, PasswordIsNotMatchesException {
        var createdUser = userService.create(newUserRegistrationRequest);
        Authentication authentication = UsernamePasswordAuthenticationToken
                .authenticated(createdUser, createdUser.getPassword(), createdUser.getAuthorities());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(tokenManager.generateTokenPair(authentication));
    }

    @Operation(
            summary = "User login",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Login successful",
                            content = {
                                    @Content(
                                            mediaType = APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = TokenPair.class)
                                    )
                            }
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Invalid user credentials",
                            content = {
                                    @Content(
                                            mediaType = APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = ErrorResponse.class)
                                    )
                            }
                    )
            }
    )
    @PostMapping(path = "/login")
    public ResponseEntity<TokenPair> login(@Valid @RequestBody UserLoginRequest userCredentials) throws BadCredentialsException {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(userCredentials.getEmail(), userCredentials.getPassword())
        );
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(tokenManager.generateTokenPair(authentication));
    }

    @Operation(
            summary = "Get new access JWT",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "New access and refresh JWT pair generated",
                            content = {
                                    @Content(
                                            mediaType = APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = TokenPair.class)
                                    )
                            }
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Provided refresh JWT is <Invalid> or <Expired>",
                            content = {
                                    @Content(
                                            mediaType = APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = ErrorResponse.class)
                                    )
                            }
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "Provided refresh JWT is <Revoked>",
                            content = {
                                    @Content(
                                            mediaType = APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = ErrorResponse.class)
                                    )
                            }
                    )
            }
    )
    @PostMapping("/token/refresh")
    public ResponseEntity<TokenPair> refreshToken(@RequestBody UserTokenPair refreshToken) throws BadCredentialsException, RefreshTokenAlreadyRevokedException {
        String token = refreshToken.getRefreshToken();
        Authentication authentication = refreshTokenProvider.authenticate(new BearerTokenAuthenticationToken(token));
        if (tokenManager.isRefreshTokenRevoked(token)) {
            throw new RefreshTokenAlreadyRevokedException(MessageFormat.format("Token <{0} is revoked>", token));
        }
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(tokenManager.generateTokenPair(authentication));
    }

    @Operation(
            summary = "Revoke current refresh JWT",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully revoke refresh JWT",
                            content = {
                                    @Content(
                                            mediaType = APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = AppResponse.class)
                                    )
                            }
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Provided refresh JWT is <Invalid> or <Expired>",
                            content = {
                                    @Content(
                                            mediaType = APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = ErrorResponse.class)
                                    )
                            }
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "JWT already revoked",
                            content = {
                                    @Content(
                                            mediaType = APPLICATION_JSON_VALUE,
                                            schema = @Schema(implementation = ErrorResponse.class))
                            }
                    )
            }
    )
    @PostMapping("/token/revoke")
    public ResponseEntity<AppResponse> revokeToken(@RequestBody UserTokenPair refreshToken) throws BadCredentialsException, RefreshTokenAlreadyRevokedException {
        String token = refreshToken.getRefreshToken();
        Authentication authentication = refreshTokenProvider.authenticate(new BearerTokenAuthenticationToken(token));
        tokenManager.revokeToken(authentication);
        var resp = new AppResponse(HttpStatus.OK.value(), String.format("Token <%s> successfully revoked", token));
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(resp);
    }
}

