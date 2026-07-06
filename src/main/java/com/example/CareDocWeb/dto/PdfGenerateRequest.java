package com.example.CareDocWeb.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.DateTimeException;
import java.time.Year;
import java.time.YearMonth;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
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
    @Min(value = 1, message = "申請月は1〜12の範囲で指定してください")
    @Max(value = 12, message = "申請月は1〜12の範囲で指定してください")
    private Integer applicationMonth;

    @NotNull(message = "申請日は必須です")
    @Min(value = 1, message = "申請日は1〜31の範囲で指定してください")
    @Max(value = 31, message = "申請日は1〜31の範囲で指定してください")
    private Integer applicationDay;

    private String changeReason;

    /**
     * 申請年が当年または翌年の範囲内かを検証する。
     * 過去年や遠い未来年を弾き、申請書として妥当な年のみ許可する。
     *
     * @return 申請年が当年または翌年なら true
     */
    @AssertTrue(message = "申請年は当年または翌年を指定してください")
    public boolean isApplicationYearInRange() {
        if (applicationYear == null) {
            return true; // null は @NotNull が担当するためここでは許可
        }
        int currentYear = LocalDate.now().getYear();
        return applicationYear == currentYear || applicationYear == currentYear + 1;
    }

    /**
     * 申請年月日が実在する日付かを検証する（うるう年・月末日を含む）。
     * 例: 2月30日、4月31日、非うるう年の2月29日などを弾く。
     * YearMonth.isValidDay() でその年月に指定日が存在するかを判定する（うるう年も自動考慮）。
     *
     * @return 実在する日付なら true
     */
    @AssertTrue(message = "申請年月日が正しくありません（存在しない日付です）")
    public boolean isValidDate() {
        if (applicationYear == null || applicationMonth == null || applicationDay == null) {
            return true; // null は @NotNull が担当
        }

        if (applicationMonth < 1 || applicationMonth > 12 || applicationDay < 1 || applicationDay > 31) {
            return true; // 範囲は @Min/@Max が担当
        }

        // 年の範囲チェック（Year.MIN_VALUE〜Year.MAX_VALUE）
        if (applicationYear < Year.MIN_VALUE || applicationYear > Year.MAX_VALUE) {
            return false; // ここで弾く
        }

        try {
            return YearMonth.of(applicationYear, applicationMonth).isValidDay(applicationDay);
        } catch (DateTimeException e) {
            return false; // 例外が出たら false を返す
        }
    }

}
