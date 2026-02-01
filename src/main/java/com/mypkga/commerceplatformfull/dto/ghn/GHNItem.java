package com.mypkga.commerceplatformfull.dto.ghn;

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class GHNItem {
    private String name;
    private String code;
    private Integer quantity;
    private Integer price;
    private Integer length;
    private Integer width;
    private Integer height;
    private Integer weight;
    private String category;
}