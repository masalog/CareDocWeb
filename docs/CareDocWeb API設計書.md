# CareDoc Web版 API設計書

## 概要

```
Java 21 + Spring Boot 4.x による REST API（Lambda + API Gateway で稼働）。
利用者・共通データの取得/更新は JSON を返す。
フロントエンド（バニラ JavaScript / app.js）が JSON を受け取り、DOM を組み立てる。
PDF生成時はバイナリ（application/pdf）を返す。
```

---

## エンドポイント一覧

### 利用者

```
GET    /api/members          利用者一覧を JSON 配列で返す
GET    /api/members/{id}     利用者詳細を JSON で返す
POST   /api/members          利用者を登録（登録結果を JSON で返す）
PUT    /api/members/{id}     利用者を更新（更新結果を JSON で返す）
DELETE /api/members/{id}     利用者を削除
```

### 共通データ

```
GET    /api/settings         共通データを JSON で返す
PUT    /api/settings         共通データを更新（更新結果を JSON で返す）
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
  エンドポイント自体は生かす）。GitHub Actions cron から定期的に叩き、
  Lambda をアイドル回収させずウォーム状態に保つ。
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
404  対象データなし
500  サーバーエラー
```

---

## オリジン設定（CloudFront統合により CORS 不要）

```
CloudFront を唯一の公開エンドポイントとし、静的配信（S3）と
API（API Gateway → Lambda）を同一ドメイン配下に統合する。

フロントとAPIが同一オリジン（CloudFrontのドメイン）となるため、
ブラウザからのAPI呼び出しはクロスオリジンにならず、CORS設定は不要。

- フロント（app.js）は相対パス `/api/...` でAPIを呼ぶ
- CloudFront が `/api/*` を API Gateway（prod ステージ）へルーティング
- `/*` は S3 へルーティング（OAC 経由・S3直接アクセスは遮断）
```
