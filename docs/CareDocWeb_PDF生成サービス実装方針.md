# CareDocWeb PDF生成サービス 実装方針

## 概要

CareDoc（デスクトップ版）の PDF 生成ロジック（Kotlin + PDFBox）を
CareDocWeb（Spring Boot）に Java で移植する。

テンプレート PDF に利用者データ・共通設定・申請日等を転記し、
完成した PDF をバイナリレスポンスとしてクライアントに返す。

---

## API仕様

`POST /api/pdf/generate` の詳細は [API設計書](CareDocWeb_API設計書.md) を参照。

---

## 処理フロー

```
1. リクエスト受信（PdfController）
       ↓
2. memberId で利用者データを取得（MemberService.findById）
       ↓
3. 共通設定を取得（CommonSettingsService.find）
       ↓
4. PDF生成（PdfService.generate）
   - テンプレートPDFを読み込み
   - 座標YAMLに基づいてフィールドにテキスト転記
   - ByteArrayOutputStream にPDFを出力（一時ファイル不使用）
       ↓
5. PDFバイナリをレスポンスとして返却（PdfController）
```

---

## リソースファイル

| ファイル | 配置先 | 説明 |
|----------|--------|------|
| `template.pdf` | `src/main/resources/templates/` | 申請書テンプレート（転記先） |
| `converted_positions.yaml` | `src/main/resources/positions/` | 各フィールドの座標（x, y, fontSize） |
| `NotoSansJP-Regular.ttf` | `src/main/resources/fonts/` | 日本語フォント |

---

## 実装のポイント

コード全体は `dto/PdfGenerateRequest.java`・`service/PdfService.java`（`PdfServiceImpl`）・
`controller/PdfController.java` を参照。設計上の要点のみ以下に記す。

- **PdfGenerateRequest**: `memberId`・`applicationYear/Month/Day` は必須、`changeReason` は任意。`@Valid` で検証
- **PdfService.generate()**: テンプレートPDFを読み込み、座標YAML（`converted_positions.yaml`）の位置にテキストを描画し、`ByteArrayOutputStream` に直接出力（一時ファイル不使用）。性別・介護度は該当座標に「〇」を描画
- **PdfController**: 申請日＋利用者名から `〇〇年〇月〇日 氏名様 介護認定申請書.pdf` 形式のファイル名を生成し、日本語のため `URLEncoder` でUTF-8エンコードして `filename*=UTF-8''...` で指定
- **PDFBox 3.x 移行**: `PDDocument.load()` はPDFBox 3.0で削除されたため `Loader.loadPDF()` を使用。フォント・テンプレートは `classpath:` から読み込みファイルシステムに依存しない

---

## 座標YAML の構造

```yaml
fields:
  Insurance ID Number: {x: 211.55, y: 713.93, fontSize: 12}
  name: {x: 211.55, y: 606.94, fontSize: 16}
  furigana: {x: 201.96, y: 637.17, fontSize: 12}
  birthYear: {x: 423.10, y: 617.98, fontSize: 12}
  ...
```

各フィールドの座標は CareDoc の `converted_positions.yaml` をそのまま流用する。

---

## 依存ライブラリ

| ライブラリ | 用途 | バージョン |
|-----------|------|-----------|
| Apache PDFBox | PDF読み込み・テキスト描画・保存 | 3.0.4 |
| SnakeYAML | 座標YAMLの読み込み | Spring Boot に同梱 |

---

## テスト方針

### PdfServiceImplTest（ユニットテスト）

| カテゴリ | テスト内容 |
|---------|-----------|
| 正常系 | 利用者 + 共通設定 + 申請日 → PDFバイナリが返る |
| 正常系 | 変更理由ありで生成できる |
| 正常系 | 変更理由なし（null）でも生成できる |
| 境界値 | 全フィールドがNULL許容項目のみの利用者でも生成できる |
| 異常系 | テンプレートPDFが見つからない場合、例外をスロー |

### PdfControllerTest（統合テスト）

| カテゴリ | テスト内容 |
|---------|-----------|
| 正常系 | POST /api/pdf/generate → 200 + Content-Type: application/pdf |
| 異常系 | memberId が存在しない → 404 |
| 異常系 | リクエストボディが不正 → 400 |
