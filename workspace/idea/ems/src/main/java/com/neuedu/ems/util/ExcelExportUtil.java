package com.neuedu.ems.util;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Excel 通用导出工具类
 */
@Component
public class ExcelExportUtil {

    /**
     * 导出 Excel（单个 sheet）
     * @param response Http响应对象（用于下载文件）
     * @param data 导出数据列表
     * @param clazz 实体类Class（指定表头映射）
     * @param fileName 导出文件名（不含.xlsx后缀）
     * @param sheetName sheet名称
     */
    public void exportExcel(HttpServletResponse response, List<?> data, Class<?> clazz, 
                           String fileName, String sheetName) {
        try {
            // 1. 设置响应头（解决文件名乱码、指定下载类型）
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            // URLEncoder.encode 解决中文文件名乱码
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString()).replaceAll("\\+", "%20");
            response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + encodedFileName + ".xlsx");

            // 2. 写入Excel数据
            EasyExcel.write(response.getOutputStream(), clazz)
                    .sheet(sheetName) // 设置sheet名称
                    .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy()) // 自动适配列宽
                    .doWrite(data); // 写入数据
        } catch (Exception e) {
            // 导出失败时返回错误信息
            throw new RuntimeException("Excel导出失败：" + e.getMessage(), e);
        }
    }
}