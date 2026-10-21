package com.yuldashov.drive2.util;

public class PathUtils {
    public static String getNameToFileOrDirectory(String path) {
        String name;
        boolean isDir = path.endsWith("/");
        path = path.replaceAll("/$", "");
        int slash = path.lastIndexOf("/");
        if (slash >= 0) {
            name = path.substring(slash + 1);
        } else {
            name = path;
        }
        if (isDir) {
            return name + "/";
        }
        return name;
    }

    public static String getPathResponseToFile(String path) {
        int slash = path.lastIndexOf("/");
        if (slash >= 0) {
            return path.substring(0, slash + 1);
        }
        return "";
    }

    public static String getNameToDirectory(String path) {
        String p = path.replaceAll("/$", "");
        int slash = p.lastIndexOf("/");
        if (slash >= 0) {
            return p.substring(slash + 1);
        }
        return p;
    }

    public static String getPathResponseToDirectory(String path) {
        String p = path.replaceAll("/$", "");
        int slash = p.lastIndexOf("/");
        if (slash >= 0) {
            return p.substring(0, slash + 1);
        }
        return "";
    }

    public static String getNameForZip(String path) {
        String name;
        boolean isDir = path.endsWith("/");
        path = path.replaceAll("/$", "");
        int slash = path.lastIndexOf("/");
        if (slash >= 0) {
            name = path.substring(slash + 1);
        } else {
            name = path;
        }
        if (isDir) {
            return name + ".zip";
        }
        return name;
    }
}
