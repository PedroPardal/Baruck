/**
 * Baruck Cloud Functions
 *
 * Push notifications are delivered by writing a document to a user's
 * inbox:  users/{uid}/inbox/{autoId}  with { title, body, data }.
 * This file watches that collection, looks up the user's FCM tokens
 * (users/{uid}/tokens/{token}) and sends a push to each device.
 *
 * It also sends a push when a friend request is created, and runs a
 * daily scheduled check that pushes reminders whose dueAt has passed.
 */
const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { onSchedule } = require("firebase-functions/v2/scheduler");
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
    await pushTo(
      event.params.uid,
      d.title || "Baruck",
      d.body || "",
      d.data || {}
    );
  }
);

// 2) Notify a user when someone sends them a friend request.
exports.onFriendRequest = onDocumentCreated(
  "users/{uid}/friendRequests/{reqId}",
  async (event) => {
    const d = event.data && event.data.data();
    if (!d) return;
    await pushTo(
      event.params.uid,
      "Novo pedido de amizade",
      `${d.fromName || "Alguém"} quer te adicionar no Baruck`,
      { type: "friend_request", fromUid: d.fromUid || "" }
    );
  }
);

// 3) Every 15 minutes, fire reminders that are due and not yet sent.
exports.dueReminders = onSchedule("every 15 minutes", async () => {
  const now = admin.firestore.Timestamp.now();
  const due = await db
    .collectionGroup("reminders")
    .where("done", "==", false)
    .where("notified", "==", false)
    .where("dueAt", "<=", now)
    .get();

  const batch = db.batch();
  for (const doc of due.docs) {
    const r = doc.data();
    const members = Array.isArray(r.targets) ? r.targets : [];
    await Promise.all(
      members.map((uid) =>
        pushTo(uid, r.title || "Lembrete", r.body || "", {
          type: "reminder",
          farmId: r.farmId || "",
        })
      )
    );
    batch.update(doc.ref, { notified: true });
  }
  await batch.commit();
});
