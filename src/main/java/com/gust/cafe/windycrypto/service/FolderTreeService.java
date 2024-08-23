package com.gust.cafe.windycrypto.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import com.gust.cafe.windycrypto.exception.WindyException;
import com.gust.cafe.windycrypto.vo.req.FolderTreeReqVo;
import com.gust.cafe.windycrypto.vo.res.FolderTreeResVo;
import org.springframework.stereotype.Service;

/**
 * 文件夹层级树服务
 *
 * @author Dororo
 * @date 2024-08-23 10:02
 */
@Service
public class FolderTreeService {
    public FolderTreeResVo getTreeData(FolderTreeReqVo folderTreeReqVo) {
        String path = folderTreeReqVo.getPath();
        WindyException.run((Void) -> Assert.isTrue(FileUtil.exist(path) && FileUtil.isDirectory(path), "路径不存在或不是目录"));
        return null;
    }
}
