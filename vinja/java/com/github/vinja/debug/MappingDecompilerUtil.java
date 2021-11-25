package com.github.vinja.debug;

import com.github.vinja.util.DecompileUtil;
import com.github.vinja.util.Pair;
import com.github.vinja.util.VjdeUtil;
import com.sun.jdi.Location;
import com.sun.jdi.ReferenceType;
import org.apache.commons.io.FilenameUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;
import java.util.NavigableMap;

public class MappingDecompilerUtil {

    public static void main(String[] args) {

        String tmpSrcPath = FilenameUtils.concat(VjdeUtil.getToolDataHome(), "tmp_src");
        File tmpSrcDir = new File(tmpSrcPath);
        if (!tmpSrcDir.exists()) {
            tmpSrcDir.mkdirs();
        }

    }

    public static Pair<String, Integer> decompileWithMappedLine(Location loc, int lineNum,String defaultPath) {

        String tmpSrcPath = FilenameUtils.concat(VjdeUtil.getToolDataHome(), "tmp_src");
        File tmpSrcDir = new File(tmpSrcPath);
        if (!tmpSrcDir.exists()) {
            tmpSrcDir.mkdir();
        }

        ReferenceType refType = loc.declaringType();
        String className = refType.name();
        String abPath = defaultPath;

        try (Socket socket = new Socket("localhost", 10237);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        ){
            out.println(loc.declaringType().name());
            String result = in.readLine();

            if (result !=null && !result.trim().equals("")) {
                Pair<String, NavigableMap<Integer, Integer>> pair = DecompileUtil.decompile(result);
                String code = pair.getFirst();
                NavigableMap<Integer, Integer> lineMap = pair.getSecond();
                for (Map.Entry<Integer,Integer> entry : lineMap.entrySet()) {
                    if (entry.getValue() !=null && entry.getValue().equals(lineNum)) {
                        lineNum = entry.getKey();
                        break;
                    }
                }
                String lastName = className.indexOf(".") > -1 ? className.substring(className.lastIndexOf(".")+1) : className;
                File tempFile = new File(tmpSrcPath, lastName + ".java");
                BufferedWriter br = new BufferedWriter(new FileWriter(tempFile));
                br.write(code);
                br.close();
                abPath = tempFile.getPath();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Pair.make(abPath, lineNum);
    }
}
