const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_100";
const STUDENT_A = "student_alice";
const STUDENT_B = "student_bob";

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
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      await ctx.firestore().collection("admins").doc(ADMIN_UID).set({
        uid: ADMIN_UID,
        email: "admin@itggopeshwar.ac.in",
        displayName: "ITG Admin",
        addedBy: ADMIN_UID,
        createdAt: new Date(Date.now() - 10000),
      });
    });
  }
});

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("notices").where("published", "==", true).get());
  await assertFails(unauthDb.collection("users").doc(STUDENT_A).get());
  await assertFails(unauthDb.collection("academic_courses").where("published", "==", true).get());
});

test("Student profile PII isolation and self-edit parity", async () => {
  const aliceDb = testEnv.authenticatedContext(STUDENT_A, {
    email: "alice@itggopeshwar.ac.in",
    email_verified: true,
  }).firestore();
  const bobDb = testEnv.authenticatedContext(STUDENT_B, {
    email: "bob@itggopeshwar.ac.in",
    email_verified: true,
  }).firestore();

  const past = new Date(Date.now() - 5000);
  await assertSucceeds(
    aliceDb.collection("users").doc(STUDENT_A).set({
      userId: STUDENT_A,
      fullName: "Alice Rawat",
      email: "alice@itggopeshwar.ac.in",
      rollNumber: "ITG2026001",
      courseId: "btech",
      courseName: "B.Tech",
      branchId: "branch_1",
      branchName: "Branch Slot 1",
      academicYear: 1,
      semester: 1,
      profileCompleted: true,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Partial update preserving createdAt and userId
  await assertSucceeds(
    aliceDb.collection("users").doc(STUDENT_A).update({
      semester: 2,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject ghost field / role escalation
  await assertFails(
    aliceDb.collection("users").doc(STUDENT_B).set({
      userId: STUDENT_B,
      fullName: "Spoof",
      email: "spoof@itggopeshwar.ac.in",
      rollNumber: "999",
      courseId: "btech",
      courseName: "B.Tech",
      academicYear: 1,
      semester: 1,
      profileCompleted: true,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Bob cannot read Alice's PII or list /users
  await assertFails(bobDb.collection("users").doc(STUDENT_A).get());
  await assertFails(bobDb.collection("users").get());

  // Admin CAN list /users for dashboard summary count
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: "admin@itggopeshwar.ac.in",
    email_verified: true,
  }).firestore();
  await assertSucceeds(adminDb.collection("users").get());
});

test("Private per-student notice read receipts", async () => {
  const aliceDb = testEnv.authenticatedContext(STUDENT_A).firestore();
  const bobDb = testEnv.authenticatedContext(STUDENT_B).firestore();
  const past = new Date(Date.now() - 1000);

  await assertSucceeds(
    aliceDb.collection("users").doc(STUDENT_A).collection("read_notices").doc("notice_1").set({
      noticeId: "notice_1",
      readAt: past,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(STUDENT_A).collection("read_notices").get()
  );
  await assertFails(
    bobDb.collection("users").doc(STUDENT_A).collection("read_notices").get()
  );
});

test("Students cannot modify admin content or escalate to admin", async () => {
  const aliceDb = testEnv.authenticatedContext(STUDENT_A, {
    email: "alice@itggopeshwar.ac.in",
    email_verified: true,
  }).firestore();
  const past = new Date(Date.now() - 2000);

  await assertFails(
    aliceDb.collection("admins").doc(STUDENT_A).set({
      uid: STUDENT_A,
      email: "alice@itggopeshwar.ac.in",
      displayName: "Hacker",
      addedBy: STUDENT_A,
      createdAt: past,
    })
  );

  await assertFails(
    aliceDb.collection("notices").doc("n1").set({
      title: "Fake Notice",
      message: "Classes cancelled",
      category: "Urgent",
      targetCourseId: "ALL",
      targetBranchId: "ALL",
      targetYear: 0,
      targetSemester: 0,
      published: true,
      important: true,
      authorName: "Alice",
      createdBy: STUDENT_A,
      createdAt: past,
      updatedAt: past,
    })
  );
});

test("Email spoofing with email_verified=false is rejected for bootstrapped admin", async () => {
  const spoofDb = testEnv.authenticatedContext("spoof_uid", {
    email: "agzyatwasi@gmail.com",
    email_verified: false,
  }).firestore();
  const past = new Date(Date.now() - 2000);

  await assertFails(
    spoofDb.collection("academic_courses").doc("btech").set({
      name: "B.Tech",
      code: "BTECH",
      durationYears: 4,
      totalSemesters: 8,
      hasBranches: true,
      published: true,
      createdBy: "spoof_uid",
      createdAt: past,
      updatedAt: past,
    })
  );
});

test("Admin CRUD, @DocumentId id rejection, and student published query alignment", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: "admin@itggopeshwar.ac.in",
    email_verified: true,
  }).firestore();
  const aliceDb = testEnv.authenticatedContext(STUDENT_A).firestore();
  const past = new Date(Date.now() - 3000);

  // Create course
  await assertSucceeds(
    adminDb.collection("academic_courses").doc("btech").set({
      name: "B.Tech",
      code: "BTECH",
      durationYears: 4,
      totalSemesters: 8,
      hasBranches: true,
      published: true,
      createdBy: ADMIN_UID,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Reject redundant `id` key in payload
  await assertFails(
    adminDb.collection("academic_courses").doc("bca").set({
      id: "bca",
      name: "BCA",
      code: "BCA",
      durationYears: 3,
      totalSemesters: 6,
      hasBranches: false,
      published: true,
      createdBy: ADMIN_UID,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Branch requires existing course
  await assertSucceeds(
    adminDb.collection("academic_branches").doc("b1").set({
      courseId: "btech",
      name: "Branch 1",
      code: "BR1",
      displayOrder: 1,
      published: true,
      createdBy: ADMIN_UID,
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertFails(
    adminDb.collection("academic_branches").doc("b_orphan").set({
      courseId: "nonexistent",
      name: "Orphan Branch",
      code: "OB",
      displayOrder: 1,
      published: true,
      createdBy: ADMIN_UID,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Create published and draft notice
  await assertSucceeds(
    adminDb.collection("notices").doc("pub_1").set({
      title: "Semester Registration",
      message: "Please complete registration before Friday.",
      category: "Academic",
      targetCourseId: "ALL",
      targetBranchId: "ALL",
      targetYear: 0,
      targetSemester: 0,
      published: true,
      important: true,
      authorName: "Dean Office",
      createdBy: ADMIN_UID,
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertSucceeds(
    adminDb.collection("notices").doc("draft_1").set({
      title: "Internal Draft",
      message: "Pending approval",
      category: "General",
      targetCourseId: "ALL",
      targetBranchId: "ALL",
      targetYear: 0,
      targetSemester: 0,
      published: false,
      important: false,
      authorName: "Dean Office",
      createdBy: ADMIN_UID,
      createdAt: past,
      updatedAt: past,
    })
  );

  // Student filtered query succeeds; unfiltered query fails
  await assertSucceeds(
    aliceDb.collection("notices").where("published", "==", true).get()
  );
  await assertFails(aliceDb.collection("notices").get());

  // Future timestamp rejected
  await assertFails(
    adminDb.collection("notices").doc("future_1").set({
      title: "Future Notice",
      message: "Invalid timestamp",
      category: "General",
      targetCourseId: "ALL",
      targetBranchId: "ALL",
      targetYear: 0,
      targetSemester: 0,
      published: true,
      important: false,
      authorName: "Dean",
      createdBy: ADMIN_UID,
      createdAt: new Date(Date.now() + 60000),
      updatedAt: past,
    })
  );

  // Gallery album & photo consistency
  await assertSucceeds(
    adminDb.collection("gallery_albums").doc("alb_1").set({
      title: "Annual Tech Fest",
      description: "ITG Gopeshwar technical exhibition",
      eventCategory: "Technical",
      eventDateText: "Oct 2026",
      photoCount: 1,
      published: true,
      createdBy: ADMIN_UID,
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertSucceeds(
    adminDb.collection("gallery_photos").doc("ph_1").set({
      albumId: "alb_1",
      caption: "Robotics showcase",
      imageUrl: "https://example.com/photo.jpg",
      published: true,
      createdBy: ADMIN_UID,
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertFails(
    adminDb.collection("gallery_photos").doc("ph_orphan").set({
      albumId: "missing_alb",
      caption: "Orphan",
      imageUrl: "https://example.com/photo.jpg",
      published: true,
      createdBy: ADMIN_UID,
      createdAt: past,
      updatedAt: past,
    })
  );
});
