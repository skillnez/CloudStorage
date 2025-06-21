package com.skillnez.cloudstorage.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StorageInfoResponseDto {

    private final String path;
    private final String name;
    private Long size;
    private final ResourceType type;

}
