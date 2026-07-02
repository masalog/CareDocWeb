package com.example.CareDocWeb.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PdfGenerateRequest {

    @NotNull(message = "利用者IDは必須です")
    private UUID memberId;

    @NotNull(message = "申請年は必須です")
    private Integer applicationYear;

    @NotNull(message = "申請月は必須です")
    private Integer applicationMonth;

    @NotNull(message = "申請日は必須です")
    private Integer applicationDay;

    private String changeReason;
}
