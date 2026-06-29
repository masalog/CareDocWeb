# CareDoc Web版 API設計書

## 概要

```
Spring Boot が HTML断片を返す API。
htmx からのリクエストに対し、HTMLパーツを返却する。
PDF生成時はバイナリ（application/pdf）を返す。
```

---

## エンドポイント一覧

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

リクエストボディ:
{
  "memberId": 1,
  "applicationYear": 2026,
  "applicationMonth": 6,
  "applicationDay": 29,
  "changeReason": "状態悪化のため"  // 任意
}

レスポンス: application/pdf（バイナリ）
```

---

## レスポンス形式

```
通常のリクエスト → Content-Type: text/html（HTML断片）
PDF生成        → Content-Type: application/pdf（バイナリ）
エラー時       → HTTP ステータスコード + エラーメッセージHTML
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

## CORS設定

```
S3（フロント）からEC2（API）へリクエストするため、
Spring Boot側でCORSを許可する必要がある。

許可オリジン: S3のホスティングURL
許可メソッド: GET, POST, PUT, DELETE
```
