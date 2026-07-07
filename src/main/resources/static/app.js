// ========================================
// セキュリティ / DOMユーティリティ
// ========================================

/**
 * DOM-based XSS 対策：HTML特殊文字をエスケープする。
 * innerHTML にサーバー由来のテキスト（利用者名など）を埋め込む際は、
 * 必ずこの関数を通すこと。
 *
 * @param {*} value - エスケープ対象（null/undefined/数値も許容）
 * @returns {string} エスケープ済み文字列
 */
function escapeHtml(value) {
    if (value === null || value === undefined) return '';
    return String(value)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

/**
 * value と表示テキストを指定して option 要素を安全に生成する。
 * textContent を使うため、表示テキストに含まれるHTMLは実行されない。
 *
 * @param {string|number} value - option の value 属性
 * @param {string} text - 画面に表示するテキスト（自動でエスケープ相当の安全処理）
 * @returns {HTMLOptionElement}
 */
function createOption(value, text) {
    const option = document.createElement('option');
    option.value = value;
    option.textContent = text;
    return option;
}

// ========================================
// セクション切り替え
// ========================================

/**
 * 表示セクションを切り替える
 * @param {string} name - セクション名 ('members' | 'settings')
 * @param {HTMLElement} btn - クリックされたナビボタン要素
 */
function showSection(name, btn) {
    document.querySelectorAll('.section').forEach(s => s.classList.add('hidden'));
    document.getElementById('section-' + name).classList.remove('hidden');

    document.querySelectorAll('.nav-btn').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');

    if (name === 'pdf') loadMemberSelect();
    if (name === 'members') loadMembers();
    if (name === 'settings') loadSettings();
}

// ========================================
// PDF生成
// ========================================

/**
 * 利用者一覧を取得してセレクトボックスに反映
 * GET /api/members
 */
function loadMemberSelect() {
    fetch('/api/members')
        .then(res => res.json())
        .then(members => {
            const select = document.getElementById('member-select');
            // innerHTML文字列連結をやめ、textContentベースのoption生成でXSSを防止
            select.replaceChildren();
            select.appendChild(createOption('', '-- 選択してください --'));
            members.forEach(m => {
                select.appendChild(createOption(m.id, `${m.name}（${m.careLevel || '未設定'}）`));
            });
        });
}

// ========================================
// 申請年月日セレクトの制御
// ========================================

/**
 * 申請年・月・日のドロップダウンを初期化する。
 * - 申請年：当年・翌年の2択
 * - 申請月：1〜12月
 * - 申請日：選択中の年月に応じて末日（28/29/30/31）を動的生成
 * ページ読み込み時に一度だけ呼ぶ。
 */
function initDateSelects() {
    const yearSelect = document.getElementById('app-year');
    const monthSelect = document.getElementById('app-month');
    const daySelect = document.getElementById('app-day');
    if (!yearSelect || !monthSelect || !daySelect) return;

    const now = new Date();
    const currentYear = now.getFullYear();

    // 申請年：当年・翌年
    yearSelect.replaceChildren();
    [currentYear, currentYear + 1].forEach(y => {
        yearSelect.appendChild(createOption(y, `${y}年`));
    });

    // 月の選択肢を生成
    monthSelect.replaceChildren();
    for (let m = 1; m <= 12; m++) {
        monthSelect.appendChild(createOption(m, `${m}月`));
    }
    monthSelect.value = now.getMonth() + 1;

    // 年・月が変わったら日の選択肢を再生成
    yearSelect.addEventListener('change', updateDayOptions);
    monthSelect.addEventListener('change', updateDayOptions);

    // 初期表示：日の選択肢を生成（初期値は当日）
    updateDayOptions();
    daySelect.value = now.getDate();
}

/**
 * 選択中の年・月に応じて、申請日の選択肢を末日まで動的生成する。
 * new Date(year, month, 0).getDate() でその月の末日が求まる（うるう年も自動対応）。
 * 再生成前の選択日が新しい末日を超える場合は末日に丸める。
 */
function updateDayOptions() {
    const yearSelect = document.getElementById('app-year');
    const monthSelect = document.getElementById('app-month');
    const daySelect = document.getElementById('app-day');

    const year = parseInt(yearSelect.value);
    const month = parseInt(monthSelect.value);
    const lastDay = new Date(year, month, 0).getDate(); // その月の末日

    const prevDay = parseInt(daySelect.value) || 1;
    daySelect.replaceChildren();
    for (let d = 1; d <= lastDay; d++) {
        daySelect.appendChild(createOption(d, `${d}日`));
    }

    // 以前の選択日を可能な範囲で維持（末日超過時は末日に丸める）
    daySelect.value = Math.min(prevDay, lastDay);
}

// ページ読み込み時に日付セレクトを初期化
document.addEventListener('DOMContentLoaded', initDateSelects);

/** PDF生成・ダウンロード処理
 * 1. バリデーション（利用者選択・年月日入力）
 * 2. POST /api/pdf/generate にJSONリクエスト送信
 * 3. レスポンスのblobをダウンロードリンクとして生成
*/
function generatePdf() {
    const memberId = document.getElementById('member-select').value;
    const applicationYear = parseInt(document.getElementById('app-year').value);
    const applicationMonth = parseInt(document.getElementById('app-month').value);
    const applicationDay = parseInt(document.getElementById('app-day').value);
    const changeReason = document.getElementById('change-reason').value || null;

    // バリデーション
    if (!memberId) {
        showMessage('pdf-message', '利用者を選択してください', 'error');
        return;
    }
    if (!applicationYear || !applicationMonth || !applicationDay) {
        showMessage('pdf-message', '申請年月日を入力してください', 'error');
        return;
    }

    // リクエストボディ組み立て
    const body = { memberId, applicationYear, applicationMonth, applicationDay, changeReason };

    // PDF生成APIを呼び出し
    fetch('/api/pdf/generate', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
    })
    .then(res => {
        if (!res.ok) throw new Error('PDF生成に失敗しました');
        return res.blob();
    })
    .then(blob => {
        // Blobからダウンロードリンクを生成して自動クリック
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'output.pdf';
        a.click();
        window.URL.revokeObjectURL(url);
        showMessage('pdf-message', 'PDFを生成しました', 'success');
    })
    .catch(err => {
        showMessage('pdf-message', err.message, 'error');
    });
}

// ========================================
// 利用者管理
// ========================================

/**
 * 利用者一覧を取得してテーブル表示
 * GET /api/members
 */
function loadMembers() {
    fetch('/api/members')
        .then(res => res.json())
        .then(members => {
            const container = document.getElementById('member-list');
            if (members.length === 0) {
                const p = document.createElement('p');
                p.textContent = '登録されている利用者がいません。';
                container.replaceChildren(p);
                return;
            }
            // 各セルはescapeHtmlでエスケープし、操作ボタンはonclick属性を使わず
            // dataset + addEventListener でバインドすることで属性インジェクションを根本排除する
            let html = `<table class="member-table">
                <thead><tr>
                    <th>氏名</th><th>フリガナ</th><th>介護度</th><th>被保険者番号</th><th>操作</th>
                </tr></thead><tbody>`;
            members.forEach(m => {
                html += `<tr>
                    <td>${escapeHtml(m.name)}</td>
                    <td>${escapeHtml(m.furigana)}</td>
                    <td>${escapeHtml(m.careLevel)}</td>
                    <td>${escapeHtml(m.insuranceIdNumber)}</td>
                    <td class="actions">
                        <button class="btn btn-edit" data-id="${escapeHtml(m.id)}" data-action="edit">編集</button>
                        <button class="btn btn-danger" data-id="${escapeHtml(m.id)}" data-name="${escapeHtml(m.name)}" data-action="delete">削除</button>
                    </td>
                </tr>`;
            });
            html += '</tbody></table>';
            container.innerHTML = html;

            // 操作ボタンにイベントを委譲でバインド（インラインJSを使わない）
            container.querySelectorAll('button[data-action="edit"]').forEach(btn => {
                btn.addEventListener('click', () => editMember(btn.dataset.id));
            });
            container.querySelectorAll('button[data-action="delete"]').forEach(btn => {
                btn.addEventListener('click', () => deleteMember(btn.dataset.id, btn.dataset.name));
            });
        });
}

/**
 * 新規登録フォームを表示
 * フォームを空の状態にリセットして表示する
 */
function showMemberForm() {
    document.getElementById('member-form-container').classList.remove('hidden');
    document.getElementById('member-form-title').textContent = '利用者登録';
    document.getElementById('member-form').reset();
    document.getElementById('member-id').value = '';
}

/** 利用者フォームを非表示にする */
function hideMemberForm() {
    document.getElementById('member-form-container').classList.add('hidden');
}

/**
 * 利用者を保存（新規登録 or 更新）
 * - id が存在する場合: PUT /api/members/{id}（更新）
 * - id が空の場合: POST /api/members（新規登録）
 */
function saveMember() {
    const id = document.getElementById('member-id').value;
    const body = {
        insuranceIdNumber: document.getElementById('m-insurance').value || null,
        name: document.getElementById('m-name').value,
        furigana: document.getElementById('m-furigana').value || null,
        birthYear: parseInt(document.getElementById('m-birth-year').value) || null,
        birthMonth: parseInt(document.getElementById('m-birth-month').value) || null,
        birthDay: parseInt(document.getElementById('m-birth-day').value) || null,
        gender: document.getElementById('m-gender').value || null,
        careLevel: document.getElementById('m-care-level').value || null,
        address: document.getElementById('m-address').value || null,
        phone: document.getElementById('m-phone').value || null,
        startYear: parseInt(document.getElementById('m-start-year').value) || null,
        startMonth: parseInt(document.getElementById('m-start-month').value) || null,
        startDay: parseInt(document.getElementById('m-start-day').value) || null,
        endYear: parseInt(document.getElementById('m-end-year').value) || null,
        endMonth: parseInt(document.getElementById('m-end-month').value) || null,
        endDay: parseInt(document.getElementById('m-end-day').value) || null,
    };

    // バリデーション
    if (!body.name) {
        showMessage('member-message', '氏名は必須です', 'error');
        return;
    }

    const method = id ? 'PUT' : 'POST';
    const url = id ? `/api/members/${id}` : '/api/members';

    fetch(url, {
        method: method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
    })
    .then(res => {
        if (!res.ok) throw new Error('保存に失敗しました');
        return res.json();
    })
    .then(() => {
        showMessage('member-message', id ? '更新しました' : '登録しました', 'success');
        hideMemberForm();
        loadMembers(); // 一覧を再取得して最新化
    })
    .catch(err => {
        showMessage('member-message', err.message, 'error');
    });
}

/**
 * 編集フォームを表示
 * 選択された利用者のデータをAPIから取得してフォームにセットする
 * @param {string} id - 編集対象の利用者ID
 */
function editMember(id) {
    fetch(`/api/members/${id}`)
        .then(res => res.json())
        .then(m => {
            document.getElementById('member-form-container').classList.remove('hidden');
            document.getElementById('member-form-title').textContent = '利用者編集';
            document.getElementById('member-id').value = m.id;
            document.getElementById('m-insurance').value = m.insuranceIdNumber || '';
            document.getElementById('m-name').value = m.name || '';
            document.getElementById('m-furigana').value = m.furigana || '';
            document.getElementById('m-birth-year').value = m.birthYear || '';
            document.getElementById('m-birth-month').value = m.birthMonth || '';
            document.getElementById('m-birth-day').value = m.birthDay || '';
            document.getElementById('m-gender').value = m.gender || '';
            document.getElementById('m-care-level').value = m.careLevel || '';
            document.getElementById('m-address').value = m.address || '';
            document.getElementById('m-phone').value = m.phone || '';
            document.getElementById('m-start-year').value = m.startYear || '';
            document.getElementById('m-start-month').value = m.startMonth || '';
            document.getElementById('m-start-day').value = m.startDay || '';
            document.getElementById('m-end-year').value = m.endYear || '';
            document.getElementById('m-end-month').value = m.endMonth || '';
            document.getElementById('m-end-day').value = m.endDay || '';
        });
}

/**
 * 利用者を削除
 * 確認ダイアログで承認後、DELETE /api/members/{id} を実行
 * @param {string} id - 削除対象の利用者ID
 * @param {string} name - 確認ダイアログ表示用の氏名
 */
function deleteMember(id, name) {
    if (!confirm(`「${name}」を削除しますか？`)) return;

    fetch(`/api/members/${id}`, { method: 'DELETE' })
        .then(res => {
            if (!res.ok) throw new Error('削除に失敗しました');
            showMessage('member-message', `「${name}」を削除しました`, 'success');
            loadMembers(); // 一覧を再取得して最新化
        })
        .catch(err => {
            showMessage('member-message', err.message, 'error');
        });
}

// ========================================
// 共通設定
// ========================================

/**
 * 共通設定を取得してフォームに反映
 * GET /api/settings
 */
/**
 * 共通設定用：年・月・日のプルダウンを初期化する汎用関数。
 * index.html の申請年月日（initDateSelects）とは独立した実装。
 * - 年：当年を中心に前後の範囲を生成（未選択の空欄を許容）
 * - 月：1〜12月（空欄を許容）
 * - 日：選択中の年・月に応じて末日まで動的生成（うるう年も自動対応）
 *
 * @param {string} prefix - 各selectのid接頭辞（例: 's-institution' → s-institution-year/month/day）
 */

// 年月日プルダウンの初期化済みフラグ
const settingsDateInitFlags = {};

function initSettingsDateGroup(prefix) {

    // すでに初期化済みなら何もしない（イベントリスナーの重複防止）
    if (settingsDateInitFlags[prefix]) return;
    settingsDateInitFlags[prefix] = true;

    const yearSelect = document.getElementById(prefix + '-year');
    const monthSelect = document.getElementById(prefix + '-month');
    const daySelect = document.getElementById(prefix + '-day');
    if (!yearSelect || !monthSelect || !daySelect) return;

    const currentYear = new Date().getFullYear();

    // 年：当年を基準に過去30年〜翌年まで（空欄を先頭に）
    yearSelect.replaceChildren(createOption('', '--'));
    for (let y = currentYear + 1; y >= currentYear - 30; y--) {
        yearSelect.appendChild(createOption(y, y + '年'));
    }

    // 月：1〜12月（空欄を先頭に）
    monthSelect.replaceChildren(createOption('', '--'));
    for (let m = 1; m <= 12; m++) {
        monthSelect.appendChild(createOption(m, m + '月'));
    }

    // 年・月変更時に日を再生成
    const rebuildDays = () => updateSettingsDayOptions(prefix);
    yearSelect.addEventListener('change', rebuildDays);
    monthSelect.addEventListener('change', rebuildDays);

    // 初期の日選択肢を生成
    updateSettingsDayOptions(prefix);
}

/**
 * 選択中の年・月に応じて、日のプルダウンを末日まで動的生成する。
 * 年または月が未選択の場合は最大31日を仮表示する。
 *
 * @param {string} prefix - 各selectのid接頭辞
 */
function updateSettingsDayOptions(prefix) {
    const yearSelect = document.getElementById(prefix + '-year');
    const monthSelect = document.getElementById(prefix + '-month');
    const daySelect = document.getElementById(prefix + '-day');

    const year = parseInt(yearSelect.value);
    const month = parseInt(monthSelect.value);

    // 年・月が揃えば正確な末日、揃わなければ31日を仮採用
    const lastDay = (year && month) ? new Date(year, month, 0).getDate() : 31;

    const prevDay = parseInt(daySelect.value) || null;
    daySelect.replaceChildren(createOption('', '--'));
    for (let d = 1; d <= lastDay; d++) {
        daySelect.appendChild(createOption(d, d + '日'));
    }

    // 以前の選択日を可能な範囲で維持（末日超過時はクリア）
    if (prevDay && prevDay <= lastDay) {
        daySelect.value = prevDay;
    }
}

function loadSettings() {
    // 共通設定が未登録でもプルダウンが空にならないよう、先に初期化しておく
    initSettingsDateGroup('s-institution');
    fetch('/api/settings')
        .then(res => {
            if (!res.ok) throw new Error('共通設定が未登録です');
            return res.json();
        })
        .then(s => {
            document.getElementById('s-survey-address').value = s.surveyAddress || '';
            document.getElementById('s-survey-phone').value = s.surveyPhone || '';
            document.getElementById('s-facility-name').value = s.facilityName || '';
            document.getElementById('s-facility-phone').value = s.facilityPhone || '';
            document.getElementById('s-institution-name').value = s.institutionName || '';
            document.getElementById('s-institution-address').value = s.institutionAddress || '';
            // 入所年月日：値をセット後、選択中の年月に応じて日の選択肢を再生成
            document.getElementById('s-institution-year').value = s.institutionYear || '';
            document.getElementById('s-institution-month').value = s.institutionMonth || '';
            updateSettingsDayOptions('s-institution');
            document.getElementById('s-institution-day').value = s.institutionDay || '';
            document.getElementById('s-agent-name').value = s.agentName || '';
            document.getElementById('s-agent-postal').value = s.agentPostal || '';
            document.getElementById('s-agent-address').value = s.agentAddress || '';
            document.getElementById('s-agent-phone').value = s.agentPhone || '';
            document.getElementById('s-doctor-name').value = s.doctorName || '';
            document.getElementById('s-clinic-name').value = s.clinicName || '';
            document.getElementById('s-clinic-postal').value = s.clinicPostal || '';
            document.getElementById('s-clinic-address').value = s.clinicAddress || '';
            document.getElementById('s-clinic-phone').value = s.clinicPhone || '';
        })
        .catch(() => {});
}

/**
 * 共通設定を保存
 * PUT /api/settings にJSONリクエスト送信
 */
function saveSettings() {
    const body = {
        surveyAddress: document.getElementById('s-survey-address').value || null,
        surveyPhone: document.getElementById('s-survey-phone').value || null,
        facilityName: document.getElementById('s-facility-name').value || null,
        facilityPhone: document.getElementById('s-facility-phone').value || null,
        institutionName: document.getElementById('s-institution-name').value || null,
        institutionAddress: document.getElementById('s-institution-address').value || null,
        institutionYear: parseInt(document.getElementById('s-institution-year').value) || null,
        institutionMonth: parseInt(document.getElementById('s-institution-month').value) || null,
        institutionDay: parseInt(document.getElementById('s-institution-day').value) || null,
        agentName: document.getElementById('s-agent-name').value || null,
        agentPostal: document.getElementById('s-agent-postal').value || null,
        agentAddress: document.getElementById('s-agent-address').value || null,
        agentPhone: document.getElementById('s-agent-phone').value || null,
        doctorName: document.getElementById('s-doctor-name').value || null,
        clinicName: document.getElementById('s-clinic-name').value || null,
        clinicPostal: document.getElementById('s-clinic-postal').value || null,
        clinicAddress: document.getElementById('s-clinic-address').value || null,
        clinicPhone: document.getElementById('s-clinic-phone').value || null,
    };

    fetch('/api/settings', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
    })
    .then(res => {
        if (!res.ok) throw new Error('保存に失敗しました');
        showMessage('settings-message', '設定を保存しました', 'success');
    })
    .catch(err => {
        showMessage('settings-message', err.message, 'error');
    });
}

// ========================================
// ユーティリティ
// ========================================

/**
 * ユーザーへのメッセージを表示（5秒後に自動消去）
 * @param {string} elementId - メッセージ表示先のDOM要素ID
 * @param {string} text - 表示するメッセージ
 * @param {string} type - 'success' または 'error'
 */
function showMessage(elementId, text, type) {
    const el = document.getElementById(elementId);
    el.textContent = text;
    el.className = `message ${type}`;
    setTimeout(() => {
        el.textContent = '';
        el.className = 'message';
    }, 5000);
}

// ========================================
// 初期化（index.html 用）
// ========================================

/** ページロード時に利用者セレクトボックスを読み込み */
document.addEventListener('DOMContentLoaded', () => {
    // index.html: PDF生成ページ
    if (document.getElementById('member-select')) {
        loadMemberSelect();
    }
});
