# CareDocWeb

CareDocWeb は、データベースの情報を PDF に転記して、介護認定申請書を自動作成する Web アプリケーションです。デスクトップアプリ [CareDoc](https://github.com/masalog/CareDoc) の Web 移行版になります。

公開用URL（CloudFront）  
https://dre5onrtbrgty.cloudfront.net

APIドキュメント（OpenAPI / Swagger UI）  
https://dre5onrtbrgty.cloudfront.net/swagger-ui/index.html

CI/CD の実行状況を確認する aws コマンド（PowerShell 向け）
```powershell
aws codepipeline get-pipeline-state --name CareDocWebPipeline --query "stageStates[].{Stage:stageName,Status:latestExecution.status}"
```

---

## 🏗 Lambda のコールドスタート対策

コスト削減のため EC2 ではなく Lambda を採用したが、Java（Spring Boot）は起動が重く、
コールドスタート時に約 7 秒かかる問題が発生。

Lambda SnapStart ＋ EventBridge cron による定期ウォームアップで、
**95% 以上のアクセスを 0.1 秒以内**（コールド時も典型約 1.2 秒）に改善。
詳細は [Lambda高速化報告書](docs/CareDocWeb_Lambda高速化報告書.md) を参照。

---


## ✨ 主な機能
- プルダウンから名前と申請年月日を選択
- 変更更新理由の入力（変更更新の場合のみ）
- 利用者の登録・編集・削除（管理画面から）
- 共通データ（事業所・担当者情報）の編集
- 介護認定申請書 PDF の生成・ダウンロード

---

## 🛠 技術スタック

| 分類 | 技術 |
|------|------|
| フロントエンド | HTML / CSS / JavaScript |
| バックエンド | Java 21 / Spring Boot 4.1 |
| データベース | Supabase (PostgreSQL) / H2 |
| データアクセス | Spring Data JPA |
| PDF生成 | Apache PDFBox 3.0.4 |
| テスト | JUnit 5 / Mockito / MockMvc |
| インフラ | AWS Lambda (SnapStart) / API Gateway (REST API) / S3 / CloudFront |
| IaC | AWS CDK (Java) |
| 監視・運用 | EventBridge（定期 Health Check） |
| ビルド | Maven 4 |
| 開発環境 | IntelliJ IDEA Community Edition |

---

## 📐 システム構成図

![システム構成図](docs/CareDocWeb_システム構成図.drawio.png)

---

## 📂 プロジェクト構成

```text
CareDocWeb/
├── docs/                        # 設計資料
├── src/
│   ├── main/
│   │   ├── java/.../CareDocWeb/
│   │   │   ├── config/          # AWS SSM から DB 接続情報を取得
│   │   │   ├── controller/      # REST API
│   │   │   ├── dto/             # リクエスト DTO
│   │   │   ├── entity/          # エンティティ(Member, CommonSettings)
│   │   │   ├── exception/       # 共通例外ハンドリング
│   │   │   ├── repository/      # Spring Data JPA リポジトリ
│   │   │   └── service/         # 業務ロジック・PDF 生成
│   │   └── resources/
│   │       ├── static/          # フロントエンド
│   │       ├── fonts/           # PDF 埋め込み用日本語フォント
│   │       ├── positions/       # PDF 座標定義
│   │       └── templates/       # PDF テンプレート
│   └── test/                    
├── cdk/                   
│   └── src/.../cdk/             # CDK スタック
├── pom.xml
├── README.md
└── SECURITY.md
```

---

## 📚 詳細ドキュメント

さらに詳しい設計内容は `docs/` 配下を参照してください。

- [要件定義書](docs/CareDocWeb_要件定義書.md)
- [API設計書](docs/CareDocWeb_API設計書.md)
- [DB設計書](docs/CareDocWeb_DB設計書.md)
- [インフラ設計書](docs/CareDocWeb_インフラ設計書.md)
- [PDF生成サービス実装方針](docs/CareDocWeb_PDF生成サービス実装方針.md)

---

## 🧪 テスト

全114件パス（JUnit 5 + Mockito + MockMvc）

| テストクラス | テスト数 | 対象 |
|---|---|---|
| CareDocWebApplicationTests | 1 | コンテキスト起動 |
| MemberControllerTest | 18 | 利用者API（HTTP検証） |
| CommonSettingsControllerTest | 10 | 共通設定API（HTTP検証） |
| PdfControllerTest | 22 | PDF生成API（HTTP検証） |
| HealthControllerTest | 3 | ヘルスチェックAPI（HTTP検証） |
| MemberServiceImplTest | 25 | 利用者サービス（ロジック検証） |
| CommonSettingsServiceImplTest | 16 | 共通設定サービス（ロジック検証） |
| PdfServiceImplTest | 19 | PDF生成サービス（バイナリ生成検証） |

---

## 💰 運用コスト

Lambda・API Gateway・S3・CloudFront は無料枠内、Supabase も Free プランで運用しているため、運用コストはほぼ無料です。

---

## ⚠️ 注意事項

※1. 本テンプレートは、東京都中央区が公開している介護認定申請書の様式を参考に作成したものです。  
正式な手続きの際には、中央区が提供する最新の書式をご使用くださいますようお願いいたします。  
なお、本書類の利用により生じた損害等について、作成者は一切の責任を負いません。

※2．医療保険と特定疾病名の項目は、第2号被保険者向けの記載欄で、ユースケースが少ない事から、現時点では自動転記の対象としておりません。

※3．個人番号は、現時点では記載が求められないことが多いため、自動転記の対象としていません。
