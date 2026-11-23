package com.yuldashov.drive2.util;

public class PathUtils {
    public static final String USER_FOLDER_PREFIX = "user-";
    public static final String USER_FOLDER_SUFFIX = "-files";

    public static String getNameToFileOrDirectory(String path) {
        boolean isDir = path.endsWith("/");
        path = path.replaceAll("/$", "");
        int lastSlashIndex = path.lastIndexOf("/");
        String name = lastSlashIndex >=0 ? path.substring(lastSlashIndex + 1) : path;
        if (isDir) {
            return name + "/";
        }
        return name;
    }

    public static String getPathResponseToFile(String path) {
        int lastSlashIndex = path.lastIndexOf("/");
        if (lastSlashIndex >= 0) {
            return path.substring(0, lastSlashIndex + 1);
        }
        return "";
    }

    public static String getNameToDirectory(String path) {
        String p = path.replaceAll("/$", "");
        int lastSlashIndex = p.lastIndexOf("/");
        if (lastSlashIndex >= 0) {
            return p.substring(lastSlashIndex + 1);
        }
        return p;
    }

    public static String getPathResponseToDirectory(String path) {
        String p = path.replaceAll("/$", "");
        int lastSlashIndex = p.lastIndexOf("/");
        if (lastSlashIndex >= 0) {
            return p.substring(0, lastSlashIndex + 1);
        }
        return "";
    }

    public static String getNameForZip(String path) {
        boolean isDir = path.endsWith("/");
        path = path.replaceAll("/$", "");
        int lastSlashIndex = path.lastIndexOf("/");
        String name = lastSlashIndex >= 0 ? path.substring(lastSlashIndex + 1) : path;
        if (isDir) {
            return name + ".zip";
        }
        return name;
    }

    public static String buildPathUserFolder(int id) {
        return USER_FOLDER_PREFIX + id + USER_FOLDER_SUFFIX;
    }
}
