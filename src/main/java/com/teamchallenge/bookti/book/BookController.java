package com.teamchallenge.bookti.book;

import static com.teamchallenge.bookti.config.SwaggerConfig.BOOK_PROFILE_SCHEMA;
import static com.teamchallenge.bookti.config.SwaggerConfig.BOOK_UPDATE_REQ_SCHEMA;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

import com.teamchallenge.bookti.dto.AppResponse;
import com.teamchallenge.bookti.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller to manage users books.
 *
 * @author MinoUni
 * @version 1.0
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Books endpoints")
@RequestMapping("/api/v1/books")
class BookController {

  private final BookService bookService;

  /**
   * The mapping to find all created books.
   *
   * @param pageable books list as pageable obj
   * @return sliced list of books
   */
  @Operation(
      summary = "Find all books",
      description = "Get all books sliced on pages",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Books found",
            content = {
              @Content(
                  mediaType = APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = BookDetails.class))
            }),
        @ApiResponse(
            responseCode = "500",
            description = "Unexpected API error",
            content = {
              @Content(
                  mediaType = APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = ErrorResponse.class))
            })
      })
  @GetMapping
  public ResponseEntity<Page<BookDetails>> findAll(final Pageable pageable) {
    return ResponseEntity.ok(bookService.findAll(pageable));
  }

  /**
   * The mapping to add a new book to a user book list.
   *
   * @param bookProfile DTO with book details
   * @return DTO of create book
   */
  @Operation(
      summary = "Create a new book",
      description = "Add a new book to a user list",
      responses = {
        @ApiResponse(
            responseCode = "201",
            description = "Book created",
            content = {
              @Content(
                  mediaType = APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = BookDetails.class))
            }),
        @ApiResponse(
            responseCode = "401",
            description = "UNAUTHORIZED",
            content = {
              @Content(
                  mediaType = APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = ErrorResponse.class))
            }),
        @ApiResponse(
            responseCode = "403",
            description = "ACCESS_DENIED",
            content = {
              @Content(
                  mediaType = APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = ErrorResponse.class))
            }),
        @ApiResponse(
            responseCode = "500",
            description = "Unexpected API error",
            content = {
              @Content(
                  mediaType = APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = ErrorResponse.class))
            })
      })
  @PostMapping(consumes = MULTIPART_FORM_DATA_VALUE, produces = APPLICATION_JSON_VALUE)
  @PreAuthorize("isAuthenticated() and authentication.principal.id == #userId")
  public ResponseEntity<BookDetails> create(
      @RequestParam("user_id") final UUID userId,
      @Parameter(description = BOOK_PROFILE_SCHEMA, required = true)
          @Valid
          @RequestPart("book_profile")
          final BookProfile bookProfile,
      @RequestPart(required = false) final MultipartFile image) {
    return ResponseEntity.status(CREATED).body(bookService.create(bookProfile, image, userId));
  }

  /**
   * The mapping to find book by id.
   *
   * @param id book identifier
   * @return book DTO
   */
  @Operation(
      summary = "Find a book",
      description = "Find book information by id",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Book found",
            content = {
              @Content(
                  mediaType = APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = BookDetails.class))
            }),
        @ApiResponse(
            responseCode = "404",
            description = "Book not found",
            content = {
              @Content(
                  mediaType = APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = ErrorResponse.class))
            }),
        @ApiResponse(
            responseCode = "500",
            description = "Unexpected API error",
            content = {
              @Content(
                  mediaType = APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = ErrorResponse.class))
            })
      })
  @GetMapping("/{id}")
  public ResponseEntity<BookDetails> findById(@PathVariable UUID id) {
    return ResponseEntity.ok(bookService.findById(id));
  }

  /**
   * Delete book from user's(owner's) collection by id.
   *
   * @param userId book owner identifier
   * @param id book identifier
   * @return message that operation was successful
   */
  @Operation(
      summary = "Delete a book",
      description = "Delete book from user's(owner's) collection by id",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Book deleted",
            content = {
              @Content(
                  mediaType = APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = BookDetails.class))
            }),
        @ApiResponse(
            responseCode = "404",
            description = "Book not found",
            content = {
              @Content(
                  mediaType = APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = ErrorResponse.class))
            }),
        @ApiResponse(
            responseCode = "500",
            description = "Unexpected API error",
            content = {
              @Content(
                  mediaType = APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = ErrorResponse.class))
            })
      })
  @DeleteMapping("/{id}")
  @PreAuthorize("isAuthenticated() and authentication.principal.id == #userId")
  public ResponseEntity<AppResponse> deleteById(
      @RequestParam("user_id") UUID userId, @PathVariable UUID id) {
    bookService.deleteById(id);
    var response = new AppResponse(OK.value(), String.format("Book <%s> deleted", id));
    return ResponseEntity.ok(response);
  }

  /**
   * Update book by id.
   *
   * @param userId book owner identifier
   * @param id book identifier
   * @return message that operation was successful
   */
  @Operation(
      summary = "Update a book",
      description = "Update book from user's(owner's) collection by id",
      responses = {
        @ApiResponse(
            responseCode = "200",
            description = "Book updated",
            content = {
              @Content(
                  mediaType = APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = AppResponse.class))
            }),
        @ApiResponse(
            responseCode = "404",
            description = "Book not found",
            content = {
              @Content(
                  mediaType = APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = ErrorResponse.class))
            }),
        @ApiResponse(
            responseCode = "500",
            description = "Unexpected API error",
            content = {
              @Content(
                  mediaType = APPLICATION_JSON_VALUE,
                  schema = @Schema(implementation = ErrorResponse.class))
            })
      })
  @PatchMapping(
      value = "/{id}",
      consumes = MULTIPART_FORM_DATA_VALUE,
      produces = APPLICATION_JSON_VALUE)
  @PreAuthorize("isAuthenticated() and authentication.principal.id == #userId")
  public ResponseEntity<BookDetails> updateById(
      @PathVariable UUID id,
      @RequestParam("user_id") UUID userId,
      @Parameter(description = BOOK_UPDATE_REQ_SCHEMA, required = true) @RequestPart("book") @Valid
          BookUpdateReq bookUpdateInfo,
      @RequestPart(value = "image", required = false) final MultipartFile imageFile) {
    return ResponseEntity.ok(bookService.updateById(id, bookUpdateInfo, imageFile));
  }
}
