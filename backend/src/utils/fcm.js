const { GoogleAuth } = require('google-auth-library');

async function getAccessToken() {
  const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT);
  const auth = new GoogleAuth({
    credentials: serviceAccount,
    scopes: ['https://www.googleapis.com/auth/firebase.messaging'],
  });
  const client = await auth.getClient();
  const token = await client.getAccessToken();
  return token.token;
}

async function sendPushNotification(fcmToken, title, body, data = {}) {
  if (!fcmToken) return;
  if (!process.env.FIREBASE_SERVICE_ACCOUNT) {
    console.warn('FIREBASE_SERVICE_ACCOUNT manquante — notifications désactivées.');
    return;
  }

  try {
    const accessToken = await getAccessToken();
    const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT);
    const projectId = serviceAccount.project_id;

    const payload = JSON.stringify({
      message: {
        token: fcmToken,
        notification: { title, body },
        android: {
          priority: 'high',
          notification: { sound: 'default', click_action: 'OPEN_CHAT_ACTIVITY' },
        },
        data: Object.fromEntries(
          Object.entries(data).map(([k, v]) => [k, String(v)])
        ),
      },
    });

    const https = require('https');
    return new Promise((resolve, reject) => {
      const options = {
        hostname: 'fcm.googleapis.com',
        path: `/v1/projects/${projectId}/messages:send`,
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer ' + accessToken,
          'Content-Length': Buffer.byteLength(payload),
        },
      };

      const req = https.request(options, (res) => {
        let responseData = '';
        res.on('data', (chunk) => { responseData += chunk; });
        res.on('end', () => {
          try {
            const parsed = JSON.parse(responseData);
            if (parsed.error) console.error('FCM erreur :', parsed.error);
            resolve(parsed);
          } catch (e) { resolve(responseData); }
        });
      });

      req.on('error', (err) => {
        console.error('Erreur envoi FCM :', err);
        reject(err);
      });
      req.write(payload);
      req.end();
    });
  } catch (err) {
    console.error('Erreur FCM v1 :', err);
  }
}

module.exports = { sendPushNotification };