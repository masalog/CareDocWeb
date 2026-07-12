# CareDoc Web版 DB設計書（ER図）

## テーブル構成

```
┌─────────────────────────────────┐
│           members               │
├─────────────────────────────────┤
│ id (PK)          UUID           │
│ insurance_id_number  TEXT       │
│ name                 TEXT       │
│ furigana             TEXT       │
│ birth_year           INT        │
│ birth_month          INT        │
│ birth_day            INT        │
│ gender               TEXT       │
│ address              TEXT       │
│ phone                TEXT       │
│ care_level           TEXT       │
│ start_year           INT        │
│ start_month          INT        │
│ start_day            INT        │
│ end_year             INT (null) │
│ end_month            INT (null) │
│ end_day              INT (null) │
│ institution_year     INT (null) │
│ institution_month    INT (null) │
│ institution_day      INT (null) │
│ created_at           TIMESTAMP  │
│ updated_at           TIMESTAMP  │
└─────────────────────────────────┘

┌─────────────────────────────────┐
│       common_settings           │
├─────────────────────────────────┤
│ id (PK)          UUID           │
│ survey_address       TEXT       │
│ survey_phone         TEXT       │
│ facility_name        TEXT       │
│ facility_phone       TEXT       │
│ institution_name     TEXT       │
│ institution_address  TEXT       │
│ agent_name           TEXT       │
│ agent_postal         TEXT       │
│ agent_address        TEXT       │
│ agent_phone          TEXT       │
│ doctor_name          TEXT       │
│ clinic_name          TEXT       │
│ clinic_postal        TEXT       │
│ clinic_address       TEXT       │
│ clinic_phone         TEXT       │
│ created_at           TIMESTAMP  │
│ updated_at           TIMESTAMP  │
└─────────────────────────────────┘
```

---

## リレーション

```
members と common_settings は独立（外部キーなし）

members       : 利用者ごとに1レコード
common_settings : 事業所全体で1レコード（設定値）
```

---

## 備考

```
- PKはUUID（Supabaseのデフォルト）
- created_at / updated_at は Supabase が自動管理
- カラム名は既存の Kotlin データクラス（Member, CommonData）に対応
- NULL許可のカラムは認定終了日・施設入所日（未設定の場合あり）
```
