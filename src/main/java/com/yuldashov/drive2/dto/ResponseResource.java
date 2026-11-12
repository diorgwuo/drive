package com.yuldashov.drive2.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
public class ResponseResource {
    private String path;
    private String name;
    private long size;
    private ResourceType type;

    public ResponseResource(String path, String name, long size) {
        this.path = path;
        this.name = name;
        this.size = size;
        this.type = ResourceType.FILE;
    }

    public ResponseResource(String path, String name) {
        this.path = path;
        this.name = name;
        this.type = ResourceType.DIRECTORY;
    }
}
