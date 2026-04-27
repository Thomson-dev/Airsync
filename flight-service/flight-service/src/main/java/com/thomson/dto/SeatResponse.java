package com.thomson.dto;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SeatResponse {
    private Long id;
    private String seatNumber;
    private String seatClass;
    private Boolean available;
}
