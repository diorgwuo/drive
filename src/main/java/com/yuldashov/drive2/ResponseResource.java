package com.yuldashov.drive2;

public class ResponseResource {
    private String path;
    private String name;
    private Long size;
    private String type;

    public ResponseResource(String path, String name, Long size) {
        this.path = path;
        this.name = name;
        this.size = size;
        this.type = "FILE";
    }

    public ResponseResource(String path, String name) {
        this.path = path;
        this.name = name;
        this.type = "DIRECTORY";
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
