import request from '@/utils/request';

export function getWsUrlPrefix() {
    return request({
        url: '/sys/getWsUrlPrefix',
        method: 'get',
    })
}