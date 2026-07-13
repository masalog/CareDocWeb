// auth.js — 管理画面用 Cognito 認証

// ---- Cognito 設定(CfnOutput の値に置き換える)----
const userPool = new AmazonCognitoIdentity.CognitoUserPool({
  UserPoolId: 'ap-northeast-1_XXXXXXXXX',
  ClientId: 'XXXXXXXXXXXXXXXXXXXXXXXXXX'
});

let currentSession = null;

// API 呼び出し用:ID トークンを返す(app.js から使う)
function getIdToken() {
  if (!currentSession || !currentSession.isValid()) return null;
  return currentSession.getIdToken().getJwtToken();
}

/**
 * Cognito のエラーをユーザー向けメッセージに変換する。
 *
 * ユーザー列挙攻撃(エラーメッセージの違いによるアカウント存在有無の推測)を
 * 防ぐため、認証失敗系(UserNotFoundException / NotAuthorizedException)は
 * 同一の汎用メッセージに丸める。
 * 正規ユーザーの操作に必要な情報(試行回数超過・入力形式エラー等)は、
 * アカウントの存在有無を暴露しない範囲で個別に案内する。
 * @param {Error} err - Cognito が返すエラー
 * @returns {string} 画面表示用メッセージ
 */
function toLoginErrorMessage(err) {
  switch (err.code) {
      // 存在有無を隠すため、この2つは必ず同一メッセージにする
    case 'UserNotFoundException':
    case 'NotAuthorizedException':
      return 'メールアドレスまたはパスワードが正しくありません';
      // 試行回数超過
    case 'TooManyRequestsException':
    case 'LimitExceededException':
      return 'しばらく時間をおいてから再度お試しください';
      // 入力形式の問題(未入力など)
    case 'InvalidParameterException':
      return 'メールアドレスとパスワードを入力してください';
      // その他は詳細を出さない
    default:
      return 'ログインに失敗しました。時間をおいて再度お試しください';
  }
}

function doLogin() {
  const email = document.getElementById('login-email').value;
  const password = document.getElementById('login-password').value;
  const msg = document.getElementById('login-message');

  const cognitoUser = new AmazonCognitoIdentity.CognitoUser({
    Username: email, Pool: userPool
  });
  cognitoUser.authenticateUser(
      new AmazonCognitoIdentity.AuthenticationDetails({
        Username: email, Password: password
      }),
      {
        onSuccess: (session) => {
          currentSession = session;
          showAdminScreen();
        },
        onFailure: (err) => {
          msg.textContent = toLoginErrorMessage(err);
        },
        // admin-create-user で作った初回ログイン時
        newPasswordRequired: () => {
          const newPw = prompt('初回ログインです。新しいパスワードを設定してください');
          if (!newPw) return;
          cognitoUser.completeNewPasswordChallenge(newPw, {}, {
            onSuccess: (session) => {
              currentSession = session;
              showAdminScreen();
            },
            onFailure: (err) => {
              // パスワードポリシー違反はここに来るため、ポリシーの案内だけ
              // 具体的に出す(認証済みユーザーのため存在有無の暴露にはならない)
              if (err.code === 'InvalidPasswordException'
                  || err.code === 'InvalidParameterException') {
                msg.textContent = 'パスワードが要件を満たしていません(8文字以上・大文字・小文字・数字を含む)';
              } else {
                msg.textContent = 'パスワードの設定に失敗しました。再度お試しください';
              }
            }
          });
        }
      }
  );
}

function showAdminScreen() {
  document.getElementById('login-overlay').style.display = 'none';
  loadMembers();
}

// ページ読み込み時:有効なセッションが残っていればログインをスキップ
document.addEventListener('DOMContentLoaded', () => {
  const user = userPool.getCurrentUser();
  if (!user) return;  // オーバーレイのまま
  user.getSession((err, session) => {
    if (!err && session && session.isValid()) {
      currentSession = session;
      showAdminScreen();
    }
  });
});