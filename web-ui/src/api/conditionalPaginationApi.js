import request from '@/utils/request';

export function getInsightTableData(_data) {
    return request({
        url: '/insightTable/getData',
        method: 'post',
        data: _data,
        headers: {
            enableAntiShake: true,
            timeout: 30000
        }
    })
}