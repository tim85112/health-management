package com.healthmanagement.dto.social;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class MonthlyStatDTO {
    private List<String> months;
    private List<Integer> counts;
}
