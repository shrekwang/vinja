package com.github.vinja.util;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;
import java.util.List;

public class SourceMappingUtil {

    public static final String patternStr = ".*//(( \\d+)+)$";

    public static class SourceMapping {

        private Map<Integer,Integer> sourceToByteMap = null;
        private Map<Integer,Integer> byteToSourceMap = null;

        public void setSourceToByteMap(Map<Integer,Integer> sourceToByteMap) {
            this.sourceToByteMap=sourceToByteMap;
        }
        public Map<Integer,Integer> getSourceToByteMap() {
            return this.sourceToByteMap;
        }


        public void setByteToSourceMap(Map<Integer,Integer> byteToSourceMap) {
            this.byteToSourceMap=byteToSourceMap;
        }
        public Map<Integer,Integer> getByteToSourceMap() {
            return this.byteToSourceMap;
        }
    }

    public static SourceMapping getSourceMapping(List<String> lines) {
        Pattern pattern = Pattern.compile(patternStr);

        Map<Integer,Integer> sourceToByteMap = new HashMap<Integer,Integer>();
        Map<Integer,Integer> byteToSourceMap = new HashMap<Integer,Integer>();

        for (int i=0; i<lines.size(); i++) {
            String line = lines.get(i);
            Matcher matcher = pattern.matcher(line);

            while (matcher.find()) {
                String lineStr = matcher.group(1);
                String [] nums =  lineStr.trim().split(" ");
                for (String numberStr : nums) {
                    Integer byteLine = Integer.parseInt(numberStr.trim());
                    sourceToByteMap.putIfAbsent(i, byteLine);
                    byteToSourceMap.put(byteLine, i);
                }
            }
        }

        SourceMapping  sourceMapping = new SourceMapping();
        sourceMapping.setSourceToByteMap(sourceToByteMap);
        sourceMapping.setByteToSourceMap(byteToSourceMap);
        return sourceMapping;
    }
    
}
