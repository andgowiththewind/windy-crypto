package com.gust.cafe.windycrypto.util;

import cn.hutool.core.io.FileUtil;
import com.gust.cafe.windycrypto.constant.CommonConstants;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

public class FreeMarkerUtils {


    public static void renderFile(FmConfig fmConfig) {
        try {
            Configuration cfg = new Configuration(CommonConstants.FREEMARKER_VERSION);// 定义模板引擎
            cfg.setDirectoryForTemplateLoading(fmConfig.getDirectoryForTemplateLoading());// 设置模板文件的目录
            Template template = cfg.getTemplate(fmConfig.getTemplateName());// 获取模板文件
            FileWriter fileWriter = new FileWriter(fmConfig.getOutputFile());// 定义输出文件
            template.process(fmConfig.getDataModel(), fileWriter);// 合并模板和数据模型
            fileWriter.close();// 关闭输出文件
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FmConfig {
        private File directoryForTemplateLoading;
        private Map<String, Object> dataModel;
        private String templateName;
        private File outputFile;
    }
}
