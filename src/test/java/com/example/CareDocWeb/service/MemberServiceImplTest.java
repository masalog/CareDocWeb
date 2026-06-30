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
 * <p>全メソッドに対して正常系・境界値・異常系を網羅的にカバーする。
 * Repositoryをモック化し、サービス層のロジックのみを検証する。</p>
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
        void returnsAllMembers_whenMultipleExist() {
            // 準備
            Member m2 = new Member();
            m2.setId(UUID.randomUUID());
            m2.setName("鈴木花子");
            m2.setCareLevel("要支援1");
            when(memberRepository.findAll()).thenReturn(List.of(sampleMember, m2));

            // 実行
            List<Member> result = memberService.findAll();

            // 検証
            assertEquals(2, result.size());
            assertEquals("田中太郎", result.get(0).getName());
            assertEquals("鈴木花子", result.get(1).getName());
            verify(memberRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("正常系: 各利用者の全フィールドが保持されている")
        void retainsAllFields() {
            // 準備
            when(memberRepository.findAll()).thenReturn(List.of(sampleMember));

            // 実行
            List<Member> result = memberService.findAll();

            // 検証
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
        }

        // --- 境界値 ---

        @Test
        @DisplayName("境界値: 利用者が0件の場合、空リストを返す")
        void returnsEmptyList_whenNoMembers() {
            // 準備
            when(memberRepository.findAll()).thenReturn(Collections.emptyList());

            // 実行
            List<Member> result = memberService.findAll();

            // 検証
            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(memberRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("境界値: 利用者が1件のみの場合、1件のリストを返す")
        void returnsSingleElementList_whenOneMember() {
            // 準備
            when(memberRepository.findAll()).thenReturn(List.of(sampleMember));

            // 実行
            List<Member> result = memberService.findAll();

            // 検証
            assertEquals(1, result.size());
            assertEquals("田中太郎", result.get(0).getName());
        }

        @Test
        @DisplayName("境界値: 大量データ（100件）でもリスト全件返す")
        void returnsAll_whenLargeDataset() {
            // 準備
            List<Member> members = new java.util.ArrayList<>();
            for (int i = 0; i < 100; i++) {
                Member m = new Member();
                m.setId(UUID.randomUUID());
                m.setName("利用者" + i);
                members.add(m);
            }
            when(memberRepository.findAll()).thenReturn(members);

            // 実行
            List<Member> result = memberService.findAll();

            // 検証
            assertEquals(100, result.size());
        }

        // --- 異常系 ---

        @Test
        @DisplayName("異常系: リポジトリが例外をスローした場合、そのまま伝播する")
        void propagatesException_whenRepositoryThrows() {
            // 準備
            when(memberRepository.findAll()).thenThrow(new RuntimeException("DB接続エラー"));

            // 実行 & 検証
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
        void returnsMember_whenIdExists() {
            // 準備
            when(memberRepository.findById(sampleId)).thenReturn(Optional.of(sampleMember));

            // 実行
            Member result = memberService.findById(sampleId);

            // 検証
            assertNotNull(result);
            assertEquals(sampleId, result.getId());
            assertEquals("田中太郎", result.getName());
            verify(memberRepository, times(1)).findById(sampleId);
        }

        @Test
        @DisplayName("正常系: 取得した利用者の全フィールドが正しい")
        void returnsCorrectFields() {
            // 準備
            when(memberRepository.findById(sampleId)).thenReturn(Optional.of(sampleMember));

            // 実行
            Member result = memberService.findById(sampleId);

            // 検証
            assertEquals("タナカタロウ", result.getFurigana());
            assertEquals("0000000001", result.getInsuranceIdNumber());
            assertEquals(1940, result.getBirthYear());
            assertEquals("男", result.getGender());
            assertEquals("要介護2", result.getCareLevel());
        }

        // --- 境界値 ---

        @Test
        @DisplayName("境界値: NULL許容フィールドがNULLの利用者を正しく返す")
        void returnsMember_withNullableFieldsAsNull() {
            // 準備
            sampleMember.setEndYear(null);
            sampleMember.setEndMonth(null);
            sampleMember.setEndDay(null);
            sampleMember.setInstitutionYear(null);
            sampleMember.setInstitutionMonth(null);
            sampleMember.setInstitutionDay(null);
            when(memberRepository.findById(sampleId)).thenReturn(Optional.of(sampleMember));

            // 実行
            Member result = memberService.findById(sampleId);

            // 検証
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
        void throwsException_whenIdNotFound() {
            // 準備
            UUID nonExistentId = UUID.randomUUID();
            when(memberRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // 実行 & 検証
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> memberService.findById(nonExistentId));
            assertTrue(exception.getMessage().contains(nonExistentId.toString()));
            verify(memberRepository, times(1)).findById(nonExistentId);
        }

        @Test
        @DisplayName("異常系: NULLのIDを渡した場合、リポジトリに委譲される")
        void delegatesToRepository_whenIdIsNull() {
            // 準備
            when(memberRepository.findById(null)).thenReturn(Optional.empty());

            // 実行 & 検証
            assertThrows(RuntimeException.class,
                    () -> memberService.findById(null));
            verify(memberRepository, times(1)).findById(null);
        }

        @Test
        @DisplayName("異常系: リポジトリが例外をスローした場合、そのまま伝播する")
        void propagatesException_whenRepositoryThrows() {
            // 準備
            when(memberRepository.findById(sampleId))
                    .thenThrow(new RuntimeException("DB接続エラー"));

            // 実行 & 検証
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
        void savesNewMember_withGeneratedId() {
            // 準備
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

            // 実行
            Member result = memberService.save(newMember);

            // 検証
            assertNotNull(result.getId());
            assertEquals("佐藤次郎", result.getName());
            assertEquals("サトウジロウ", result.getFurigana());
            assertEquals("要支援2", result.getCareLevel());
            verify(memberRepository, times(1)).save(newMember);
        }

        @Test
        @DisplayName("正常系: 既存利用者の介護度を更新する")
        void updatesExistingMember() {
            // 準備
            sampleMember.setCareLevel("要介護3");
            when(memberRepository.save(sampleMember)).thenReturn(sampleMember);

            // 実行
            Member result = memberService.save(sampleMember);

            // 検証
            assertEquals(sampleId, result.getId());
            assertEquals("要介護3", result.getCareLevel());
            verify(memberRepository, times(1)).save(sampleMember);
        }

        @Test
        @DisplayName("正常系: 全フィールドを指定して登録する")
        void savesWithAllFields() {
            // 準備
            when(memberRepository.save(sampleMember)).thenReturn(sampleMember);

            // 実行
            Member result = memberService.save(sampleMember);

            // 検証
            assertEquals("田中太郎", result.getName());
            assertEquals("タナカタロウ", result.getFurigana());
            assertEquals("0000000001", result.getInsuranceIdNumber());
            assertEquals(1940, result.getBirthYear());
            assertEquals(5, result.getBirthMonth());
            assertEquals(15, result.getBirthDay());
            assertEquals("男", result.getGender());
        }

        // --- 境界値 ---

        @Test
        @DisplayName("境界値: 任意項目（終了日・施設入所日）がNULLでも登録できる")
        void savesWithNullOptionalFields() {
            // 準備
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

            // 実行
            Member result = memberService.save(minimalMember);

            // 検証
            assertNotNull(result.getId());
            assertEquals("最小データ", result.getName());
        }

        @Test
        @DisplayName("境界値: 名前が1文字の利用者を登録できる")
        void savesWithSingleCharName() {
            // 準備
            Member oneCharMember = new Member();
            oneCharMember.setName("あ");

            Member savedMember = new Member();
            savedMember.setId(UUID.randomUUID());
            savedMember.setName("あ");

            when(memberRepository.save(oneCharMember)).thenReturn(savedMember);

            // 実行
            Member result = memberService.save(oneCharMember);

            // 検証
            assertEquals("あ", result.getName());
        }

        @Test
        @DisplayName("境界値: 被保険者番号が空文字でも登録できる")
        void savesWithEmptyInsuranceId() {
            // 準備
            Member emptyInsurance = new Member();
            emptyInsurance.setName("テスト");
            emptyInsurance.setInsuranceIdNumber("");

            Member savedMember = new Member();
            savedMember.setId(UUID.randomUUID());
            savedMember.setName("テスト");
            savedMember.setInsuranceIdNumber("");

            when(memberRepository.save(emptyInsurance)).thenReturn(savedMember);

            // 実行
            Member result = memberService.save(emptyInsurance);

            // 検証
            assertEquals("", result.getInsuranceIdNumber());
        }

        // --- 異常系 ---

        @Test
        @DisplayName("異常系: NULLのエンティティを渡した場合、リポジトリに委譲される")
        void throwsException_whenEntityIsNull() {
            // 準備
            when(memberRepository.save(null))
                    .thenThrow(new IllegalArgumentException("エンティティがnullです"));

            // 実行 & 検証
            assertThrows(IllegalArgumentException.class,
                    () -> memberService.save(null));
            verify(memberRepository, times(1)).save(null);
        }

        @Test
        @DisplayName("異常系: リポジトリが例外をスローした場合、そのまま伝播する")
        void propagatesException_whenRepositoryThrows() {
            // 準備
            when(memberRepository.save(sampleMember))
                    .thenThrow(new RuntimeException("DB書き込みエラー"));

            // 実行 & 検証
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
        void deletesSuccessfully_whenIdExists() {
            // 準備
            doNothing().when(memberRepository).deleteById(sampleId);

            // 実行
            memberService.deleteById(sampleId);

            // 検証
            verify(memberRepository, times(1)).deleteById(sampleId);
        }

        @Test
        @DisplayName("正常系: 削除後にリポジトリのdeleteByIdが1回だけ呼ばれる")
        void callsRepositoryExactlyOnce() {
            // 準備
            doNothing().when(memberRepository).deleteById(sampleId);

            // 実行
            memberService.deleteById(sampleId);

            // 検証
            verify(memberRepository, times(1)).deleteById(sampleId);
            verifyNoMoreInteractions(memberRepository);
        }

        // --- 境界値 ---

        @Test
        @DisplayName("境界値: 存在しないIDを指定しても例外はスローされない")
        void doesNotThrow_whenIdDoesNotExist() {
            // 準備
            UUID nonExistentId = UUID.randomUUID();
            doNothing().when(memberRepository).deleteById(nonExistentId);

            // 実行 & 検証
            assertDoesNotThrow(() -> memberService.deleteById(nonExistentId));
            verify(memberRepository, times(1)).deleteById(nonExistentId);
        }

        // --- 異常系 ---

        @Test
        @DisplayName("異常系: NULLのIDを渡した場合、リポジトリに委譲される")
        void throwsException_whenIdIsNull() {
            // 準備
            doThrow(new IllegalArgumentException("IDがnullです"))
                    .when(memberRepository).deleteById(null);

            // 実行 & 検証
            assertThrows(IllegalArgumentException.class,
                    () -> memberService.deleteById(null));
            verify(memberRepository, times(1)).deleteById(null);
        }

        @Test
        @DisplayName("異常系: リポジトリが例外をスローした場合、そのまま伝播する")
        void propagatesException_whenRepositoryThrows() {
            // 準備
            doThrow(new RuntimeException("DB削除エラー"))
                    .when(memberRepository).deleteById(sampleId);

            // 実行 & 検証
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> memberService.deleteById(sampleId));
            assertEquals("DB削除エラー", exception.getMessage());
        }
    }
}
