# CareDocWeb

CareDocWeb は、介護保険の「要介護認定・要支援認定 申請書」を Supabase 上のデータから<br>
PDF に転記して作成するための Web アプリケーションです。<br>
デスクトップ版 [CareDoc](https://github.com/masalog/CareDoc) の Web 移行版です。

## ✨ 主な機能
- プルダウンから名前を選択すると、申請書PDFを生成・ダウンロード
- カレンダーから申請年月日を指定
- 変更更新理由の入力（任意）
- 利用者の登録・編集・削除（Web画面からCRUD）
- 共通データ（事業所・担当者情報）の編集
- ログイン認証（Supabase Auth）

## 🛠 技術スタック

| 項目 | 技術 |
|------|------|
| フロントエンド | HTML + htmx + CSS（S3 ホスティング） |
| バックエンド | Java 21 + Spring Boot 4.1.0 |
| DB | Supabase（PostgreSQL） / H2（Phase 1 ローカル） |
| ORM | Spring Data JPA |
| PDF生成 | Apache PDFBox 3.0.4 |
| テスト | JUnit 5 + Mockito + MockMvc |
| インフラ | AWS S3（フロント）+ EC2（API） |
| IDE | IntelliJ IDEA Community Edition |
| ビルド | Maven |

## 📐 システム構成

```
ブラウザ → S3（HTML + htmx + CSS）
              ↓ htmx リクエスト
           EC2（Spring Boot API）
              ↓ JPA
           Supabase（PostgreSQL）
```

## 📂 プロジェクト構成

```text
CareDocWeb/
├── docs/
│   ├── CareDocWeb 要件定義書.md
│   ├── CareDocWeb API設計書.md
│   ├── CareDocWeb DB設計書.md
│   └── CareDocWeb PDF生成サービス実装方針.md
├── src/
│   ├── main/
│   │   ├── java/com/example/CareDocWeb/
│   │   │   ├── CareDocWebApplication.java
│   │   │   ├── config/
│   │   │   │   └── DataInitializer.java
│   │   │   ├── controller/
│   │   │   │   ├── MemberController.java
│   │   │   │   ├── CommonSettingsController.java
│   │   │   │   └── PdfController.java
│   │   │   ├── dto/
│   │   │   │   └── PdfGenerateRequest.java
│   │   │   ├── entity/
│   │   │   │   ├── Member.java
│   │   │   │   └── CommonSettings.java
│   │   │   ├── exception/
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   └── ResourceNotFoundException.java
│   │   │   ├── repository/
│   │   │   │   ├── MemberRepository.java
│   │   │   │   └── CommonSettingsRepository.java
│   │   │   └── service/
│   │   │       ├── MemberService.java
│   │   │       ├── MemberServiceImpl.java
│   │   │       ├── CommonSettingsService.java
│   │   │       ├── CommonSettingsServiceImpl.java
│   │   │       ├── PdfService.java
│   │   │       ├── PdfServiceImpl.java
│   │   │       └── LayoutLoader.java
│   │   └── resources/
│   │       ├── application.yaml
│   │       ├── fonts/
│   │       │   └── NotoSansJP-Regular.ttf
│   │       ├── positions/
│   │       │   └── converted_positions.yaml
│   │       └── templates/
│   │           └── template.pdf
│   └── test/
│       ├── java/com/example/CareDocWeb/
│       │   ├── CareDocWebApplicationTests.java
│       │   ├── controller/
│       │   │   ├── MemberControllerTest.java
│       │   │   ├── CommonSettingsControllerTest.java
│       │   │   └── PdfControllerTest.java
│       │   └── service/
│       │       ├── MemberServiceImplTest.java
│       │       ├── CommonSettingsServiceImplTest.java
│       │       └── PdfServiceImplTest.java
│       └── resources/
│           └── application.yaml
├── pom.xml
├── mvnw / mvnw.cmd
└── README.md
```

## 🔗 API エンドポイント

### 利用者

| メソッド | パス | ステータス | 機能 |
|---------|------|-----------|------|
| GET | /api/members | 200 | 利用者一覧取得 |
| GET | /api/members/{id} | 200 / 404 | 利用者詳細取得 |
| POST | /api/members | 201 | 利用者登録 |
| PUT | /api/members/{id} | 200 / 404 | 利用者更新 |
| DELETE | /api/members/{id} | 204 / 404 | 利用者削除 |

### 共通データ

| メソッド | パス | ステータス | 機能 |
|---------|------|-----------|------|
| GET | /api/settings | 200 / 404 | 共通設定取得 |
| PUT | /api/settings | 200 | 共通設定更新 |

### PDF生成

| メソッド | パス | ステータス | 機能 |
|---------|------|-----------|------|
| POST | /api/pdf/generate | 200 (application/pdf) | PDF生成・ダウンロード |

## 🗄 DBテーブル

| テーブル | 説明 |
|----------|------|
| `members` | 利用者ごとに1レコード（被保険者番号、氏名、生年月日、介護度など） |
| `common_settings` | 事業所全体で1レコード（調査先、施設、代理人、クリニック情報） |

※ 詳細は [DB設計書](docs/CareDocWeb%20DB設計書.md) を参照

## 🧪 テスト

全79件パス（JUnit 5 + Mockito + MockMvc）

| テストクラス | テスト数 | 対象 |
|---|---|---|
| CareDocWebApplicationTests | 1 | コンテキスト起動 |
| MemberControllerTest | 18 | 利用者API（HTTP検証） |
| CommonSettingsControllerTest | 10 | 共通設定API（HTTP検証） |
| PdfControllerTest | 4 | PDF生成API（HTTP検証） |
| MemberServiceImplTest | 25 | 利用者サービス（ロジック検証） |
| CommonSettingsServiceImplTest | 16 | 共通設定サービス（ロジック検証） |
| PdfServiceImplTest | 5 | PDF生成サービス（バイナリ生成検証） |

```powershell
mvn clean test
```

## 🚀 開発フェーズ

| Phase | 内容 | 状態 |
|-------|------|------|
| 1 | 利用者選択 → PDF生成（インメモリDB, 認証なし, ローカル実行） | ✅ 完了 |
| 2 | CRUD + Supabase接続 + S3/EC2デプロイ | ⬜ |
| 3 | 認証（Supabase Auth）, プレビュー, CI/CD | ⬜ |

## 🏗 ビルド・実行方法

※ JAVA_HOME には Java 21 JDK のパス設定が必要です

```powershell
cd C:\Users\dghy1\IdeaProjects\CareDocWeb

# ビルド
mvn clean package

# 実行
mvn spring-boot:run
```

アプリ起動後：
- `http://localhost:8080/api/members` — 利用者一覧（JSON）
- `http://localhost:8080/h2-console` — H2コンソール（JDBC URL: `jdbc:h2:mem:caredoc`）

### PDF生成の動作確認

```powershell
# 利用者一覧からIDを取得
(Invoke-WebRequest http://localhost:8080/api/members -UseBasicParsing).Content

# PDF生成
Invoke-WebRequest -Uri "http://localhost:8080/api/pdf/generate" `
  -Method POST `
  -ContentType "application/json" `
  -Body '{"memberId":"取得したUUID","applicationYear":2026,"applicationMonth":7,"applicationDay":2}' `
  -OutFile "output.pdf" `
  -UseBasicParsing
```

## 💰 運用コスト（見込み）

| 項目 | 月額 |
|------|------|
| Supabase（Free） | $0 |
| S3（静的ホスティング） | ほぼ $0 |
| EC2（t3.micro, 無料枠後） | $8〜15 |

## 🧑‍💻 留意点
※1. 本テンプレートは、東京都中央区が公開している介護認定申請書の様式を参考に作成したものです。<br>
     正式な手続きの際には、中央区が提供する最新の書式をご使用くださいますようお願いいたします。<br>
     なお、本書類の利用により生じた損害等について、作成者は一切の責任を負いません。

※2．医療保険と特定疾病名の項目は、第2号被保険者向けの記載欄で、ユースケースが少ない事から、<br>
   現時点では自動転記の対象としておりません。

※3．個人番号は、現時点では記載が求められないことが多いため、自動転記の対象としていません。
