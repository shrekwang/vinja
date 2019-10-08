package com.github.vinja.omni;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.github.vinja.compiler.CompilerContext;

public class ReactorProjectManager {

    private static Map<String,ClassMetaInfoManager> metaInfoMap = new ConcurrentHashMap<String,ClassMetaInfoManager>();

    public static ClassMetaInfoManager initClassMetaInfoManager(String reactorRoot, CompilerContext ctx) {

        ClassMetaInfoManager metaInfo = metaInfoMap.get(reactorRoot);
        if (metaInfo == null ) {
            metaInfo = new ClassMetaInfoManager(ctx);
            metaInfoMap.put(reactorRoot, metaInfo);
        } else {
        	metaInfo.addCompilerContext(ctx);
        }
        return metaInfo;
    }


}
