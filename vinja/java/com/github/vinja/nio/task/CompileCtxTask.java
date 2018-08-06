package com.github.vinja.nio.task;

import com.alibaba.fastjson.JSONObject;
import com.github.vinja.compiler.CompilerContext;
import com.github.vinja.compiler.CompilerContextManager;
import com.github.vinja.parser.VinjaJavaSourceSearcher;
import com.github.vinja.util.JdeLogger;

import io.netty.channel.ChannelHandlerContext;

public class CompileCtxTask implements Runnable {

    private ChannelHandlerContext ctx;
    private JSONObject jsonObj;

    public CompileCtxTask(ChannelHandlerContext ctx, JSONObject jsonObj) {
        this.ctx = ctx;
        this.jsonObj = jsonObj;
    }

    public void run() {
		String classPathXml = jsonObj.getString("classPathXml");
		if (classPathXml == null || classPathXml.trim().equals("")) return;

    	try {
			CompilerContextManager ccm = CompilerContextManager.getInstnace();
			ccm.reloadCompilerContext(classPathXml, false);
			CompilerContext cc = ccm.getCompilerContext(classPathXml);
			cc.cacheClassInfo();
			VinjaJavaSourceSearcher.clean();
    	} catch (Exception e) {
			JdeLogger.getLogger("VinjaMsgHandler").warn(e.getMessage());
    	}

        String msg = "clean vinja project : " + classPathXml + " ok\n";
        ctx.writeAndFlush(msg);
        ctx.channel().close();
    }

}
