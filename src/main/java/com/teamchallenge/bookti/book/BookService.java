package com.teamchallenge.bookti.book;

import static com.teamchallenge.bookti.utils.CloudinaryUtils.BOOKS_FOLDER_NAME;
import static org.springframework.transaction.annotation.Isolation.REPEATABLE_READ;

import com.teamchallenge.bookti.exception.BookNotFoundException;
import com.teamchallenge.bookti.exception.UserNotFoundException;
import com.teamchallenge.bookti.user.UserEntity;
import com.teamchallenge.bookti.user.UserRepository;
import com.teamchallenge.bookti.utils.CloudinaryUtils;
import com.teamchallenge.bookti.utils.MapperUtils;
import java.time.Year;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Book Service.
 *
 * @author MinoUni
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class BookService {

  private final BookRepository bookRepository;
  private final UserRepository userRepository;
  private final CloudinaryUtils cloudinaryUtils;
  private final MapperUtils mapperUtils;

  /**
   * The method to add a new book to a user book list.
   *
   * @param bookProfile DTO with book details
   * @param imageFile book avatar image
   * @param userId user identifier
   * @return DTO of create book
   */
  @Transactional
  public BookDetails create(BookProfile bookProfile, MultipartFile imageFile, UUID userId) {
    String imageUrl = null;
    String imageName = UUID.randomUUID().toString();
    if (imageFile != null && !imageFile.isEmpty()) {
      imageUrl = cloudinaryUtils.uploadFile(imageFile, imageName, BOOKS_FOLDER_NAME);
    }
    var user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found."));
    var book = build(bookProfile, imageUrl, imageName, user);
    book = bookRepository.save(book);
    return BookDetails.build(book);
  }

  /**
   * The method to find book by id.
   *
   * @param id book identifier
   * @return book DTO
   */
  public BookDetails findById(UUID id) {
    return bookRepository
        .findBookById(id)
        .orElseThrow(() -> new BookNotFoundException("Book not found."));
  }

  /**
   * The method to find all books and slice it.
   *
   * @param pageable pagination
   * @return page of books
   */
  public Page<BookDetails> findAll(Pageable pageable) {
    return bookRepository.findAllByPageable(pageable);
  }

  /**
   * The method to delete book by its identifier.
   *
   * @param id book identifier
   */
  @Transactional
  public void deleteById(UUID id) {
    if (!bookRepository.existsById(id)) {
      throw new BookNotFoundException("Book not found.");
    }
    bookRepository.deleteById(id);
  }

  /**
   * The method to update book information.
   *
   * @param id book identifier
   * @param bookUpdateInfo info to update
   * @return updated book details DTO
   */
  @Transactional(isolation = REPEATABLE_READ)
  public BookDetails updateById(UUID id, BookUpdateReq bookUpdateInfo, MultipartFile imageFile) {
    if (!bookRepository.existsById(id)) {
      throw new BookNotFoundException("Book not found.");
    }
    var book = bookRepository.getReferenceById(id);
    if (imageFile != null && !imageFile.isEmpty()) {
      var imageUrl = cloudinaryUtils.uploadFile(imageFile, book.getImageName(), BOOKS_FOLDER_NAME);
      book.setImageUrl(imageUrl);
    }
    mapperUtils.mapBookUpdateToBook(bookUpdateInfo, book);
    bookRepository.save(book);
    return findById(id);
  }

  /**
   * Method to build book entity from book DTO.
   *
   * @param bookProfile book DTO
   * @param imageUrl image url
   * @param user user entity
   * @return book entity
   */
  private Book build(BookProfile bookProfile, String imageUrl, String imageName, UserEntity user) {
    return Book.builder()
        .title(bookProfile.getTitle())
        .author(bookProfile.getAuthor())
        .genre(bookProfile.getGenre())
        .description(bookProfile.getDescription())
        .imageName(imageName)
        .imageUrl(imageUrl)
        .language(bookProfile.getLanguage())
        .publicationDate(Year.parse(bookProfile.getPublicationDate()))
        .tradeFormat(bookProfile.getTradeFormat())
        .owner(user)
        .build();
  }
}
