import request from '@/utils/request';


/**
 * 表格样式_行样式_根据行号换不同的背景色
 */
export function rowStyleFn({row, rowIndex}) {
    return {backgroundColor: (rowIndex % 2 === 0 ? '#f0f9eb' : '#b7a476'), color: 'black'};
}

/**
 * 表格样式_单元格样式_部分列的单元格希望内容居中有些又希望左对齐
 *     //根据列的字段名进行判断
 *     // if (['name', 'absPath'].includes(column.property)) {return {textAlign: 'left'};}
 *     //根据下标进行判断,下标从0开始
 *     // if ([1, 2, 3, 4, 5].includes(columnIndex)) {return {textAlign: 'left'};}
 */
export function cellStyleFn({row, column, rowIndex, columnIndex}) {
    if (['name', 'absPath'].includes(column.property)) {
        return {textAlign: 'left'};
    }
    if (['status', 'percentage', 'sizeLabel', 'extName'].includes(column.property)) {
        return {textAlign: 'center'};
    }
}

export function headerCellStyleFn({row, column, rowIndex, columnIndex}) {
    return {backgroundColor: '#b4bece', textAlign: 'center', color: 'black', fontSize: '10px'};
}