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
| バックエンド | Java 21 + Spring Boot 4.1.0（HTML断片を返すAPI） |
| DB | Supabase（PostgreSQL） |
| ORM | Spring Data JPA |
| PDF生成 | Apache PDFBox（既存ロジック流用） |
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
│   └── CareDocWeb DB設計書.md
├── src/
│   ├── main/
│   │   ├── java/com/example/CareDocWeb/
│   │   │   └── CareDocWebApplication.java
│   │   └── resources/
│   │       ├── application.yaml
│   │       ├── static/
│   │       └── templates/
│   └── test/
├── pom.xml
├── mvnw / mvnw.cmd
└── README.md
```

## 🔗 API エンドポイント

### 利用者

```
GET    /api/members          利用者一覧のHTML断片を返す
GET    /api/members/{id}     利用者詳細のHTML断片を返す
POST   /api/members          利用者を登録
PUT    /api/members/{id}     利用者を更新
DELETE /api/members/{id}     利用者を削除
```

### 共通データ

```
GET    /api/settings         共通データのHTML断片を返す
PUT    /api/settings         共通データを更新
```

### PDF生成

```
POST   /api/pdf/generate     PDFを生成してダウンロード
```

## 🗄 DBテーブル

| テーブル | 説明 |
|----------|------|
| `members` | 利用者ごとに1レコード（被保険者番号、氏名、生年月日、介護度など） |
| `common_settings` | 事業所全体で1レコード（調査先、施設、代理人、クリニック情報） |

※ 詳細は [DB設計書](docs/CareDocWeb%20DB設計書.md) を参照

## 🚀 開発フェーズ

| Phase | 内容 |
|-------|------|
| 1 | 利用者選択 → PDF生成（インメモリデータ, 認証なし, ローカル実行） |
| 2 | CRUD + Supabase接続 + S3/EC2デプロイ |
| 3 | 認証（Supabase Auth）, プレビュー, CI/CD |

## 🏗 ビルド・実行方法

※ JAVA_HOME には Java 21 JDK のパス設定が必要です

```powershell
cd C:\Users\dghy1\IdeaProjects\CareDocWeb

# ビルド
mvn clean package

# 実行
mvn spring-boot:run
```

アプリ起動後、`http://localhost:8080` にアクセスしてください。

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
