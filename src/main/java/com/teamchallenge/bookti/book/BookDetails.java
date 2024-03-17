package com.teamchallenge.bookti.book;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Year;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * DTO that represents book details.
 *
 * @author MinoUni
 * @version 1.0
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class BookDetails {

  private UUID id;

  private String title;

  private String author;

  private String genre;

  @JsonProperty("publication_date")
  private Year publicationDate;

  private String language;

  @JsonProperty("trade_format")
  private String tradeFormat;

  @JsonProperty("image_url")
  private String imageUrl;

  @JsonProperty("user_id")
  private UUID userId;

  private String description;

  /**
   * Constructor to include only book-specific information.
   *
   * @param id book identifier
   * @param title title
   * @param author author
   * @param genre genre
   * @param publicationDate publication date
   * @param language language
   * @param tradeFormat trade format
   * @param imageUrl image url
   * @param description description
   */
  public BookDetails(
      UUID id,
      String title,
      String author,
      String genre,
      Year publicationDate,
      String language,
      String tradeFormat,
      String imageUrl,
      String description) {
    this.id = id;
    this.title = title;
    this.author = author;
    this.genre = genre;
    this.publicationDate = publicationDate;
    this.language = language;
    this.tradeFormat = tradeFormat;
    this.imageUrl = imageUrl;
    this.description = description;
  }

  /**
   * Method to map book entity into book DTO.
   *
   * @param book entity
   * @return book DTO
   */
  public static BookDetails build(Book book) {
    return BookDetails.builder()
        .id(book.getId())
        .title(book.getTitle())
        .author(book.getAuthor())
        .genre(book.getGenre())
        .publicationDate(book.getPublicationDate())
        .language(book.getLanguage())
        .tradeFormat(book.getTradeFormat())
        .imageUrl(book.getImageUrl())
        .userId(book.getOwner().getId())
        .description(book.getDescription())
        .build();
  }
}
