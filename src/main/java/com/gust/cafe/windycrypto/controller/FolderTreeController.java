package com.gust.cafe.windycrypto.controller;

import com.gust.cafe.windycrypto.dto.core.R;
import com.gust.cafe.windycrypto.service.FolderTreeService;
import com.gust.cafe.windycrypto.vo.req.FolderTreeReqVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 目录树控制器
 *
 * @author Dororo
 * @date 2024-08-23 10:00
 */
@RestController
@RequestMapping("/folderTree")
public class FolderTreeController {
    @Autowired
    private FolderTreeService folderTreeService;

    @PostMapping("/getTreeData")
    public R getTreeData(@RequestBody @Validated FolderTreeReqVo folderTreeReqVo) {
        return R.data(folderTreeService.getTreeData(folderTreeReqVo));
    }
}
