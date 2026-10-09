const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const fs = require("fs");
const path = require("path");
const test = require("node:test");
const assert = require("node:assert");

let testEnv;

test.before(async () => {
  process.env.FIRESTORE_EMULATOR_HOST = "127.0.0.1:8085";
  testEnv = await initializeTestEnvironment({
    projectId: "sos-test-project-" + Date.now(),
    firestore: {
      rules: fs.readFileSync(path.resolve(__dirname, "../firestore.rules"), "utf8"),
      host: "127.0.0.1",
      port: 8085,
    },
  });
});

test.after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

test.beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

const collections = [
  "safety_timers",
  "ai_analysis",
  "ai_emergency_analysis_new",
  "fall_events",
];

collections.forEach((col) => {
  test(`[${col}] Owner can create document with own userId`, async () => {
    const db = testEnv.authenticatedContext("user_alice", { email: "alice@test.com" }).firestore();
    await assertSucceeds(
      db.collection(col).doc("doc1").set({
        userId: "user_alice",
        data: "test data",
      })
    );
  });

  test(`[${col}] Owner can read, update, and delete own document`, async () => {
    const aliceDb = testEnv.authenticatedContext("user_alice").firestore();

    await testEnv.withSecurityRulesDisabled(async (context) => {
      await context.firestore().collection(col).doc("doc1").set({
        userId: "user_alice",
        data: "initial",
      });
    });

    await assertSucceeds(aliceDb.collection(col).doc("doc1").get());

    await assertSucceeds(
      aliceDb.collection(col).doc("doc1").update({
        data: "updated",
        userId: "user_alice",
      })
    );

    await assertSucceeds(aliceDb.collection(col).doc("doc1").delete());
  });

  test(`[${col}] Another authenticated user cannot read, update, or delete document`, async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      await context.firestore().collection(col).doc("doc1").set({
        userId: "user_alice",
        data: "secret",
      });
    });

    const bobDb = testEnv.authenticatedContext("user_bob").firestore();

    await assertFails(bobDb.collection(col).doc("doc1").get());
    await assertFails(
      bobDb.collection(col).doc("doc1").update({
        data: "hijacked",
        userId: "user_bob",
      })
    );
    await assertFails(bobDb.collection(col).doc("doc1").delete());
  });

  test(`[${col}] Creating document with another user's userId is denied`, async () => {
    const aliceDb = testEnv.authenticatedContext("user_alice").firestore();
    await assertFails(
      aliceDb.collection(col).doc("doc1").set({
        userId: "user_bob",
        data: "spoofed",
      })
    );
  });

  test(`[${col}] Changing userId to transfer ownership on update is denied`, async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      await context.firestore().collection(col).doc("doc1").set({
        userId: "user_alice",
        data: "initial",
      });
    });

    const aliceDb = testEnv.authenticatedContext("user_alice").firestore();
    await assertFails(
      aliceDb.collection(col).doc("doc1").update({
        userId: "user_bob",
        data: "transfer",
      })
    );
  });

  test(`[${col}] Unauthenticated access is denied`, async () => {
    const anonDb = testEnv.unauthenticatedContext().firestore();
    await assertFails(
      anonDb.collection(col).doc("doc1").set({
        userId: "user_alice",
        data: "anon",
      })
    );
    await assertFails(anonDb.collection(col).doc("doc1").get());
  });

  test(`[${col}] Documents without userId cannot be read, updated, or deleted`, async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      await context.firestore().collection(col).doc("doc_nouser").set({
        data: "no owner",
      });
    });

    const aliceDb = testEnv.authenticatedContext("user_alice").firestore();
    await assertFails(aliceDb.collection(col).doc("doc_nouser").get());
    await assertFails(
      aliceDb.collection(col).doc("doc_nouser").update({
        data: "new",
      })
    );
    await assertFails(aliceDb.collection(col).doc("doc_nouser").delete());
  });
});

test("Root notifications collection is denied", async () => {
  const aliceDb = testEnv.authenticatedContext("user_alice").firestore();
  await assertFails(
    aliceDb.collection("notifications").doc("notif1").set({
      userId: "user_alice",
    })
  );
  await assertFails(aliceDb.collection("notifications").doc("notif1").get());
});

test("User-specific notifications subcollection is restricted to owner", async () => {
  const aliceDb = testEnv.authenticatedContext("user_alice").firestore();
  const bobDb = testEnv.authenticatedContext("user_bob").firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc("user_alice").collection("notifications").doc("n1").set({
      message: "hello",
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc("user_alice").collection("notifications").doc("n1").get()
  );

  await assertFails(
    bobDb.collection("users").doc("user_alice").collection("notifications").doc("n1").get()
  );
});

test("Unmatched paths are denied by default", async () => {
  const aliceDb = testEnv.authenticatedContext("user_alice").firestore();
  await assertFails(aliceDb.collection("unknown_collection").doc("doc1").set({ data: "test" }));
  await assertFails(aliceDb.collection("unknown_collection").doc("doc1").get());
});
