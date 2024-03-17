package com.teamchallenge.bookti.utils;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.Year;

/**
 * Utility class that convert {@link Year} into {@link Short} to store value into database.
 *
 * @author MinoUni
 * @version 1.0
 */
@Converter(autoApply = true)
public class YearConverter implements AttributeConverter<Year, Short> {

  @Override
  public Short convertToDatabaseColumn(Year attribute) {
    if (attribute != null) {
      return (short) attribute.getValue();
    }
    return null;
  }

  @Override
  public Year convertToEntityAttribute(Short dbData) {
    if (dbData != null) {
      return Year.of(dbData);
    }
    return null;
  }
}
