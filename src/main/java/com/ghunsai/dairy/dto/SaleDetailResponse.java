package com.ghunsai.dairy.dto;

import java.util.List;

/** Full bill: header + line items (receipt). */
public record SaleDetailResponse(SaleResponse sale, List<SaleItemResponse> items) {
}
