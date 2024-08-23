package com.gust.cafe.windycrypto.vo.res;

import com.gust.cafe.windycrypto.dto.core.Windy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InsightTableResVo {
    private List<Windy> list;
    private Long total;
}
