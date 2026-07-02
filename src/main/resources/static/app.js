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
            select.innerHTML = '<option value="">-- 選択してください --</option>';
            members.forEach(m => {
                select.innerHTML += `<option value="${m.id}">${m.name}（${m.careLevel || '未設定'}）</option>`;
            });
        });
}

/**
 * PDF生成・ダウンロード処理
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
                container.innerHTML = '<p>登録されている利用者がいません。</p>';
                return;
            }
            let html = `<table class="member-table">
                <thead><tr>
                    <th>氏名</th><th>フリガナ</th><th>介護度</th><th>被保険者番号</th><th>操作</th>
                </tr></thead><tbody>`;
            members.forEach(m => {
                html += `<tr>
                    <td>${m.name}</td>
                    <td>${m.furigana || ''}</td>
                    <td>${m.careLevel || ''}</td>
                    <td>${m.insuranceIdNumber || ''}</td>
                    <td class="actions">
                        <button class="btn btn-edit" onclick="editMember('${m.id}')">編集</button>
                        <button class="btn btn-danger" onclick="deleteMember('${m.id}', '${m.name}')">削除</button>
                    </td>
                </tr>`;
            });
            html += '</tbody></table>';
            container.innerHTML = html;
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
function loadSettings() {
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
