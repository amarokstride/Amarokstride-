const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_runner_123";
const BOB_UID = "bob_partner_456";
const CHARLIE_UID = "charlie_outsider_789";

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

function validProfile(uid, ts) {
  return {
    userId: uid,
    displayName: "Alex Trail",
    preferredUnit: "MILES",
    weeklyBaselineMiles: 12.0,
    corosConnected: false,
    garminConnected: false,
    stravaConnected: false,
    pathboundConnected: false,
    healthConnectAutoExport: false,
    currentStreakWeeks: 0,
    accountRole: "PRIMARY",
    seasonTheme: "AUTO",
    createdAt: ts,
    updatedAt: ts,
  };
}

function validActivity(uid, ts, overrides = {}) {
  return {
    userId: uid,
    title: "Morning Ridge Tempo",
    activityType: "RUN",
    source: "GPS",
    distanceMiles: 5.2,
    durationSeconds: 2640,
    avgPaceSecPerMile: 508,
    elevationGainFeet: 420,
    startedAtEpochMs: Date.now() - 3600000,
    notes: "Felt smooth on the climbs.",
    routePoints: ["37.7749,-122.4194,120", "37.7760,-122.4180,145"],
    splitPacesSec: [520, 510, 505, 500, 495],
    createdAt: ts,
    updatedAt: ts,
    ...overrides,
  };
}

function validChallenge(uid, ts) {
  return {
    userId: uid,
    weekKey: "2026-W47",
    targetMiles: 15.0,
    loggedMiles: 5.2,
    difficultyTier: "RIDGE_RUNNER",
    progressionReason: "Up +10% after crushing last week's target!",
    challengeStatus: "ACTIVE",
    createdAt: ts,
    updatedAt: ts,
  };
}

function validMilestone(uid, ts) {
  return {
    userId: uid,
    badgeKey: "centurion_100_miles",
    title: "Centurion of the Hundred Leagues",
    subtitle: "First 100 miles logged across quests!",
    metricHighlight: "100.4 mi",
    unlockedAtEpochMs: Date.now() - 5000,
    sharedCount: 0,
    createdAt: ts,
    updatedAt: ts,
  };
}

test("Unauthenticated users cannot read or write any user collections or parties", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const ts = new Date(Date.now() - 1000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID, ts)));
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("activities").get());
  await assertFails(unauthDb.collection("parties").doc("WOLF-482").get());
  await assertFails(unauthDb.collection("parties").get());
});

test("UserProfile: owner can create and update preferences, partner role & seasonal theme; cross-user rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = new Date(Date.now() - 5000);

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID, ts)));
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      preferredUnit: "KILOMETERS",
      garminConnected: true,
      stravaConnected: true,
      pathboundConnected: true,
      seasonTheme: "WINTER",
      partnerPartyCode: "WOLF-482",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject ghost field injection
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject cross-user read and write
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Hijacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("ActivityRecord: equestrian rides, Garmin/Strava/Pathbound sources, gearId & voice transcripts succeed; invalid fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("activities")
      .doc("act_eq")
      .set(
        validActivity(ALICE_UID, ts, {
          title: "Dawn Cavalry Endurance",
          activityType: "EQUESTRIAN",
          source: "GARMIN",
          gearId: "saddle_1",
          voiceTranscript: "Great trot transition at mile 3.",
        })
      )
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("activities").doc("act_eq").update({
      voiceTranscript: "Updated voice note: horse felt strong on canter.",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject invalid source or @DocumentId injection
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("activities")
      .doc("act_bad")
      .set(validActivity(ALICE_UID, ts, { id: "act_bad" }))
  );

  // Reject cross-user access
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("activities").get());
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("activities").doc("act_eq").delete()
  );
});

test("WeeklyChallenge and MilestoneRecord rules hold", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = new Date(Date.now() - 5000);

  const chRef = aliceDb.collection("users").doc(ALICE_UID).collection("challenges").doc("2026-W47");
  await assertSucceeds(chRef.set(validChallenge(ALICE_UID, ts)));
  await assertSucceeds(
    chRef.update({
      loggedMiles: 16.0,
      challengeStatus: "COMPLETED",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  const msRef = aliceDb.collection("users").doc(ALICE_UID).collection("milestones").doc("centurion_100_miles");
  await assertSucceeds(msRef.set(validMilestone(ALICE_UID, ts)));
  await assertSucceeds(
    msRef.update({
      sharedCount: 2,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("TavernParty: primary creates party, partner joins via invite code, members cheer; 3rd party & list blocked", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const ts = new Date(Date.now() - 5000);

  const partyCode = "WOLF-7842";
  const alicePartyRef = aliceDb.collection("parties").doc(partyCode);
  const bobPartyRef = bobDb.collection("parties").doc(partyCode);
  const charliePartyRef = charlieDb.collection("parties").doc(partyCode);

  // Primary creates Tavern Party
  await assertSucceeds(
    alicePartyRef.set({
      partyCode,
      creatorUid: ALICE_UID,
      memberUids: [ALICE_UID],
      primaryName: "Alex",
      primaryTotalMiles: 42.5,
      primaryLastQuest: "Pine Ridge Run",
      partnerTotalMiles: 0.0,
      cheerCount: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Listing all parties is forbidden
  await assertFails(bobDb.collection("parties").get());

  // Partner joins via secret invite code
  await assertSucceeds(
    bobPartyRef.update({
      memberUids: [ALICE_UID, BOB_UID],
      partnerUid: BOB_UID,
      partnerName: "Sam",
      partnerTotalMiles: 31.2,
      partnerLastQuest: "Meadow Equestrian Endurance",
      updatedAt: new Date(Date.now() - 2000),
    })
  );

  // Partner sends a cheer & updates miles
  await assertSucceeds(
    bobPartyRef.update({
      partnerTotalMiles: 36.4,
      cheerCount: 1,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Outsider cannot join a full 2-member party or mutate it
  await assertFails(
    charliePartyRef.update({
      memberUids: [ALICE_UID, BOB_UID, CHARLIE_UID],
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    charliePartyRef.update({
      cheerCount: 99,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(bobPartyRef.delete());
  await assertSucceeds(alicePartyRef.delete());
});

test("Public Tavern Chat and Private Pack Chat enforce auth, schema, and Pack membership", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const ts = new Date(Date.now() - 2000);

  const msg = (uid, name) => ({
    senderUid: uid,
    senderName: name,
    senderWolfCoat: "ARCTIC_WHITE",
    senderPackClass: "Silverwolf Strider",
    text: "Howling from the summit trail!",
    sentAtEpochMs: Date.now() - 1000,
    createdAt: ts,
  });

  // Unauthenticated denied on public chat
  await assertFails(unauthDb.collection("public_chat").get());
  await assertFails(unauthDb.collection("public_chat").doc("m1").set(msg(ALICE_UID, "Alice")));

  // Signed-in striders can post & read public chat, only sender can delete
  await assertSucceeds(aliceDb.collection("public_chat").doc("m1").set(msg(ALICE_UID, "Alice")));
  await assertSucceeds(bobDb.collection("public_chat").get());
  await assertFails(bobDb.collection("public_chat").doc("m1").delete());
  await assertSucceeds(aliceDb.collection("public_chat").doc("m1").delete());

  // Create Pack party and test private Pack Chat
  const code = "WOLF-9021";
  await assertSucceeds(
    aliceDb.collection("parties").doc(code).set({
      partyCode: code,
      creatorUid: ALICE_UID,
      memberUids: [ALICE_UID],
      primaryName: "Alice",
      primaryTotalMiles: 12.0,
      primaryLastQuest: "Ridge Run",
      partnerTotalMiles: 0.0,
      cheerCount: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    bobDb.collection("parties").doc(code).update({
      memberUids: [ALICE_UID, BOB_UID],
      partnerUid: BOB_UID,
      partnerName: "Bob",
      partnerTotalMiles: 8.5,
      partnerLastQuest: "Pine Hike",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Pack members can chat; outsider Charlie is blocked
  await assertSucceeds(aliceDb.collection("parties").doc(code).collection("messages").doc("p1").set(msg(ALICE_UID, "Alice")));
  await assertSucceeds(bobDb.collection("parties").doc(code).collection("messages").get());
  await assertFails(charlieDb.collection("parties").doc(code).collection("messages").get());
  await assertFails(charlieDb.collection("parties").doc(code).collection("messages").doc("p2").set(msg(CHARLIE_UID, "Charlie")));
});

test("Live Solo & Pack Ranking Boards enforce owner/member writes and authenticated reads", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const ts = new Date(Date.now() - 2000);

  await assertFails(unauthDb.collection("solo_rankings").get());
  await assertSucceeds(
    aliceDb.collection("solo_rankings").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      wolfCoat: "SILVER_FROST",
      packClass: "Silverwolf Strider",
      level: 5,
      totalMiles: 44.8,
      weeklyMiles: 12.4,
      updatedAt: ts,
    })
  );
  await assertSucceeds(bobDb.collection("solo_rankings").get());
  await assertFails(
    bobDb.collection("solo_rankings").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Hacked",
      wolfCoat: "TIMBER_GREY",
      packClass: "Scout",
      level: 99,
      totalMiles: 9999.0,
      weeklyMiles: 500.0,
      updatedAt: ts,
    })
  );

  // Pack rankings require Pack membership
  const code = "WOLF-7744";
  await assertSucceeds(
    aliceDb.collection("parties").doc(code).set({
      partyCode: code,
      creatorUid: ALICE_UID,
      memberUids: [ALICE_UID],
      primaryName: "Alice",
      primaryTotalMiles: 44.8,
      primaryLastQuest: "Summit",
      partnerTotalMiles: 0.0,
      cheerCount: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    aliceDb.collection("pack_rankings").doc(code).set({
      partyCode: code,
      packName: "Alice's Wolf Pack",
      memberCount: 1,
      combinedMiles: 44.8,
      leadWolfCoat: "SILVER_FROST",
      updatedAt: ts,
    })
  );
  await assertSucceeds(charlieDb.collection("pack_rankings").get());
  await assertFails(
    charlieDb.collection("pack_rankings").doc(code).set({
      partyCode: code,
      packName: "Spoof",
      memberCount: 2,
      combinedMiles: 9999.0,
      leadWolfCoat: "TIMBER_GREY",
      updatedAt: ts,
    })
  );
});
