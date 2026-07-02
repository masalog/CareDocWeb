# CareDocWeb PDF生成サービス 実装方針

## 概要

CareDoc（デスクトップ版）の PDF 生成ロジック（Kotlin + PDFBox）を
CareDocWeb（Spring Boot）に Java で移植する。

テンプレート PDF に利用者データ・共通設定・申請日等を転記し、
完成した PDF をバイナリレスポンスとしてクライアントに返す。

---

## API仕様（API設計書より）

```
POST /api/pdf/generate

リクエストボディ:
{
  "memberId": "uuid",
  "applicationYear": 2026,
  "applicationMonth": 7,
  "applicationDay": 1,
  "changeReason": "状態悪化のため"  // 任意
}

レスポンス:
  Content-Type: application/pdf
  ボディ: PDFバイナリ
```

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
   - 一時ファイルとしてPDFを出力
       ↓
5. PDFバイナリをレスポンスとして返却（PdfController）
```

---

## 作成するファイル

| ファイル | パッケージ | 役割 |
|----------|-----------|------|
| `PdfGenerateRequest.java` | `dto` | リクエストボディの受け皿（DTO） |
| `PdfService.java` | `service` | PDF生成ロジック（インタフェース） |
| `PdfServiceImpl.java` | `service` | PDF生成ロジック（実装） |
| `PdfController.java` | `controller` | `/api/pdf/generate` エンドポイント |
| `PdfServiceImplTest.java` | `test/service` | PDF生成のユニットテスト |
| `PdfControllerTest.java` | `test/controller` | コントローラーの統合テスト |

---

## リソースファイル（CareDocから移植）

| ファイル | 配置先 | 説明 |
|----------|--------|------|
| `template.pdf` | `src/main/resources/templates/` | 申請書テンプレート（転記先） |
| `converted_positions.yaml` | `src/main/resources/positions/` | 各フィールドの座標（x, y, fontSize） |
| `NotoSansJP-Regular.ttf` | `src/main/resources/fonts/` | 日本語フォント |

---

## 既存ロジックからの移植ポイント

### CareDoc（Kotlin）の `PdfEditor.editPdf()` のロジック

1. テンプレートPDFを InputStream から読み込み
2. 日本語フォント（NotoSansJP）をロード
3. 座標YAML（`converted_positions.yaml`）からフィールド位置を取得
4. `PDPageContentStream` を使って各座標にテキストを描画
5. 性別・介護度は「〇」を座標に描画（`drawCircle`）
6. 一時ファイルに保存して返却

### Java移植時の主な変更点

| Kotlin | Java |
|--------|------|
| `PDDocument.load(inputStream)` | `Loader.loadPDF(inputStream)` (PDFBox 3.x) |
| `.use { }` | try-with-resources |
| `?.let { }` | `if (x != null) { }` または `Optional` |
| `File.createTempFile()` | `ByteArrayOutputStream` に直接書き出し（ファイル不要） |

---

## DTO設計

### PdfGenerateRequest

```java
public class PdfGenerateRequest {
    private UUID memberId;            // 必須
    private Integer applicationYear;  // 必須
    private Integer applicationMonth; // 必須
    private Integer applicationDay;   // 必須
    private String changeReason;      // 任意
}
```

---

## PdfService インタフェース

```java
public interface PdfService {
    /**
     * 申請書PDFを生成する。
     *
     * @param member 利用者データ
     * @param settings 共通設定データ
     * @param request PDF生成リクエスト（申請日・変更理由）
     * @return PDF のバイト配列
     */
    byte[] generate(Member member, CommonSettings settings, PdfGenerateRequest request);
}
```

---

## PdfController エンドポイント

```java
@PostMapping("/api/pdf/generate")
public ResponseEntity<byte[]> generate(@RequestBody PdfGenerateRequest request) {
    Member member = memberService.findById(request.getMemberId());
    CommonSettings settings = commonSettingsService.find();
    byte[] pdf = pdfService.generate(member, settings, request);

    return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=output.pdf")
            .contentType(MediaType.APPLICATION_PDF)
            .body(pdf);
}
```

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
| Apache PDFBox | PDF読み込み・テキスト描画・保存 | 3.x（Spring Boot 4対応） |
| SnakeYAML | 座標YAMLの読み込み | Spring Boot に同梱 |

### pom.xml に追加が必要

```xml
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>3.0.4</version>
</dependency>
```

※ CareDoc は PDFBox 2.0.30 だが、Web版では PDFBox 3.x に移行する。
  API が一部変更されている（`PDDocument.load()` → `Loader.loadPDF()` 等）。

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

---

## 実装順序

1. リソースファイル配置（template.pdf, YAML, フォント）
2. `PdfGenerateRequest`（DTO）作成
3. `PdfService` / `PdfServiceImpl` 作成
4. `PdfController` 作成
5. テスト作成
6. 動作確認（H2 + 初期データで PDF 生成）

---

## 注意事項

- PDFBox 3.x では `PDDocument.load()` が非推奨。`Loader.loadPDF()` を使用すること
- フォント・テンプレートは `classpath:` から読み込む（ファイルシステム依存しない）
- PDF生成は `ByteArrayOutputStream` に直接出力し、一時ファイルを使わない設計にする
- PDFBox 3.x の `PDPageContentStream` のコンストラクタ引数が変更されている可能性あり
