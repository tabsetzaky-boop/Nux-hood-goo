const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_nux_123";
const BOB_UID = "bob_nux_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write user profiles or conversations", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("conversations")
      .get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("conversations")
      .doc("conv_1")
      .set({
        userId: ALICE_UID,
        title: "Hack",
        pinned: false,
        archived: false,
        locked: false,
        modelName: "gemini-3.8-flash",
        createdAt: new Date(Date.now() - 1000),
        updatedAt: new Date(Date.now() - 1000),
      })
  );
});

test("Owner can create, update, and list UserProfile; cross-user and ghost fields fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "سارة أحمد",
      email: "sara@example.com",
      preferredName: "سارة",
      customInstructions: "أحب الإجابات المختصرة",
      gender: "female",
      age: 25,
      liveVoice: "Zephyr",
      profileCompleted: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      preferredName: "سارونة",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Immutable createdAt cannot be overwritten
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Ghost field rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Cross-user read/write rejected
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Bob",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Conversations & Messages: Kotlin payloads, partial updates, and isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 5000);

  const convRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("conversations")
    .doc("conv_1");

  await assertSucceeds(
    convRef.set({
      userId: ALICE_UID,
      title: "محادثة جديدة",
      pinned: false,
      archived: false,
      locked: false,
      modelName: "gemini-3.8-flash",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    convRef.update({
      pinned: true,
      title: "خطة السفر",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject @DocumentId "id" key in map
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("conversations")
      .doc("conv_2")
      .set({
        id: "conv_2",
        userId: ALICE_UID,
        title: "Bad",
        pinned: false,
        archived: false,
        locked: false,
        modelName: "gemini-3.8-flash",
        createdAt: now,
        updatedAt: now,
      })
  );

  const msgRef = convRef.collection("messages").doc("msg_1");
  await assertSucceeds(
    msgRef.set({
      userId: ALICE_UID,
      conversationId: "conv_1",
      role: "assistant",
      content: "مرحبا بك في نوكس!",
      thinking: "تحليل الطلب",
      sources: ["https://example.com"],
      model: "gemini-3.8-flash",
      webSearch: false,
      errorFlag: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    msgRef.update({
      feedback: "up",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Bob cannot list Alice's conversations or messages
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("conversations")
      .get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("conversations")
      .doc("conv_1")
      .collection("messages")
      .get()
  );
});

test("Projects, Memory, SiteBuilds, NativeApps, SecurityPin, and Feedback", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 4000);

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("projects")
      .doc("proj_1")
      .set({
        userId: ALICE_UID,
        name: "متجر القهوة",
        iconEmoji: "☕",
        description: "تطوير متجر قهوة مختصة",
        instructions: "استخدم لغة تسويقية راقية",
        pinned: true,
        buildKind: "site",
        createdAt: now,
        updatedAt: now,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("memory")
      .doc("mem_1")
      .set({
        userId: ALICE_UID,
        content: "أفضّل الإجابات العربية المختصرة",
        category: "تفضيلات",
        enabled: true,
        source: "manual",
        createdAt: now,
        updatedAt: now,
      })
  );

  // Invalid memory category rejected
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("memory")
      .doc("mem_bad")
      .set({
        userId: ALICE_UID,
        content: "test",
        category: "invalid_category",
        enabled: true,
        source: "manual",
        createdAt: now,
        updatedAt: now,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("site_builds")
      .doc("site_1")
      .set({
        userId: ALICE_UID,
        title: "صفحة القهوة",
        description: "صفحة هبوط",
        prompt: "صمم صفحة هبوط لمتجر قهوة",
        htmlCode: "<html><body><h1>Coffee</h1></body></html>",
        published: true,
        versionCount: 1,
        createdAt: now,
        updatedAt: now,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("native_apps")
      .doc("app_1")
      .set({
        userId: ALICE_UID,
        name: "تطبيق المهام",
        idea: "تطبيق مهام يومية",
        packageName: "com.nux.tasks",
        status: "جاهز",
        specSummary: "شاشة رئيسية وشاشة إضافة",
        permissions: ["NOTIFICATIONS"],
        features: ["تذكير ذكي"],
        secretKeys: ["GEMINI_KEY"],
        versionName: "1.0.0",
        versionCode: 1,
        createdAt: now,
        updatedAt: now,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("security")
      .doc("pin_chats")
      .set({
        userId: ALICE_UID,
        kind: "chats",
        pinHash: "a1b2c3d4e5f60718293a4b5c6d7e8f901234567890abcdef",
        salt: "randomsalt123",
        enabled: true,
        trustedDevices: ["device_1"],
        createdAt: now,
        updatedAt: now,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("feedback")
      .doc("fb_1")
      .set({
        userId: ALICE_UID,
        messageId: "msg_1",
        conversationId: "conv_1",
        rating: "up",
        note: "رد ممتاز",
        model: "gemini-3.8-flash",
        createdAt: now,
      })
  );
});
