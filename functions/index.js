/**
 * Baruck Cloud Functions
 *
 * Push notifications are delivered by writing a document to a user's
 * inbox:  users/{uid}/inbox/{autoId}  with { title, body, data }.
 * This file watches that collection, looks up the user's FCM tokens
 * (users/{uid}/tokens/{token}) and sends a push to each device.
 *
 * Lembretes vencidos são verificados pelo próprio app (WorkManager).
 */
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();
const messaging = admin.messaging();

async function tokensFor(uid) {
  const snap = await db.collection(`users/${uid}/tokens`).get();
  return snap.docs.map((d) => d.id).filter(Boolean);
}

async function pushTo(uid, title, body, data = {}) {
  const tokens = await tokensFor(uid);
  if (tokens.length === 0) return;
  const payload = {
    tokens,
    notification: { title, body },
    data: Object.fromEntries(
      Object.entries(data).map(([k, v]) => [k, String(v)])
    ),
    android: { priority: "high", notification: { channelId: "baruck_alerts" } },
  };
  const res = await messaging.sendEachForMulticast(payload);
  // Clean up tokens the device no longer accepts.
  const dead = [];
  res.responses.forEach((r, i) => {
    if (!r.success) {
      const code = r.error && r.error.code;
      if (
        code === "messaging/registration-token-not-registered" ||
        code === "messaging/invalid-registration-token"
      ) {
        dead.push(tokens[i]);
      }
    }
  });
  await Promise.all(
    dead.map((t) => db.doc(`users/${uid}/tokens/${t}`).delete())
  );
}

// 1) Deliver anything dropped into a user's inbox.
exports.onInbox = onDocumentCreated(
  "users/{uid}/inbox/{itemId}",
  async (event) => {
    const d = event.data && event.data.data();
    if (!d) return;
    await pushTo(event.params.uid, d.title || "Baruck", d.body || "", {
      inboxId: event.params.itemId,
      type: d.type || "",
      farmId: d.farmId || "",
      fromAvatar: d.fromAvatar || "baruck",
    });
  }
);
