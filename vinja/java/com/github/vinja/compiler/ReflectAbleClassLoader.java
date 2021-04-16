package com.github.vinja.compiler;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.eclipse.jdt.internal.compiler.classfmt.ClassFileReader;
import org.eclipse.jdt.internal.compiler.env.NameEnvironmentAnswer;

import javax.swing.text.html.Option;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class ReflectAbleClassLoader extends URLClassLoader {

    private static Map<String,LoadingCache<String, Optional<Object>>> allCache = new HashMap<>();

    private void initCache(String projectRoot) {
        LoadingCache<String, Optional<Object>> cache = allCache.get(projectRoot);

        if (cache == null) {
            cache = CacheBuilder.newBuilder()
                    .initialCapacity(5000)
                    .maximumSize(20000)
                    .softValues()
                    .build(new CacheLoader<String, Optional<Object>>() {
                        @Override
                        public Optional<Object> load(String resourceName) throws Exception {
                            try (InputStream is = getResourceAsStream(resourceName)) {
                                if (is != null) {
                                    byte[] classBytes;
                                    byte[] buf = new byte[8192];
                                    ByteArrayOutputStream baos = new ByteArrayOutputStream(buf.length);
                                    int count;
                                    while ((count = is.read(buf, 0, buf.length)) > 0) {
                                        baos.write(buf, 0, count);
                                    }
                                    baos.flush();
                                    classBytes = baos.toByteArray();
                                    return Optional.of(classBytes);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            return Optional.empty();
                        }
                    });
            allCache.put(projectRoot, cache);
        }
    }


    public ReflectAbleClassLoader(String projectRoot, URL[] urls, ClassLoader parent) {
        super(urls, parent);
        initCache(projectRoot);
    }


    public Package[] getPackageInfo() {
        return this.getPackages();
    }

    public void clearResourceByteCache(String projectRoot, String resourceName) {
        LoadingCache<String, Optional<Object>> cache = allCache.get(projectRoot);
        if (cache != null) {
            cache.invalidate(resourceName);
        }
    }

    public byte[] getResourceByte(String projectRoot,String resourceName){
        try {
            LoadingCache<String, Optional<Object>> cache = allCache.get(projectRoot);
            Optional<Object> obj = cache.get(resourceName);
            if (obj.isPresent()) {
                return (byte[])obj.get();
            }
            return null;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return null;
        }
    }


}
