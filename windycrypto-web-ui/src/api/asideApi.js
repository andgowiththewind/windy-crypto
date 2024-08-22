import request from '@/utils/request';

export function getTreeData(query) {
    return request({
        url: '/folder/getTreeData',
        method: 'get',
        params: query,
        headers: {
            enableAntiShake: false
        }
    })
}