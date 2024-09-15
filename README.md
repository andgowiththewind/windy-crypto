<h1 align="center" style="color:rgb(0,133,125)">windy-crypto</h1>

<div style="text-align: center;"><img src="./attachments/md/logo_left.png" referrerpolicy="no-referrer" alt="logo"></div>

---

# 介绍

`windy-crypto`是一个基于`SpringBoot+Vue`的可视化文件加解密工具，支持对文件进行加密、解密等操作。加密方式采用流式加密，每个字节都加密，破解难度=256^字节数，基本上8个字节以上的文件在当前世界的物理条件已经无法破解。





# 场景

2024 年 9 月 14 日，阿里云盘被曝出存在一个“灾难级的严重 bug”。有用户偶然发现，在阿里云盘的相册功能中，只要创建一个文件夹，然后在分类选择图片这一操作下，竟然可以看到其他用户云盘里的图片。

`windy-crypto`正是可以解决此类问题，我们即需要使用网络云盘进行备份存储，但同时不希望运营商内部能查看我们的资产，或者因为阿里云盘此类的灾难事件泄漏我们的资产。所以我们需要一个能快速加解密的工具进行文件加密解密。





# 安装

WIN：下载exe文件；