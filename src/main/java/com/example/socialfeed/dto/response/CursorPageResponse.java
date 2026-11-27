package com.example.socialfeed.dto.response;

import lombok.*;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CursorPageResponse<T> {
    private List<T> items;
    // Opaque token for the next page; null when there are no more items.
    private String nextCursor;
}
