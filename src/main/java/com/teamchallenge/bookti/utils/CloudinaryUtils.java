package com.teamchallenge.bookti.utils;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.teamchallenge.bookti.exception.CloudinaryException;
import com.teamchallenge.bookti.exception.UnsupportedFileTypeException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service to connect with cloudinary workspace to manage files.
 *
 * @author MinoUni
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CloudinaryUtils {

  public static final String BOOKS_FOLDER_NAME = "/books";
  public static final String USERS_FOLDER_NAME = "/users";
  private final Cloudinary cloudinary;

  /**
   * Method to upload files to cloudinary.
   *
   * @param file file that will be stored
   * @param fileName new file name
   * @param folderName cloudinary folder where to store file
   * @return file url
   */
  public String uploadFile(MultipartFile file, String fileName, String folderName) {
    if (!checkFileType(file)) {
      throw new UnsupportedFileTypeException("File type unsupported");
    }
    try {
      return cloudinary
          .uploader()
          .upload(
              file.getBytes(),
              ObjectUtils.asMap("public_id", fileName, "overwrite", true, "folder", folderName))
          .get("url")
          .toString();
    } catch (IOException e) {
      log.error("Failed to upload file: {}", file.getName(), e);
      throw new CloudinaryException("Failed to upload file");
    }
  }

  /**
   * Method to delete files from cloudinary.
   *
   * @param fileName file name that will be deleted
   * @return true if deleted successfully
   */
  public boolean deleteFile(String fileName) {
    try {
      var result = cloudinary.uploader().destroy(fileName, ObjectUtils.emptyMap());
      if (result.containsKey("result") && result.get("result").equals("ok")) {
        return true;
      }
    } catch (IOException e) {
      log.error("Failed to delete a file: {}", fileName, e);
      throw new CloudinaryException("Failed to delete file");
    }
    return false;
  }

  /**
   * Method to validate file type.
   *
   * @param file file to save
   * @return true if file type .jpg/.jpeg or .png
   */
  private boolean checkFileType(MultipartFile file) {
    Map<String, List<Byte>> signatures =
        Map.of(
            "*.jpeg, *.jpg", List.of((byte) 0xFF, (byte) 0xD8),
            "*.png",
                List.of(
                    (byte) 0x89,
                    (byte) 0x50,
                    (byte) 0x4E,
                    (byte) 0x47,
                    (byte) 0x0D,
                    (byte) 0x0A,
                    (byte) 0x1A,
                    (byte) 0x0A));
    try {
      byte[] bytes = Arrays.copyOfRange(file.getBytes(), 0, 8);
      if (signatures.values().stream()
          .anyMatch(
              signature ->
                  IntStream.range(0, signature.size())
                      .allMatch(i -> signature.get(i).equals(bytes[i])))) {
        return true;
      }
    } catch (IOException e) {
      log.error("Failed to read a file {}", file.getName(), e);
    }
    return false;
  }
}
