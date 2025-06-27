package com.skillnez.cloudstorage.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@RequiredArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public class StorageInfoResponseDto {

    private final String path;
    private final String name;
    private Long size;
    private final ResourceType type;

}
