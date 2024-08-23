package com.gust.cafe.windycrypto.service;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import com.gust.cafe.windycrypto.dto.TreeLeafDTO;
import com.gust.cafe.windycrypto.exception.WindyException;
import com.gust.cafe.windycrypto.vo.req.FolderTreeReqVo;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 文件夹层级树服务
 *
 * @author Dororo
 * @date 2024-08-23 10:02
 */
@Service
public class FolderTreeService {
    // ElementUI树数据结构
    public List<TreeLeafDTO> getElementUiTreeData(FolderTreeReqVo folderTreeReqVo) {
        String path = folderTreeReqVo.getPath();
        WindyException.run((Void) -> Assert.isTrue(FileUtil.exist(path) && FileUtil.isDirectory(path), "路径不存在或不是目录"));
        // 树顶级节点ID
        int topId = 0;
        // 线程安全地操作原子类,实现ID自增
        AtomicInteger atomicId = new AtomicInteger(1);
        // 树顶级节点
        TreeLeafDTO topLeaf = TreeLeafDTO.builder().id(atomicId.get()).parentId(topId).label(FileUtil.getName(path)).absPath(path).build();

        // 子节点
        recursivelyCollect(topLeaf, atomicId);


        //
        return CollectionUtil.toList(topLeaf);
    }

    private static void recursivelyCollect(TreeLeafDTO topLeaf, AtomicInteger atomicId) {
        List<File> sonFolders = Arrays.stream(FileUtil.ls(topLeaf.getAbsPath())).filter(FileUtil::isDirectory).collect(Collectors.toList());
        if (CollectionUtil.isNotEmpty(sonFolders)) {
            List<TreeLeafDTO> children = new ArrayList<>();
            for (File sonFolder : sonFolders) {
                TreeLeafDTO sonLeaf = TreeLeafDTO.builder().id(atomicId.incrementAndGet()).parentId(topLeaf.getId()).label(sonFolder.getName()).absPath(sonFolder.getPath()).build();
                // 递归收集子节点
                recursivelyCollect(sonLeaf, atomicId);
                children.add(sonLeaf);
            }
            topLeaf.setChildren(children);
        }
    }

}
