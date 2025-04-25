package com.healthmanagement.dto.social;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class RankingStatDTO {
    private List<String> titles;
    private List<Integer> counts;
}
