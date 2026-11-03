package com.yuldashov.drive2.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ResponseResource {
    private String path;
    private String name;
    private Long size;
    private ResourceType type;


    public ResponseResource(String path, String name, Long size) {
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
