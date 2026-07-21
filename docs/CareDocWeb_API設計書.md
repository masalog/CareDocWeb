# CareDoc Web版 API設計書

## 概要

```
Java 21 + Spring Boot 4.x による REST API（Lambda + API Gateway で稼働）。
利用者・共通データの取得/更新は JSON を返す。
フロントエンド（バニラ JavaScript / app.js）が JSON を受け取り、DOM を組み立てる。
PDF生成時はバイナリ（application/pdf）を返す。

公開API（/api/members の一覧取得のみ・/api/pdf/** ・/api/health）は認証不要。
管理API（/api/admin/**）は API Gateway の Cognito Authorizer により認証必須で、
有効な ID トークンがないリクエストはアプリに到達しない。
また READ_ONLY_MODE 有効時は管理APIの登録・更新・削除系が 403 を返す。
```

---

## エンドポイント一覧

### 利用者

```
GET    /api/members                公開API。利用者一覧を JSON 配列で返す
GET    /api/admin/members/{id}     管理API（Cognito認証必須）。利用者詳細を JSON で返す
POST   /api/admin/members          管理API（Cognito認証必須）。利用者を登録（登録結果を JSON で返す）
PUT    /api/admin/members/{id}     管理API（Cognito認証必須）。利用者を更新（更新結果を JSON で返す）
DELETE /api/admin/members/{id}     管理API（Cognito認証必須）。利用者を削除
```

### 共通データ

```text
GET    /api/admin/settings         管理API（Cognito認証必須）。共通データを JSON で返す
PUT    /api/admin/settings         管理API（Cognito認証必須）。共通データを更新（更新結果を JSON で返す）
```

### PDF生成

```
POST   /api/pdf/generate     PDFを生成してダウンロード

リクエストボディ:
{
  "memberId": "uuid",
  "applicationYear": 2026,
  "applicationMonth": 6,
  "applicationDay": 29,
  "changeReason": "状態悪化のため"  // 任意
}

レスポンス: application/pdf（バイナリ）
```

### ヘルスチェック

```
GET    /api/health          死活監視・ウォームアップ用

レスポンス（application/json）:
{ "status": "ok", "db": "up" }   // DB接続を connection.isValid で確認

※ DB接続に失敗しても 200 + db:"down" を返す（ウォームアップ用途のため
  エンドポイント自体は生かす）。EventBridge cron から定期的に叩くことで、
  Lambda のアイドル回収によるコールドスタートを抑制する（保証はなく、ベストエフォート）。
```

---

## レスポンス形式

```
通常のリクエスト → Content-Type: application/json（JSON）
PDF生成        → Content-Type: application/pdf（バイナリ）
エラー時       → HTTP ステータスコード（フロントの app.js が res.ok で判定しメッセージ表示）
```

---

## ステータスコード

```
200  成功
201  登録成功
400  バリデーションエラー
401  未認証（管理APIにIDトークンなしでアクセス）
403  閲覧専用モード（READ_ONLY_MODE）のため書き込み不可
404  対象データなし
500  サーバーエラー
```

---

## オリジン設定

```text
CloudFront が静的配信（S3）とAPI（API Gateway → Lambda）を同一ドメイン配下に
統合するため、フロントとAPIは同一オリジンとなり CORS設定は不要。
フロント（app.js）は相対パス `/api/...` でAPIを呼ぶ。
```
