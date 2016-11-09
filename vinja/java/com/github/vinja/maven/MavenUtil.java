package com.github.vinja.maven;

import java.io.File;

public class MavenUtil {

    public static boolean underParentPom(String path) {
        File file = new File(path);
        if (!file.exists()) return false;

        while (true ) {
            file = file.getParentFile();
            if (file == null ) break;
            if (new File(file, "pom.xml").exists()
                    && new File(file.getParentFile(), "pom.xml").exists()) {
                return true;
            }
        }
        return false;
    }

}
