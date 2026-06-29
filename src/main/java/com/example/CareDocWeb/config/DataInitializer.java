package com.example.CareDocWeb.config;

import com.example.CareDocWeb.entity.CommonSettings;
import com.example.CareDocWeb.entity.Member;
import com.example.CareDocWeb.repository.CommonSettingsRepository;
import com.example.CareDocWeb.repository.MemberRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 初期データ投入設定クラス。
 *
 * <p>アプリケーション起動時に {@link CommandLineRunner} を通じて、
 * H2インメモリDBへサンプルデータを自動投入する。</p>
 *
 * <p>Phase 1（ローカル実行・インメモリ）での動作確認用。<br>
 * {@code @Profile("local")} により、{@code spring.profiles.active=local} の場合のみ有効。
 * Phase 2 以降（Supabase接続）ではプロファイルを切り替えることで自動的に無効化される。</p>
 *
 * <p>投入データ:</p>
 * <ul>
 *   <li>利用者（{@link Member}）— サンプル2名</li>
 *   <li>共通設定（{@link CommonSettings}）— 事業所情報1件</li>
 * </ul>
 *
 * @see com.example.CareDocWeb.repository.MemberRepository
 * @see com.example.CareDocWeb.repository.CommonSettingsRepository
 */
@Configuration
public class DataInitializer {

    /**
     * 初期データを投入する {@link CommandLineRunner} Bean。
     *
     * <p>Spring Boot 起動完了後に自動実行され、
     * リポジトリ経由でサンプルデータをDBに保存する。</p>
     *
     * @param memberRepo   利用者リポジトリ
     * @param settingsRepo 共通設定リポジトリ
     * @return 初期データ投入処理を行う CommandLineRunner
     */
    @Bean
    CommandLineRunner initData(MemberRepository memberRepo, CommonSettingsRepository settingsRepo) {
        return args -> {

            // サンプル利用者1
            Member m1 = new Member();
            m1.setName("田中太郎");
            m1.setFurigana("タナカタロウ");
            m1.setInsuranceIdNumber("0000000001");
            m1.setBirthYear(1940);
            m1.setBirthMonth(5);
            m1.setBirthDay(15);
            m1.setGender("男");
            m1.setAddress("東京都中央区日本橋1-1-1");
            m1.setPhone("03-1234-5678");
            m1.setCareLevel("要介護2");
            m1.setStartYear(2025);
            m1.setStartMonth(4);
            m1.setStartDay(1);
            m1.setEndYear(2026);
            m1.setEndMonth(3);
            m1.setEndDay(31);
            memberRepo.save(m1);

            // サンプル利用者2
            Member m2 = new Member();
            m2.setName("鈴木花子");
            m2.setFurigana("スズキハナコ");
            m2.setInsuranceIdNumber("0000000002");
            m2.setBirthYear(1935);
            m2.setBirthMonth(11);
            m2.setBirthDay(3);
            m2.setGender("女");
            m2.setAddress("東京都中央区築地2-2-2");
            m2.setPhone("03-9876-5432");
            m2.setCareLevel("要支援1");
            m2.setStartYear(2025);
            m2.setStartMonth(6);
            m2.setStartDay(1);
            m2.setEndYear(2026);
            m2.setEndMonth(5);
            m2.setEndDay(31);
            memberRepo.save(m2);

            // 共通設定（事業所全体で1レコード）
            CommonSettings settings = new CommonSettings();
            settings.setSurveyAddress("東京都中央区日本橋3-3-3");
            settings.setSurveyPhone("03-1111-2222");
            settings.setFacilityName("中央介護センター");
            settings.setFacilityPhone("03-3333-4444");
            settings.setInstitutionName("中央病院");
            settings.setInstitutionAddress("東京都中央区銀座4-4-4");
            settings.setAgentName("山田一郎");
            settings.setAgentPostal("104-0061");
            settings.setAgentAddress("東京都中央区銀座5-5-5");
            settings.setAgentPhone("03-5555-6666");
            settings.setDoctorName("佐藤医師");
            settings.setClinicName("中央クリニック");
            settings.setClinicPostal("104-0062");
            settings.setClinicAddress("東京都中央区銀座6-6-6");
            settings.setClinicPhone("03-7777-8888");
            settingsRepo.save(settings);

            System.out.println("=== 初期データ投入完了 ===");
        };
    }
}
