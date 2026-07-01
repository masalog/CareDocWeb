package com.example.CareDocWeb.controller;

import com.example.CareDocWeb.entity.Member;
import com.example.CareDocWeb.service.MemberService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * {@link MemberController} の統合テスト。
 *
 * <p>{@code @WebMvcTest} を使用し、HTTPリクエスト/レスポンスの観点から
 * コントローラー層の動作を検証する。サービス層はモック化する。</p>
 */
@WebMvcTest(MemberController.class)
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MemberService memberService;

    private Member sampleMember;
    private UUID sampleId;

    @BeforeEach
    void setUp() {
        sampleId = UUID.randomUUID();
        sampleMember = new Member();
        sampleMember.setId(sampleId);
        sampleMember.setName("田中太郎");
        sampleMember.setFurigana("タナカタロウ");
        sampleMember.setInsuranceIdNumber("0000000001");
        sampleMember.setBirthYear(1940);
        sampleMember.setBirthMonth(5);
        sampleMember.setBirthDay(15);
        sampleMember.setGender("男");
        sampleMember.setAddress("東京都中央区日本橋1-1-1");
        sampleMember.setPhone("03-1234-5678");
        sampleMember.setCareLevel("要介護2");
        sampleMember.setStartYear(2025);
        sampleMember.setStartMonth(4);
        sampleMember.setStartDay(1);
    }

    // ========================================
    // GET /api/members
    // ========================================

    @Nested
    @DisplayName("GET /api/members")
    class FindAll {

        // --- 正常系 ---

        @Test
        @DisplayName("正常系: 利用者一覧を200で返す")
        void returns200_withMemberList() throws Exception {
            // 準備
            Member m2 = new Member();
            m2.setId(UUID.randomUUID());
            m2.setName("鈴木花子");
            when(memberService.findAll()).thenReturn(List.of(sampleMember, m2));

            // 実行 & 検証
            mockMvc.perform(get("/api/members"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].name").value("田中太郎"))
                    .andExpect(jsonPath("$[1].name").value("鈴木花子"));

            verify(memberService, times(1)).findAll();
        }

        // --- 境界値 ---

        @Test
        @DisplayName("境界値: 利用者が0件の場合、空配列を200で返す")
        void returns200_withEmptyList() throws Exception {
            // 準備
            when(memberService.findAll()).thenReturn(Collections.emptyList());

            // 実行 & 検証
            mockMvc.perform(get("/api/members"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        // --- 異常系 ---

        @Test
        @DisplayName("異常系: サービスが例外をスローした場合、500を返す")
        void returns500_whenServiceThrows() throws Exception {
            // 準備
            when(memberService.findAll()).thenThrow(new RuntimeException("DB接続エラー"));

            // 実行 & 検証
            mockMvc.perform(get("/api/members"))
                    .andExpect(status().isInternalServerError());
        }
    }

    // ========================================
    // GET /api/members/{id}
    // ========================================

    @Nested
    @DisplayName("GET /api/members/{id}")
    class FindById {

        // --- 正常系 ---

        @Test
        @DisplayName("正常系: 存在するIDで利用者詳細を200で返す")
        void returns200_withMember() throws Exception {
            // 準備
            when(memberService.findById(sampleId)).thenReturn(sampleMember);

            // 実行 & 検証
            mockMvc.perform(get("/api/members/{id}", sampleId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value(sampleId.toString()))
                    .andExpect(jsonPath("$.name").value("田中太郎"))
                    .andExpect(jsonPath("$.furigana").value("タナカタロウ"))
                    .andExpect(jsonPath("$.insuranceIdNumber").value("0000000001"))
                    .andExpect(jsonPath("$.careLevel").value("要介護2"));

            verify(memberService, times(1)).findById(sampleId);
        }

        // --- 異常系 ---

        @Test
        @DisplayName("異常系: 存在しないIDの場合、404を返す")
        void returns404_whenNotFound() throws Exception {
            // 準備
            UUID nonExistentId = UUID.randomUUID();
            when(memberService.findById(nonExistentId))
                    .thenThrow(new com.example.CareDocWeb.exception.ResourceNotFoundException("利用者が見つかりません: " + nonExistentId));

            // 実行 & 検証
            mockMvc.perform(get("/api/members/{id}", nonExistentId))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("異常系: 不正なID形式の場合、エラーを返す")
        void returnsError_whenInvalidIdFormat() throws Exception {
            // 実行 & 検証
            mockMvc.perform(get("/api/members/{id}", "invalid-uuid"))
                    .andExpect(status().isInternalServerError());
        }
    }

    // ========================================
    // POST /api/members
    // ========================================

    @Nested
    @DisplayName("POST /api/members")
    class Create {

        // --- 正常系 ---

        @Test
        @DisplayName("正常系: 利用者を登録して201で返す")
        void returns201_withCreatedMember() throws Exception {
            // 準備
            Member newMember = new Member();
            newMember.setName("佐藤次郎");
            newMember.setCareLevel("要支援2");

            Member savedMember = new Member();
            savedMember.setId(UUID.randomUUID());
            savedMember.setName("佐藤次郎");
            savedMember.setCareLevel("要支援2");

            when(memberService.save(any(Member.class))).thenReturn(savedMember);

            // 実行 & 検証
            mockMvc.perform(post("/api/members")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newMember)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("佐藤次郎"))
                    .andExpect(jsonPath("$.careLevel").value("要支援2"))
                    .andExpect(jsonPath("$.id").isNotEmpty());

            verify(memberService, times(1)).save(any(Member.class));
        }

        // --- 境界値 ---

        @Test
        @DisplayName("境界値: 必須項目のみで登録できる")
        void returns201_withMinimalData() throws Exception {
            // 準備
            Member minimalMember = new Member();
            minimalMember.setName("最小データ");

            Member savedMember = new Member();
            savedMember.setId(UUID.randomUUID());
            savedMember.setName("最小データ");

            when(memberService.save(any(Member.class))).thenReturn(savedMember);

            // 実行 & 検証
            mockMvc.perform(post("/api/members")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(minimalMember)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("最小データ"));
        }

        // --- 異常系 ---

        @Test
        @DisplayName("異常系: リクエストボディが空の場合、エラーを返す")
        void returnsError_whenBodyIsEmpty() throws Exception {
            // 実行 & 検証
            mockMvc.perform(post("/api/members")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(""))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("異常系: サービスが例外をスローした場合、500を返す")
        void returns500_whenServiceThrows() throws Exception {
            // 準備
            when(memberService.save(any(Member.class)))
                    .thenThrow(new RuntimeException("DB書き込みエラー"));

            Member newMember = new Member();
            newMember.setName("テスト");

            // 実行 & 検証
            mockMvc.perform(post("/api/members")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(newMember)))
                    .andExpect(status().isInternalServerError());
        }
    }

    // ========================================
    // PUT /api/members/{id}
    // ========================================

    @Nested
    @DisplayName("PUT /api/members/{id}")
    class Update {

        // --- 正常系 ---

        @Test
        @DisplayName("正常系: 利用者を更新して200で返す")
        void returns200_withUpdatedMember() throws Exception {
            // 準備
            sampleMember.setCareLevel("要介護3");
            when(memberService.save(any(Member.class))).thenReturn(sampleMember);

            // 実行 & 検証
            mockMvc.perform(put("/api/members/{id}", sampleId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sampleMember)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(sampleId.toString()))
                    .andExpect(jsonPath("$.careLevel").value("要介護3"));

            verify(memberService, times(1)).save(any(Member.class));
        }

        // --- 異常系 ---

        @Test
        @DisplayName("異常系: 不正なID形式の場合、エラーを返す")
        void returnsError_whenInvalidIdFormat() throws Exception {
            // 実行 & 検証
            mockMvc.perform(put("/api/members/{id}", "invalid-uuid")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sampleMember)))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("異常系: サービスが例外をスローした場合、500を返す")
        void returns500_whenServiceThrows() throws Exception {
            // 準備
            when(memberService.save(any(Member.class)))
                    .thenThrow(new RuntimeException("DB書き込みエラー"));

            // 実行 & 検証
            mockMvc.perform(put("/api/members/{id}", sampleId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sampleMember)))
                    .andExpect(status().isInternalServerError());
        }
    }

    // ========================================
    // DELETE /api/members/{id}
    // ========================================

    @Nested
    @DisplayName("DELETE /api/members/{id}")
    class Delete {

        // --- 正常系 ---

        @Test
        @DisplayName("正常系: 利用者を削除して204で返す")
        void returns204_whenDeleted() throws Exception {
            // 準備
            doNothing().when(memberService).deleteById(sampleId);

            // 実行 & 検証
            mockMvc.perform(delete("/api/members/{id}", sampleId))
                    .andExpect(status().isNoContent());

            verify(memberService, times(1)).deleteById(sampleId);
        }

        // --- 境界値 ---

        @Test
        @DisplayName("異常系: 存在しないIDで削除しようとした場合、404を返す")
        void returns404_whenIdDoesNotExist() throws Exception {
            // 準備
            UUID nonExistentId = UUID.randomUUID();
            when(memberService.existsById(nonExistentId)).thenReturn(false);

            // 実行 & 検証
            mockMvc.perform(delete("/api/members/{id}", nonExistentId))
                    .andExpect(status().isNotFound());
        }

        // --- 異常系 ---

        @Test
        @DisplayName("異常系: 不正なID形式の場合、エラーを返す")
        void returnsError_whenInvalidIdFormat() throws Exception {
            // 実行 & 検証
            mockMvc.perform(delete("/api/members/{id}", "invalid-uuid"))
                    .andExpect(status().isInternalServerError());
        }

        @Test
        @DisplayName("異常系: サービスが例外をスローした場合、500を返す")
        void returns500_whenServiceThrows() throws Exception {
            // 準備
            doThrow(new RuntimeException("DB削除エラー"))
                    .when(memberService).deleteById(sampleId);

            // 実行 & 検証
            mockMvc.perform(delete("/api/members/{id}", sampleId))
                    .andExpect(status().isInternalServerError());
        }
    }
}
