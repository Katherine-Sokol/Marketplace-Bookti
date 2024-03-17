package com.teamchallenge.bookti.utils;

import static org.mapstruct.InjectionStrategy.CONSTRUCTOR;
import static org.mapstruct.NullValuePropertyMappingStrategy.IGNORE;

import com.teamchallenge.bookti.book.Book;
import com.teamchallenge.bookti.book.BookUpdateReq;
import com.teamchallenge.bookti.user.UserEntity;
import com.teamchallenge.bookti.user.UserUpdateReq;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * Mapper to map objects.
 *
 * @author MinoUni
 * @version 1.0
 */
@Mapper(
    componentModel = "spring",
    injectionStrategy = CONSTRUCTOR,
    nullValuePropertyMappingStrategy = IGNORE)
public interface MapperUtils {

  /**
   * The method to map {@link BookUpdateReq} into {@link Book}.
   *
   * @param bookUpdate new info about book to save
   * @param book book model
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "imageName", ignore = true)
  @Mapping(target = "imageUrl", ignore = true)
  @Mapping(target = "owner", ignore = true)
  void mapBookUpdateToBook(BookUpdateReq bookUpdate, @MappingTarget Book book);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "avatarName", ignore = true)
  @Mapping(target = "avatarUrl", ignore = true)
  @Mapping(target = "books", ignore = true)
  void mapUserUpdateToUser(UserUpdateReq userUpdate, @MappingTarget UserEntity user);
}
