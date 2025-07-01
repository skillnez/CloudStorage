package com.skillnez.cloudstorage;

import com.skillnez.cloudstorage.dto.ResourceType;
import com.skillnez.cloudstorage.dto.StorageInfoResponseDto;
import com.skillnez.cloudstorage.exception.BadPathFormatException;
import com.skillnez.cloudstorage.utils.PathUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.*;

public class PathUtilTest {

    private static final String USER_PATH_PREFIX = "user-1-files/";
    private static final String NORMAL_DIRECTORY_PATH = "user-1-files/test/";
    private static final String NORMAL_FILE_PATH = "user-1-files/test";
    private static final Long DEFAULT_USER_ID = 1L;
    private static final String MALFORMED_USER_PATH = " //  / /";
    private static final String USER_PATH_W_RESTRICTED_CHARS = "user-1-files/test?/";
    private static final String PATH_TRAVERSAL_ATTEMPT= "user-1-files/../";
    private static final String NORMAL_FILENAME = "test.txt";
    private static final String EMPTY_STRING = "";

    @Test
    void testFormatPathForBackend_root() {
        Assertions.assertEquals(USER_PATH_PREFIX, PathUtils.formatPathForBackend("", DEFAULT_USER_ID));
    }

    @Test
    void testFormatPathForBackend_malformedPath() {
        Assertions.assertEquals(USER_PATH_PREFIX, PathUtils.formatPathForBackend(MALFORMED_USER_PATH, DEFAULT_USER_ID));
    }

    @Test
    void testFormatPathForBackend_normalDirectory() {
        Assertions.assertEquals(NORMAL_DIRECTORY_PATH, PathUtils.formatPathForBackend("/test  //", DEFAULT_USER_ID));
    }

    @Test
    void testFormatPathForBackend_normalFile() {
        Assertions.assertEquals(NORMAL_FILE_PATH, PathUtils.formatPathForBackend("/  //test  ", DEFAULT_USER_ID));
    }

    @Test
    void testFormatPathForBackend_restrictedChars() {
        assertThrows(BadPathFormatException.class,
                () -> PathUtils.formatPathForBackend(USER_PATH_W_RESTRICTED_CHARS, DEFAULT_USER_ID));
    }

    @Test
    public void testFormatPathForUpload() {
        ///В данных тестах не проверяется валидность пути, потому что все то же самое проверено в тестах выше
        assertThrows(BadPathFormatException.class, () -> PathUtils.formatPathForUpload(EMPTY_STRING, NORMAL_DIRECTORY_PATH));
        assertThrows(BadPathFormatException.class, () -> PathUtils.formatPathForUpload(NORMAL_DIRECTORY_PATH, EMPTY_STRING));
        assertThrows(BadPathFormatException.class, () -> PathUtils.formatPathForUpload(NORMAL_FILE_PATH, NORMAL_FILENAME));
    }

    @Test
    void testFormStorageInfoResponseDto_file() {
        String path = "user-8-files/docs/test.txt";
        Long fileSize = 123L;
        StorageInfoResponseDto dto = PathUtils.formStorageInfoResponseDto(path, fileSize);

        Assertions.assertEquals("docs/", dto.getPath());
        Assertions.assertEquals("test.txt", dto.getName());
        Assertions.assertEquals(fileSize, dto.getSize());
        Assertions.assertEquals(ResourceType.FILE, dto.getType());
    }

    @Test
    void testFormStorageInfoResponseDto_folder() {
        String path = "user-8-files/docs/";
        Long fileSize = 123L;
        StorageInfoResponseDto dto = PathUtils.formStorageInfoResponseDto(path, fileSize);

        Assertions.assertEquals("", dto.getPath());
        Assertions.assertEquals("docs/", dto.getName());
        Assertions.assertNull(dto.getSize());
        Assertions.assertEquals(ResourceType.DIRECTORY, dto.getType());
    }

    @Test
    void testFormStorageInfoResponseDto_equals() {
        String path = "user-8-files/docs/test.txt";
        Long fileSize = 123L;
        StorageInfoResponseDto expected = new StorageInfoResponseDto("docs/", "test.txt", fileSize, ResourceType.FILE);
        StorageInfoResponseDto actual = PathUtils.formStorageInfoResponseDto(path, fileSize);

        Assertions.assertEquals(expected, actual);
    }

    @Test
    void testFormStorageInfoResponseDto_invalid() {
        assertThrows(BadPathFormatException.class,
                () -> PathUtils.formStorageInfoResponseDto("", 10L));
    }

}
