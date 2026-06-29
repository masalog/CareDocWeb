package com.example.CareDocWeb.service;

import com.example.CareDocWeb.entity.Member;
import com.example.CareDocWeb.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * {@link MemberServiceImpl} のユニットテスト。
 *
 * <p>全メソッドに対して正常系・境界値・異常系を網羅的にカバーし、
 * Repositoryをモック化してサービス層のロジックのみを検証する。</p>
 */
@ExtendWith(MockitoExtension.class)
class MemberServiceImplTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberServiceImpl memberService;

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
        sampleMember.setEndYear(2026);
        sampleMember.setEndMonth(3);
        sampleMember.setEndDay(31);
    }

    // ========================================
    // findAll
    // ========================================

    @Nested
    @DisplayName("findAll")
    class FindAll {

        // --- 正常系 ---

        @Test
        @DisplayName("正常系: 複数件の利用者が存在する場合、全件をリストで返す")
        void 複数件存在する場合_全件返す() {
            // Given
            Member m2 = new Member();
            m2.setId(UUID.randomUUID());
            m2.setName("鈴木花子");
            m2.setCareLevel("要支援1");
            when(memberRepository.findAll()).thenReturn(List.of(sampleMember, m2));

            // When
            List<Member> result = memberService.findAll();

            // Then
            assertEquals(2, result.size());
            assertEquals("田中太郎", result.get(0).getName());
            assertEquals("鈴木花子", result.get(1).getName());
            verify(memberRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("正常系: 各利用者の全フィールドが保持されている")
        void 全フィールドが保持されている() {
            // Given
            when(memberRepository.findAll()).thenReturn(List.of(sampleMember));

            // When
            List<Member> result = memberService.findAll();

            // Then
            Member m = result.get(0);
            assertEquals(sampleId, m.getId());
            assertEquals("田中太郎", m.getName());
            assertEquals("タナカタロウ", m.getFurigana());
            assertEquals("0000000001", m.getInsuranceIdNumber());
            assertEquals(1940, m.getBirthYear());
            assertEquals(5, m.getBirthMonth());
            assertEquals(15, m.getBirthDay());
            assertEquals("男", m.getGender());
            assertEquals("東京都中央区日本橋1-1-1", m.getAddress());
            assertEquals("03-1234-5678", m.getPhone());
            assertEquals("要介護2", m.getCareLevel());
            assertEquals(2025, m.getStartYear());
            assertEquals(4, m.getStartMonth());
            assertEquals(1, m.getStartDay());
            assertEquals(2026, m.getEndYear());
            assertEquals(3, m.getEndMonth());
            assertEquals(31, m.getEndDay());
        }

        // --- 境界値 ---

        @Test
        @DisplayName("境界値: 利用者が0件の場合、空リストを返す")
        void 利用者0件の場合_空リストを返す() {
            // Given
            when(memberRepository.findAll()).thenReturn(Collections.emptyList());

            // When
            List<Member> result = memberService.findAll();

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(memberRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("境界値: 利用者が1件のみの場合、1件のリストを返す")
        void 利用者1件のみの場合_1件返す() {
            // Given
            when(memberRepository.findAll()).thenReturn(List.of(sampleMember));

            // When
            List<Member> result = memberService.findAll();

            // Then
            assertEquals(1, result.size());
            assertEquals("田中太郎", result.get(0).getName());
        }

        @Test
        @DisplayName("境界値: 大量データ（100件）でもリスト全件返す")
        void 大量データでも全件返す() {
            // Given
            List<Member> members = new java.util.ArrayList<>();
            for (int i = 0; i < 100; i++) {
                Member m = new Member();
                m.setId(UUID.randomUUID());
                m.setName("利用者" + i);
                members.add(m);
            }
            when(memberRepository.findAll()).thenReturn(members);

            // When
            List<Member> result = memberService.findAll();

            // Then
            assertEquals(100, result.size());
        }

        // --- 異常系 ---

        @Test
        @DisplayName("異常系: リポジトリが例外をスローした場合、そのまま伝播する")
        void リポジトリ例外_伝播する() {
            // Given
            when(memberRepository.findAll()).thenThrow(new RuntimeException("DB接続エラー"));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> memberService.findAll());
            assertEquals("DB接続エラー", exception.getMessage());
        }
    }

    // ========================================
    // findById
    // ========================================

    @Nested
    @DisplayName("findById")
    class FindById {

        // --- 正常系 ---

        @Test
        @DisplayName("正常系: 存在するIDを指定した場合、該当する利用者を返す")
        void 存在するID_利用者を返す() {
            // Given
            when(memberRepository.findById(sampleId)).thenReturn(Optional.of(sampleMember));

            // When
            Member result = memberService.findById(sampleId);

            // Then
            assertNotNull(result);
            assertEquals(sampleId, result.getId());
            assertEquals("田中太郎", result.getName());
            verify(memberRepository, times(1)).findById(sampleId);
        }

        @Test
        @DisplayName("正常系: 取得した利用者の全フィールドが正しい")
        void 全フィールドが正しい() {
            // Given
            when(memberRepository.findById(sampleId)).thenReturn(Optional.of(sampleMember));

            // When
            Member result = memberService.findById(sampleId);

            // Then
            assertEquals("タナカタロウ", result.getFurigana());
            assertEquals("0000000001", result.getInsuranceIdNumber());
            assertEquals(1940, result.getBirthYear());
            assertEquals("男", result.getGender());
            assertEquals("要介護2", result.getCareLevel());
        }

        // --- 境界値 ---

        @Test
        @DisplayName("境界値: NULL許容フィールドがNULLの利用者を正しく返す")
        void NULL許容フィールドがNULLでも返す() {
            // Given
            sampleMember.setEndYear(null);
            sampleMember.setEndMonth(null);
            sampleMember.setEndDay(null);
            sampleMember.setInstitutionYear(null);
            sampleMember.setInstitutionMonth(null);
            sampleMember.setInstitutionDay(null);
            when(memberRepository.findById(sampleId)).thenReturn(Optional.of(sampleMember));

            // When
            Member result = memberService.findById(sampleId);

            // Then
            assertNotNull(result);
            assertEquals("田中太郎", result.getName());
            assertNull(result.getEndYear());
            assertNull(result.getEndMonth());
            assertNull(result.getEndDay());
            assertNull(result.getInstitutionYear());
            assertNull(result.getInstitutionMonth());
            assertNull(result.getInstitutionDay());
        }

        // --- 異常系 ---

        @Test
        @DisplayName("異常系: 存在しないIDを指定した場合、RuntimeExceptionをスローする")
        void 存在しないID_例外をスロー() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(memberRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> memberService.findById(nonExistentId));
            assertTrue(exception.getMessage().contains("利用者が見つかりません"));
            assertTrue(exception.getMessage().contains(nonExistentId.toString()));
            verify(memberRepository, times(1)).findById(nonExistentId);
        }

        @Test
        @DisplayName("異常系: NULLのIDを渡した場合、リポジトリに委譲される")
        void NULLのID_リポジトリに委譲() {
            // Given
            when(memberRepository.findById(null)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(RuntimeException.class,
                    () -> memberService.findById(null));
        }

        @Test
        @DisplayName("異常系: リポジトリが例外をスローした場合、そのまま伝播する")
        void リポジトリ例外_伝播する() {
            // Given
            when(memberRepository.findById(sampleId))
                    .thenThrow(new RuntimeException("DB接続エラー"));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> memberService.findById(sampleId));
            assertEquals("DB接続エラー", exception.getMessage());
        }
    }

    // ========================================
    // save
    // ========================================

    @Nested
    @DisplayName("save")
    class Save {

        // --- 正常系 ---

        @Test
        @DisplayName("正常系: 新規利用者を登録し、IDが付与された状態で返す")
        void 新規利用者_登録される() {
            // Given
            Member newMember = new Member();
            newMember.setName("佐藤次郎");
            newMember.setFurigana("サトウジロウ");
            newMember.setCareLevel("要支援2");

            Member savedMember = new Member();
            savedMember.setId(UUID.randomUUID());
            savedMember.setName("佐藤次郎");
            savedMember.setFurigana("サトウジロウ");
            savedMember.setCareLevel("要支援2");

            when(memberRepository.save(newMember)).thenReturn(savedMember);

            // When
            Member result = memberService.save(newMember);

            // Then
            assertNotNull(result.getId());
            assertEquals("佐藤次郎", result.getName());
            assertEquals("サトウジロウ", result.getFurigana());
            assertEquals("要支援2", result.getCareLevel());
            verify(memberRepository, times(1)).save(newMember);
        }

        @Test
        @DisplayName("正常系: 既存利用者の介護度を更新する")
        void 既存利用者_更新される() {
            // Given
            sampleMember.setCareLevel("要介護3");
            when(memberRepository.save(sampleMember)).thenReturn(sampleMember);

            // When
            Member result = memberService.save(sampleMember);

            // Then
            assertEquals(sampleId, result.getId());
            assertEquals("要介護3", result.getCareLevel());
            verify(memberRepository, times(1)).save(sampleMember);
        }

        @Test
        @DisplayName("正常系: 全フィールドを指定して登録する")
        void 全フィールド指定_登録される() {
            // Given
            when(memberRepository.save(sampleMember)).thenReturn(sampleMember);

            // When
            Member result = memberService.save(sampleMember);

            // Then
            assertEquals("田中太郎", result.getName());
            assertEquals("タナカタロウ", result.getFurigana());
            assertEquals("0000000001", result.getInsuranceIdNumber());
            assertEquals(1940, result.getBirthYear());
            assertEquals(5, result.getBirthMonth());
            assertEquals(15, result.getBirthDay());
            assertEquals("男", result.getGender());
            assertEquals("東京都中央区日本橋1-1-1", result.getAddress());
            assertEquals("03-1234-5678", result.getPhone());
            assertEquals("要介護2", result.getCareLevel());
        }

        // --- 境界値 ---

        @Test
        @DisplayName("境界値: 任意項目（終了日・施設入所日）がNULLでも登録できる")
        void 任意項目がNULL_登録される() {
            // Given
            Member minimalMember = new Member();
            minimalMember.setName("最小データ");
            minimalMember.setEndYear(null);
            minimalMember.setEndMonth(null);
            minimalMember.setEndDay(null);
            minimalMember.setInstitutionYear(null);
            minimalMember.setInstitutionMonth(null);
            minimalMember.setInstitutionDay(null);

            Member savedMember = new Member();
            savedMember.setId(UUID.randomUUID());
            savedMember.setName("最小データ");

            when(memberRepository.save(minimalMember)).thenReturn(savedMember);

            // When
            Member result = memberService.save(minimalMember);

            // Then
            assertNotNull(result.getId());
            assertEquals("最小データ", result.getName());
        }

        @Test
        @DisplayName("境界値: 名前が1文字の利用者を登録できる")
        void 名前1文字_登録される() {
            // Given
            Member oneCharMember = new Member();
            oneCharMember.setName("あ");

            Member savedMember = new Member();
            savedMember.setId(UUID.randomUUID());
            savedMember.setName("あ");

            when(memberRepository.save(oneCharMember)).thenReturn(savedMember);

            // When
            Member result = memberService.save(oneCharMember);

            // Then
            assertEquals("あ", result.getName());
        }

        @Test
        @DisplayName("境界値: 被保険者番号が空文字でも登録できる")
        void 被保険者番号空文字_登録される() {
            // Given
            Member emptyInsurance = new Member();
            emptyInsurance.setName("テスト");
            emptyInsurance.setInsuranceIdNumber("");

            Member savedMember = new Member();
            savedMember.setId(UUID.randomUUID());
            savedMember.setName("テスト");
            savedMember.setInsuranceIdNumber("");

            when(memberRepository.save(emptyInsurance)).thenReturn(savedMember);

            // When
            Member result = memberService.save(emptyInsurance);

            // Then
            assertEquals("", result.getInsuranceIdNumber());
        }

        // --- 異常系 ---

        @Test
        @DisplayName("異常系: NULLのエンティティを渡した場合、リポジトリに委譲される")
        void NULLエンティティ_リポジトリに委譲() {
            // Given
            when(memberRepository.save(null))
                    .thenThrow(new IllegalArgumentException("エンティティがnullです"));

            // When & Then
            assertThrows(IllegalArgumentException.class,
                    () -> memberService.save(null));
            verify(memberRepository, times(1)).save(null);
        }

        @Test
        @DisplayName("異常系: リポジトリが例外をスローした場合、そのまま伝播する")
        void リポジトリ例外_伝播する() {
            // Given
            when(memberRepository.save(sampleMember))
                    .thenThrow(new RuntimeException("DB書き込みエラー"));

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> memberService.save(sampleMember));
            assertEquals("DB書き込みエラー", exception.getMessage());
        }
    }

    // ========================================
    // deleteById
    // ========================================

    @Nested
    @DisplayName("deleteById")
    class DeleteById {

        // --- 正常系 ---

        @Test
        @DisplayName("正常系: 存在するIDを指定して削除する")
        void 存在するID_削除される() {
            // Given
            doNothing().when(memberRepository).deleteById(sampleId);

            // When
            memberService.deleteById(sampleId);

            // Then
            verify(memberRepository, times(1)).deleteById(sampleId);
        }

        @Test
        @DisplayName("正常系: 削除後にリポジトリのdeleteByIdが1回だけ呼ばれる")
        void リポジトリが1回呼ばれる() {
            // Given
            doNothing().when(memberRepository).deleteById(sampleId);

            // When
            memberService.deleteById(sampleId);

            // Then
            verify(memberRepository, times(1)).deleteById(sampleId);
            verifyNoMoreInteractions(memberRepository);
        }

        // --- 境界値 ---

        @Test
        @DisplayName("境界値: 存在しないIDを指定しても例外はスローされない")
        void 存在しないID_例外なし() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            doNothing().when(memberRepository).deleteById(nonExistentId);

            // When & Then（例外が出ないことを確認）
            assertDoesNotThrow(() -> memberService.deleteById(nonExistentId));
            verify(memberRepository, times(1)).deleteById(nonExistentId);
        }

        // --- 異常系 ---

        @Test
        @DisplayName("異常系: NULLのIDを渡した場合、リポジトリに委譲される")
        void NULLのID_リポジトリに委譲() {
            // Given
            doThrow(new IllegalArgumentException("IDがnullです"))
                    .when(memberRepository).deleteById(null);

            // When & Then
            assertThrows(IllegalArgumentException.class,
                    () -> memberService.deleteById(null));
            verify(memberRepository, times(1)).deleteById(null);
        }

        @Test
        @DisplayName("異常系: リポジトリが例外をスローした場合、そのまま伝播する")
        void リポジトリ例外_伝播する() {
            // Given
            doThrow(new RuntimeException("DB削除エラー"))
                    .when(memberRepository).deleteById(sampleId);

            // When & Then
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> memberService.deleteById(sampleId));
            assertEquals("DB削除エラー", exception.getMessage());
        }
    }
}
