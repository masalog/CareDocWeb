# CareDoc Web版 要件定義書

## 目的

```
デスクトップ版をWeb化し、ブラウザから申請書PDFを作成できるようにする。
```

---

## 機能

```
| 機能                         | 説明                           |
|------------------------------|--------------------------------|
| 利用者選択 → PDF生成         | 名前を選んで申請書PDFをダウンロード |
| 申請年月日の指定             | カレンダーから選択               |
| 変更更新理由の入力           | 任意                           |
| 利用者の登録・編集・削除     | Web画面からCRUD（管理画面のみ）  |
| 共通データの編集             | 事業所・担当者情報（管理画面のみ）|
| 管理画面ログイン認証         | Amazon Cognito（/api/admin/** 配下はAPI Gateway側で認証必須） |
```

---

## 技術スタック

```
| 項目             | 技術                                      |
|------------------|-------------------------------------------|
| フロントエンド   | HTML + バニラ JavaScript + CSS（S3 ホスティング） |
| バックエンド     | Java 21 + Spring Boot 4.x（JSON を返す REST API） |
| DB               | Supabase（PostgreSQL）                    |
| ORM              | Spring Data JPA                           |
| PDF生成          | Apache PDFBox（既存ロジック流用）          |
| インフラ         | AWS S3 + CloudFront（フロント）+ Lambda + API Gateway（API）  |
```

---

## システム構成

```
ブラウザ → CloudFront（唯一の公開エンドポイント）
              ├─ /*             → S3（HTML + バニラ JavaScript + CSS）
              └─ /api/*         → API Gateway → Lambda（Spring Boot API）
                   └─ /api/admin/* → Cognito Authorizer による認証必須
                                            ↓ JPA
                                         Supabase（PostgreSQL）
                                            ↑ SSM Parameter Store
                                         （DB接続情報を実行時取得）
```

---

## DB テーブル

テーブル構成・カラム定義は [DB設計書](CareDocWeb_DB設計書.md) を参照。

---

## 進め方

```
| Phase | 内容                                                  |
|-------|-------------------------------------------------------|
| 1     | 利用者選択 → PDF生成（インメモリデータ, 認証なし, ローカル実行）✅ |
| 2     | CRUD + Supabase接続 ✅ / S3+CloudFrontデプロイ ✅ / Lambda+API Gatewayデプロイ ✅ |
| 3     | CloudFront統合（/api/* ルーティング）✅ / PDF等バイナリ配信（binaryMediaTypes）✅ / 管理画面認証（Cognito）✅ / CI/CD（CodeBuild + CodePipeline）✅ |
```

---

## コスト

```
| 項目                        | 月額     |
|-----------------------------|----------|
| Supabase（Free）            | $0       |
| S3 + CloudFront（静的ホスティング + CDN） | ほぼ $0（低トラフィック時） |
| Lambda + API Gateway        | ほぼ $0（無料枠内・低トラフィック時） |
```
