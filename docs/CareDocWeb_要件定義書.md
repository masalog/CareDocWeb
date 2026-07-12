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
| 利用者の登録・編集・削除     | Web画面からCRUD                 |
| 共通データの編集             | 事業所・担当者情報               |
| ログイン認証                 | Supabase Auth                  |
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
              ├─ /*     → S3（HTML + バニラ JavaScript + CSS）
              └─ /api/* → API Gateway → Lambda（Spring Boot API）
                                            ↓ JPA
                                         Supabase（PostgreSQL）
                                            ↑ SSM Parameter Store
                                         （DB接続情報を実行時取得）
```

---

## DB テーブル

```
members（利用者）
  id, 被保険者番号, 氏名, フリガナ, 生年月日（年/月/日）, 性別,
  住所, 電話番号, 介護度, 認定開始日（年/月/日）, 認定終了日（年/月/日）,
  施設入所日（年/月/日）

common_settings（共通）
  id, 調査先住所, 調査先電話, 施設名, 施設電話, 医療機関名, 医療機関住所,
  代理人氏名, 代理人郵便番号, 代理人住所, 代理人電話, 主治医氏名,
  クリニック名, クリニック郵便番号, クリニック住所, クリニック電話
```

---

## 進め方

```
| Phase | 内容                                                  |
|-------|-------------------------------------------------------|
| 1     | 利用者選択 → PDF生成（インメモリデータ, 認証なし, ローカル実行）✅ |
| 2     | CRUD + Supabase接続 ✅ / S3+CloudFrontデプロイ ✅ / Lambda+API Gatewayデプロイ ✅ |
| 3     | CloudFront統合（/api/* ルーティング）✅ / PDF等バイナリ配信（binaryMediaTypes）✅ → 認証（Supabase Auth）, プレビュー, CI/CD |
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
