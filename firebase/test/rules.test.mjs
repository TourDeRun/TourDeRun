// Firestore 보안 규칙 시나리오 (#79). 기술 명세서 "보안 규칙 시나리오 (에뮬레이터)" 표 1~14 + users 생성.
// 실행: firebase/ 폴더에서 `npm test` (에뮬레이터를 켜고 이 파일을 돌린 뒤 끈다)
import { after, before, beforeEach, describe, test } from 'node:test';
import { readFileSync } from 'node:fs';
import assert from 'node:assert/strict';
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from '@firebase/rules-unit-testing';
import {
  Timestamp,
  collection,
  deleteDoc,
  doc,
  getDoc,
  getDocs,
  increment,
  serverTimestamp,
  setDoc,
  updateDoc,
  writeBatch,
} from 'firebase/firestore';

let env;

before(async () => {
  env = await initializeTestEnvironment({
    projectId: 'demo-tourderun',
    firestore: {
      rules: readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8'),
      host: '127.0.0.1',
      port: 8080,
    },
  });
});

after(async () => {
  await env.cleanup();
});

beforeEach(async () => {
  await env.clearFirestore();
});

// ---------- 도우미 ----------

/** uid로 로그인한 사용자의 Firestore. 규칙이 적용된다. */
const db = (uid) => env.authenticatedContext(uid).firestore();

/** 규칙을 끄고 미리 데이터를 넣는다 (콘솔에서 넣는 것과 같다). */
const seed = (fn) => env.withSecurityRulesDisabled((ctx) => fn(ctx.firestore()));

const now = () => Timestamp.now();

const userData = (nickname, teamId = null) => ({
  nickname, createdAt: now(), termsAgreedAt: now(), teamId, totalM: 0, runCount: 0, cards: {},
});

const teamData = (memberCount, distanceM = 0) => ({
  name: '뛰뛰크루', icon: 0, color: 0, memberCount, season: 1, distanceM, reachedAt: now(), lastRunId: null,
});

const memberData = (nickname, role, inviteCode) => ({
  nickname, role, joinedAt: now(), inviteCode, contributionM: 0, seenArrivals: 0, lastRunId: null,
});

/** 팀 하나를 만들어 둔다. uids 순서대로 가입, 첫 번째가 팀장. */
async function seedTeam(uids, { teamId = 't1', code = 'ABC234', distanceM = 0 } = {}) {
  await seed(async (d) => {
    await setDoc(doc(d, 'teams', teamId), teamData(uids.length, distanceM));
    await setDoc(doc(d, 'inviteCodes', code), { teamId, createdAt: now() });
    for (const [i, uid] of uids.entries()) {
      await setDoc(doc(d, 'users', uid), userData(`러너${i + 1}`, teamId));
      await setDoc(doc(d, 'teams', teamId, 'members', uid), memberData(`러너${i + 1}`, i === 0 ? 'leader' : 'member', code));
    }
  });
}

/** 팀이 없는 사용자를 만들어 둔다. */
const seedUser = (uid) => seed((d) => setDoc(doc(d, 'users', uid), userData('새러너')));

/** TEAM-03 참여 배치: 내 members 생성 + 팀 인원 newCount + 내 users.teamId. */
function joinBatch(uid, { teamId = 't1', code = 'ABC234', newCount }) {
  const d = db(uid);
  const b = writeBatch(d);
  b.set(doc(d, 'teams', teamId, 'members', uid), {
    nickname: '새러너', role: 'member', joinedAt: serverTimestamp(), inviteCode: code,
    contributionM: 0, seenArrivals: 0, lastRunId: null,
  });
  b.update(doc(d, 'teams', teamId), { memberCount: newCount });
  b.update(doc(d, 'users', uid), { teamId });
  return b;
}

/**
 * REC-01 러닝 저장 배치 (앱의 runSavePlan과 같은 5개 문서).
 * @param memberMOf teamStats.memberM에서 거리를 올릴 칸. 기본은 나 (시나리오 8은 다른 팀원)
 */
function runBatch(uid, { teamId = 't1', runId = 'r1', distanceM = 5000, periodId = '2026-W47', memberMOf = uid } = {}) {
  const d = db(uid);
  const b = writeBatch(d);
  b.set(doc(d, 'runs', runId), {
    uid, teamId, startedAt: now(), endedAt: now(), movingSec: 1800, distanceM, paceSecPerKm: 360,
    periodId, title: '저녁 러닝', memo: null, path: [],
  });
  b.update(doc(d, 'teams', teamId), { distanceM: increment(distanceM), reachedAt: serverTimestamp(), lastRunId: runId });
  b.update(doc(d, 'teams', teamId, 'members', uid), { contributionM: increment(distanceM), lastRunId: runId });
  b.set(doc(d, 'teamStats', `${teamId}_${periodId}`), {
    distanceM: increment(distanceM), runCount: increment(1), memberM: { [memberMOf]: increment(distanceM) },
    lastRunId: runId, teamId, season: 1, periodId,
  }, { merge: true });
  b.update(doc(d, 'users', uid), { totalM: increment(distanceM), runCount: increment(1) });
  return b;
}

/** 규칙을 끄고 문서를 읽는다 (배치 결과 값 확인용). */
async function readAsAdmin(...path) {
  let data;
  await seed(async (d) => { data = (await getDoc(doc(d, ...path))).data(); });
  return data;
}

// ---------- users (AUTH-02) ----------

describe('users', () => {
  const newUser = (extra = {}) => ({
    nickname: '민찬', createdAt: serverTimestamp(), termsAgreedAt: serverTimestamp(),
    teamId: null, totalM: 0, runCount: 0, cards: {}, ...extra,
  });

  test('닉네임·서버 시각·teamId null로 생성 → 허용', async () => {
    await assertSucceeds(setDoc(doc(db('u1'), 'users', 'u1'), newUser()));
  });

  test('termsAgreedAt 없이 생성 → 거절', async () => {
    const { termsAgreedAt, ...withoutTerms } = newUser();
    await assertFails(setDoc(doc(db('u1'), 'users', 'u1'), withoutTerms));
  });

  test('1자 닉네임 → 거절', async () => {
    await assertFails(setDoc(doc(db('u1'), 'users', 'u1'), newUser({ nickname: '민' })));
  });

  test('다른 사람 문서로 생성 → 거절', async () => {
    await assertFails(setDoc(doc(db('u1'), 'users', 'u2'), newUser()));
  });

  test('다른 사람 users 읽기 → 거절, 내 것 → 허용', async () => {
    await seedUser('u1');
    await seedUser('u2');
    await assertFails(getDoc(doc(db('u1'), 'users', 'u2')));
    await assertSucceeds(getDoc(doc(db('u1'), 'users', 'u1')));
  });

  test('약관 동의 시각 바꾸기 → 거절', async () => {
    await seedUser('u1');
    await assertFails(updateDoc(doc(db('u1'), 'users', 'u1'), { termsAgreedAt: serverTimestamp() }));
  });
});

// ---------- 기술 명세서 시나리오 1~14 ----------

describe('팀 만들기·참여 (TEAM-01·03)', () => {
  const teamBatch = (uid, { withMember = true } = {}) => {
    const d = db(uid);
    const b = writeBatch(d);
    b.set(doc(d, 'teams', 't1'), {
      name: '뛰뛰크루', icon: 0, color: 0, memberCount: 1, season: 1, distanceM: 0,
      reachedAt: serverTimestamp(), lastRunId: null,
    });
    if (withMember) {
      b.set(doc(d, 'teams', 't1', 'members', uid), {
        nickname: '새러너', role: 'leader', joinedAt: serverTimestamp(), inviteCode: 'ABC234',
        contributionM: 0, seenArrivals: 0, lastRunId: null,
      });
      b.set(doc(d, 'inviteCodes', 'ABC234'), { teamId: 't1', createdAt: serverTimestamp() });
    }
    b.update(doc(d, 'users', uid), { teamId: 't1' });
    return b;
  };

  test('1. 팀 만들기 배치 (teams·members·inviteCodes·users) → 허용', async () => {
    await seedUser('u1');
    await assertSucceeds(teamBatch('u1').commit());
  });

  test('1-1. members 없이 teams만 → 거절', async () => {
    await seedUser('u1');
    await assertFails(teamBatch('u1', { withMember: false }).commit());
  });

  test('2. 초대 코드로 참여, 4번째 사람 → 허용', async () => {
    await seedTeam(['u1', 'u2', 'u3']);
    await seedUser('u4');
    await assertSucceeds(joinBatch('u4', { newCount: 4 }).commit());
  });

  test('3. 5번째 사람 참여 → 거절', async () => {
    await seedTeam(['u1', 'u2', 'u3', 'u4']);
    await seedUser('u5');
    await assertFails(joinBatch('u5', { newCount: 5 }).commit());
  });

  test('4. 이미 팀이 있는 사람이 다른 팀 참여 → 거절', async () => {
    await seedTeam(['u1']);
    await seedTeam(['u2'], { teamId: 't2', code: 'XYZ789' });
    await assertFails(joinBatch('u2', { newCount: 2 }).commit());
  });
});

describe('러닝 저장 (REC-01)', () => {
  test('5. REC-01 배치 (5개 문서) → 허용', async () => {
    await seedTeam(['u1', 'u2']);
    await assertSucceeds(runBatch('u1').commit());
  });

  test('6. 같은 lastRunId로 팀 거리 한 번 더 증가 → 거절', async () => {
    await seedTeam(['u1', 'u2']);
    await runBatch('u1').commit();
    await assertFails(updateDoc(doc(db('u1'), 'teams', 't1'), {
      distanceM: increment(5000), reachedAt: serverTimestamp(), lastRunId: 'r1',
    }));
  });

  test('7. 러닝 없이 teams.distanceM만 증가 → 거절', async () => {
    await seedTeam(['u1']);
    await assertFails(updateDoc(doc(db('u1'), 'teams', 't1'), { distanceM: increment(5000) }));
  });

  test('8. 다른 팀원의 memberM 항목 증가 → 거절', async () => {
    await seedTeam(['u1', 'u2']);
    await assertFails(runBatch('u1', { memberMOf: 'u2' }).commit());
  });

  test('5-1. 같은 주 두 번째 러닝 (이미 있는 teamStats에 더함) → 허용', async () => {
    await seedTeam(['u1', 'u2']);
    await runBatch('u1', { runId: 'r1', distanceM: 5000 }).commit();
    await assertSucceeds(runBatch('u1', { runId: 'r2', distanceM: 3000 }).commit());

    const stats = await readAsAdmin('teamStats', 't1_2026-W47');
    assert.equal(stats.distanceM, 8000);
    assert.equal(stats.runCount, 2);
    assert.equal(stats.memberM.u1, 8000);
  });

  test('5-2. 다른 팀원이 같은 주 문서에 더함 → 허용, 내 memberM 칸은 그대로', async () => {
    await seedTeam(['u1', 'u2']);
    await runBatch('u1', { runId: 'r1', distanceM: 5000 }).commit();
    await assertSucceeds(runBatch('u2', { runId: 'r2', distanceM: 3000 }).commit());

    const stats = await readAsAdmin('teamStats', 't1_2026-W47');
    assert.equal(stats.distanceM, 8000);
    assert.equal(stats.runCount, 2);
    assert.deepEqual(stats.memberM, { u1: 5000, u2: 3000 });
  });

  test('5-3. 팀 없는 사용자의 저장 (runs + users 2개 문서) → 허용', async () => {
    await seedUser('u1');
    const d = db('u1');
    const b = writeBatch(d);
    b.set(doc(d, 'runs', 'r1'), {
      uid: 'u1', teamId: null, startedAt: now(), endedAt: now(), movingSec: 1800, distanceM: 5000,
      paceSecPerKm: 360, periodId: '2026-W47', title: '저녁 러닝', memo: null, path: [],
    });
    b.update(doc(d, 'users', 'u1'), { totalM: increment(5000), runCount: increment(1) });
    await assertSucceeds(b.commit());
  });

  test('9. 다른 사람의 runs 읽기 → 거절, 내 것 → 허용', async () => {
    await seedTeam(['u1', 'u2']);
    await runBatch('u2', { runId: 'r2' }).commit();
    await assertFails(getDoc(doc(db('u1'), 'runs', 'r2')));
    await assertSucceeds(getDoc(doc(db('u2'), 'runs', 'r2')));
  });

  test('14. 기록의 distanceM 수정 → 거절, 제목 수정 → 허용', async () => {
    await seedTeam(['u1']);
    await runBatch('u1').commit();
    await assertFails(updateDoc(doc(db('u1'), 'runs', 'r1'), { distanceM: 99999 }));
    await assertSucceeds(updateDoc(doc(db('u1'), 'runs', 'r1'), { title: '아침 러닝' }));
  });
});

describe('팀원 문서 수정 (JRN-05·REC-01)', () => {
  test('본인 seenArrivals 수정 → 허용', async () => {
    await seedTeam(['u1', 'u2']);
    await assertSucceeds(updateDoc(doc(db('u2'), 'teams', 't1', 'members', 'u2'), { seenArrivals: 3 }));
  });

  /** u2의 본 도착 수를 n으로 맞춰 둔다. */
  const seedSeen = (n) => seed((d) => updateDoc(doc(d, 'teams', 't1', 'members', 'u2'), { seenArrivals: n }));

  test('본 도착 수 2 → 3 → 허용', async () => {
    await seedTeam(['u1', 'u2']);
    await seedSeen(2);
    await assertSucceeds(updateDoc(doc(db('u2'), 'teams', 't1', 'members', 'u2'), { seenArrivals: 3 }));
  });

  test('본 도착 수 3 → 2 (줄이기) → 거절', async () => {
    await seedTeam(['u1', 'u2']);
    await seedSeen(3);
    await assertFails(updateDoc(doc(db('u2'), 'teams', 't1', 'members', 'u2'), { seenArrivals: 2 }));
  });

  test('본 도착 수 8 (도시 7곳 초과) → 거절', async () => {
    await seedTeam(['u1', 'u2']);
    await assertFails(updateDoc(doc(db('u2'), 'teams', 't1', 'members', 'u2'), { seenArrivals: 8 }));
  });

  test('본 도착 수 그대로 다시 쓰기 → 허용', async () => {
    await seedTeam(['u1', 'u2']);
    await seedSeen(3);
    await assertSucceeds(updateDoc(doc(db('u2'), 'teams', 't1', 'members', 'u2'), { seenArrivals: 3 }));
  });

  test('닉네임만 수정 (본 도착 수 그대로) → 허용', async () => {
    await seedTeam(['u1', 'u2']);
    await seedSeen(3);
    await assertSucceeds(updateDoc(doc(db('u2'), 'teams', 't1', 'members', 'u2'), { nickname: '바뀐닉' }));
  });

  test('다른 팀원의 seenArrivals 수정 → 거절 (팀장이어도)', async () => {
    await seedTeam(['u1', 'u2']);
    await assertFails(updateDoc(doc(db('u1'), 'teams', 't1', 'members', 'u2'), { seenArrivals: 3 }));
  });

  test('러닝 없이 contributionM 직접 수정 → 거절', async () => {
    await seedTeam(['u1', 'u2']);
    await assertFails(updateDoc(doc(db('u2'), 'teams', 't1', 'members', 'u2'), { contributionM: increment(5000) }));
  });
});

describe('초대 코드·팀 관리 (TEAM-02·05·06·07)', () => {
  test('10. inviteCodes 목록 조회 → 거절, 단건 조회 → 허용', async () => {
    await seedTeam(['u1']);
    await seedUser('u9');
    await assertFails(getDocs(collection(db('u9'), 'inviteCodes')));
    await assertSucceeds(getDoc(doc(db('u9'), 'inviteCodes', 'ABC234')));
  });

  test('11. 팀원이 팀 이름 수정 → 거절 / 팀장이 수정 → 허용', async () => {
    await seedTeam(['u1', 'u2']);
    await assertFails(updateDoc(doc(db('u2'), 'teams', 't1'), { name: '바뀐이름' }));
    await assertSucceeds(updateDoc(doc(db('u1'), 'teams', 't1'), { name: '바뀐이름' }));
  });

  test('12. 팀장이 팀원 내보내기 (members 삭제 + 인원 -1) → 허용', async () => {
    await seedTeam(['u1', 'u2']);
    const d = db('u1');
    const b = writeBatch(d);
    b.delete(doc(d, 'teams', 't1', 'members', 'u2'));
    b.update(doc(d, 'teams', 't1'), { memberCount: 1 });
    await assertSucceeds(b.commit());
  });

  test('13. 마지막 1명 탈퇴 (팀 전체 삭제 배치) → 허용', async () => {
    await seedTeam(['u1']);
    await seed((s) => setDoc(doc(s, 'teamStats', 't1_2026-W47'), {
      teamId: 't1', season: 1, periodId: '2026-W47', distanceM: 0, runCount: 0, memberM: {}, lastRunId: null,
    }));
    const d = db('u1');
    const b = writeBatch(d);
    b.delete(doc(d, 'teams', 't1'));
    b.delete(doc(d, 'teams', 't1', 'members', 'u1'));
    b.delete(doc(d, 'inviteCodes', 'ABC234'));
    b.delete(doc(d, 'teamStats', 't1_2026-W47'));
    b.update(doc(d, 'users', 'u1'), { teamId: null });
    await assertSucceeds(b.commit());
  });

  test('TEAM-07 팀원이 스스로 나가기 (내 members 삭제 + 인원 -1 + users.teamId null) → 허용', async () => {
    await seedTeam(['u1', 'u2']);
    const d = db('u2');
    const b = writeBatch(d);
    b.delete(doc(d, 'teams', 't1', 'members', 'u2'));
    b.update(doc(d, 'teams', 't1'), { memberCount: 1 });
    b.update(doc(d, 'users', 'u2'), { teamId: null });
    await assertSucceeds(b.commit());
  });

  test('TEAM-07 팀장이 나가며 다음 팀원을 팀장으로 → 허용', async () => {
    await seedTeam(['u1', 'u2', 'u3']);
    const d = db('u1');
    const b = writeBatch(d);
    b.update(doc(d, 'teams', 't1', 'members', 'u2'), { role: 'leader' });
    b.delete(doc(d, 'teams', 't1', 'members', 'u1'));
    b.update(doc(d, 'teams', 't1'), { memberCount: 2 });
    b.update(doc(d, 'users', 'u1'), { teamId: null });
    await assertSucceeds(b.commit());
  });

  test('TEAM-07 팀원이 나가며 다른 팀원을 팀장으로 → 거절', async () => {
    await seedTeam(['u1', 'u2', 'u3']);
    const d = db('u2');
    const b = writeBatch(d);
    b.update(doc(d, 'teams', 't1', 'members', 'u3'), { role: 'leader' });
    b.delete(doc(d, 'teams', 't1', 'members', 'u2'));
    b.update(doc(d, 'teams', 't1'), { memberCount: 2 });
    b.update(doc(d, 'users', 'u2'), { teamId: null });
    await assertFails(b.commit());
  });

  test('TEAM-07 나가면서 인원을 2 줄이기 → 거절', async () => {
    await seedTeam(['u1', 'u2', 'u3']);
    const d = db('u3');
    const b = writeBatch(d);
    b.delete(doc(d, 'teams', 't1', 'members', 'u3'));
    b.update(doc(d, 'teams', 't1'), { memberCount: 1 });
    await assertFails(b.commit());
  });

  test('팀원이 아닌 사람의 members 읽기 → 거절', async () => {
    await seedTeam(['u1']);
    await seedUser('u9');
    await assertFails(getDoc(doc(db('u9'), 'teams', 't1', 'members', 'u1')));
  });

  test('팀원이 팀 삭제 (2명 남음) → 거절', async () => {
    await seedTeam(['u1', 'u2']);
    await assertFails(deleteDoc(doc(db('u1'), 'teams', 't1')));
  });
});
