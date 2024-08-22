/**
 * 拖拽改变宽度的指令
 * <p>原作者:https://blog.csdn.net/yehaocheng520/article/details/131721881</p>
 * bug修改 dragDom.style.width = `${nw}px`; 增加了$符号
 */
export default {
    install(Vue) {
        Vue.mixin({
            directives: {
                drag: {
                    inserted: function (el) {
                        const dragDom = el;
                        dragDom.style.cursor = "e-resize";
                        dragDom.onmousedown = e => {
                            //鼠标按下，计算当前元素距离可视区的距离
                            const disX = e.clientX;
                            const w = dragDom.clientWidth;
                            const minW = 50;
                            const maxW = 600;
                            var nw;
                            document.onmousemove = function (e) {
                                //通过事件委托，计算移动的距离
                                const l = e.clientX - disX;
                                //改变当前元素宽度，不可超过最小最大值
                                nw = w + l;
                                nw = nw < minW ? minW : nw;
                                nw = nw > maxW ? maxW : nw;
                                dragDom.style.width = `${nw}px`;
                            }
                            document.onmouseup = function (e) {
                                document.onmousemove = null;
                                document.onmouseup = null;
                            }
                        }
                    }
                }
            }
        })
    }
}