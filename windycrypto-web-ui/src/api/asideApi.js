import request from '@/utils/request';

export function getTreeData(_data) {
    return request({
        url: '/folderTree/getTreeData',
        method: 'post',
        data: _data
    })
}