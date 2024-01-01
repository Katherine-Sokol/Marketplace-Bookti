package com.teamchallenge.bookti.dto.authorization;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class UserTokenPair {

    @JsonProperty("user_id")
    @NotBlank(message = "Property <user_id> can't be blank")
    private String userId;

    @JsonProperty("refresh_token")
    @NotBlank(message = "Property <refresh_token> can't be blank")
    private String refreshToken;
}
