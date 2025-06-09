package com.example.wms.moveorder.dto;

import com.example.wms.moveorder.domain.MoveOrderType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CreateMoveOrderRequest {

    @NotBlank(message = "오더 이름은 필수입니다.")
    private String name;

    @NotNull(message = "오더 타입은 필수입니다.")
    private MoveOrderType type;

    @NotNull(message = "출발지 ID는 필수입니다.")
    private Long fromLocationId;

    @NotNull(message = "도착지 ID는 필수입니다.")
    private Long toLocationId;

    @NotNull(message = "예정일은 필수입니다.")
    private LocalDate scheduledDate;

    @NotNull(message = "상품 ID는 필수입니다.")
    private Long wareId;

    @NotNull(message = "수량은 필수입니다.")
    @Min(value = 1, message = "수량은 1 이상이어야 합니다.")
    private Long quantity;
} 