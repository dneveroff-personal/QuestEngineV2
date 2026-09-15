package dn.questenginev2.common.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * ADR-0012: обёртка listing-эндпоинтов с метаданными пагинации. Своя DTO,
 * а не проброс {@link org.springframework.data.domain.Page} напрямую в
 * контракт API — публичный контракт не должен зависеть от конкретной
 * реализации пагинации Spring Data.
 */
public record PageResponse<T>(
    List<T> content, int page, int size, long totalElements, int totalPages) {

  /**
   * Строит ответ из уже смэпленной в DTO страницы — сервис сначала
   * трансформирует {@code Page<Entity>} в {@code Page<ResponseDto>} через
   * {@code Page.map(...)} (сохраняет метаданные пагинации), затем
   * оборачивает здесь.
   */
  public static <T> PageResponse<T> from(Page<T> page) {
    return new PageResponse<>(
        page.getContent(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages());
  }
}
