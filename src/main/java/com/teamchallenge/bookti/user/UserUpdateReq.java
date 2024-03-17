package com.teamchallenge.bookti.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * DTO with user data to update.
 *
 * @author MinoUni
 * @version 1.0
 */
@Data
@AllArgsConstructor
public class UserUpdateReq {

  @Schema(type = "string", example = "mark.javar@gmail.com")
  @NotBlank(message = "Field <email> must not be blank")
  @Pattern(
      regexp =
          "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)"
              + "*@[^-][A-Za-z0-9-](\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$",
      message = "Invalid email")
  private String email;

  @JsonProperty("full_name")
  @NotBlank(message = "Field <full_name> must not be blank")
  private String fullName;

  @NotBlank(message = "Field <location> must not be blank")
  private String location;

  @JsonProperty("telegram_id")
  @Size(min = 5, max = 32, message = "Must be from 5 to 32 symbols length")
  private String telegramId;
}
