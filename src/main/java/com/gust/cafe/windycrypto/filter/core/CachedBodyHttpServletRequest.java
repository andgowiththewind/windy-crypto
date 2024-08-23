package com.gust.cafe.windycrypto.filter.core;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.*;

/**
 * 缓存请求体
 * <p>
 * 解决请求体只能读取一次的问题。原理：
 * 通过 `BufferedReader` 逐行读取请求体,并将其转换为字符串,最后再转换为字节数组进行缓存。
 * (1)`getRequestBody` 方法：使用 `BufferedReader` 读取整个请求体并将其转换为字符串。
 * (2)`getInputStream` 和 `getReader` 方法：允许重新读取缓存的请求体内容。
 * (3)方法糖：`getRequestBodyString` 方法：直接获取请求体字符串,即JSON字符串,可以在filter中直接使用。
 * </p>
 */
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    private byte[] cachedBody;

    public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);
        // 缓存请求体
        InputStream requestInputStream = request.getInputStream();
        this.cachedBody = getRequestBody(requestInputStream).getBytes("UTF-8");
    }

    private String getRequestBody(InputStream inputStream) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
        } finally {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
        }
        return stringBuilder.toString();
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return new CachedBodyServletInputStream(this.cachedBody);
    }

    @Override
    public BufferedReader getReader() throws IOException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(this.cachedBody);
        return new BufferedReader(new InputStreamReader(byteArrayInputStream));
    }

    // 提供一个API可以直接获取请求体字符串,即JSON字符串,可以在filter中直接使用
    public String getRequestBodyString() throws IOException {
        InputStream inputStream = new CachedBodyServletInputStream(this.cachedBody);
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
        } finally {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
        }
        return stringBuilder.toString();
    }

    private static class CachedBodyServletInputStream extends ServletInputStream {

        private final ByteArrayInputStream byteArrayInputStream;

        public CachedBodyServletInputStream(byte[] cachedBody) {
            this.byteArrayInputStream = new ByteArrayInputStream(cachedBody);
        }

        @Override
        public boolean isFinished() {
            return byteArrayInputStream.available() == 0;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            // Not implemented
        }

        @Override
        public int read() throws IOException {
            return byteArrayInputStream.read();
        }
    }
}