package com.skillnez.cloudstorage.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.Objects;

@RequiredArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public class StorageInfoResponseDto {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StorageInfoResponseDto that = (StorageInfoResponseDto) o;
        return Objects.equals(path, that.path) && Objects.equals(name, that.name) && Objects.equals(size, that.size) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, name, size, type);
    }

    private final String path;
    private final String name;
    private Long size;
    private final ResourceType type;

}
