package com.teamchallenge.bookti.book;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * DTO that contains fields required to represent book at user profile page.
 *
 * @author MinoUni
 * @version 1.0
 */
@Data
@AllArgsConstructor
public class BookShortDetails {

  private UUID id;

  private String title;

  private String author;

  private String language;

  @JsonProperty("image_url")
  private String imageUrl;
}
