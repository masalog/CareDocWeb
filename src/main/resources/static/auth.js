// auth.js — 管理画面用 Cognito 認証

// ---- Cognito 設定(CfnOutput の値に置き換える)----
const userPool = new AmazonCognitoIdentity.CognitoUserPool({
  UserPoolId: 'ap-northeast-1_XXXXXXXXX',
  ClientId: 'XXXXXXXXXXXXXXXXXXXXXXXXXX'
});

let currentSession = null;

// API 呼び出し用:ID トークンを返す(app.js から使う)
function getIdToken() {
  return currentSession ? currentSession.getIdToken().getJwtToken() : null;
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
          msg.textContent = 'ログインに失敗しました: ' + err.message;
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
              msg.textContent = 'パスワード設定に失敗: ' + err.message;
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