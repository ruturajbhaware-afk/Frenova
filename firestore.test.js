const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const DEV_UID = "dev_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
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

test("Unauthenticated user: cannot read posts", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("posts").get());
});

test("Authenticated user: can create their own post", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("posts").doc("post_1").set({
      authorId: ALICE_UID,
      authorName: "Alice",
      content: "Hello Frenova!",
      isReel: false,
      createdAt: new Date(),
    })
  );
});

test("Authenticated user: cannot impersonate author on post creation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("posts").doc("post_2").set({
      authorId: BOB_UID,
      authorName: "Bob",
      content: "Impersonated post",
      isReel: false,
      createdAt: new Date(),
    })
  );
});

test("Non-developer user: cannot access audit_logs", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("audit_logs").get());
});

test("Developer account: can access audit_logs", async () => {
  const devDb = testEnv.authenticatedContext(DEV_UID, {
    email: "bhawareraj852@gmail.com",
    email_verified: true,
  }).firestore();
  await assertSucceeds(devDb.collection("audit_logs").get());
});

test("Developer account: can access reported conversation", async () => {
  // Seed reported conversation
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("conversations").doc("conv_reported").set({
      participantIds: [ALICE_UID, BOB_UID],
      isReported: true,
      createdAt: new Date(),
    });
  });

  const devDb = testEnv.authenticatedContext(DEV_UID, {
    email: "bhawareraj852@gmail.com",
    email_verified: true,
  }).firestore();
  await assertSucceeds(devDb.collection("conversations").doc("conv_reported").get());
});

test("Non-participant cannot access un-reported conversation", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("conversations").doc("conv_private").set({
      participantIds: [ALICE_UID, BOB_UID],
      isReported: false,
      createdAt: new Date(),
    });
  });

  const charlieDb = testEnv.authenticatedContext("charlie_999").firestore();
  await assertFails(charlieDb.collection("conversations").doc("conv_private").get());
});
