package com.neuedu.ems.util;

import jakarta.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;

public class FileDownloadUtil {

    public static void download(HttpServletResponse response, String filePath, String fileName) throws Exception {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new FileNotFoundException("文件不存在：" + filePath);
        }

        // ✅ 修复文件名变成 response.bin 的核心代码
        String encodeFileName = URLEncoder.encode(fileName, "UTF-8");
        response.setContentType("application/octet-stream");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + encodeFileName + "\"; filename*=UTF-8''" + encodeFileName);
        response.setHeader("Content-Length", String.valueOf(file.length()));

        try (InputStream in = new FileInputStream(file);
             OutputStream out = response.getOutputStream()) {

            byte[] buffer = new byte[4096];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            out.flush();
        }
    }
}