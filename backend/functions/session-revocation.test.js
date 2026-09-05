"use strict";

const assert = require("node:assert/strict");
const test = require("node:test");
const { createRevokeOwnSessionsHandler } = require("./session-revocation");

test("rejects an unauthenticated request", async () => {
  const revoked = [];
  const handler = createRevokeOwnSessionsHandler({
    revokeRefreshTokens: async (uid) => revoked.push(uid),
  });

  await assert.rejects(
    handler({ data: {} }),
    (error) => error.code === "unauthenticated",
  );
  assert.deepEqual(revoked, []);
});

test("revokes only the uid from the verified auth context", async () => {
  const revoked = [];
  const handler = createRevokeOwnSessionsHandler({
    revokeRefreshTokens: async (uid) => revoked.push(uid),
  });

  const response = await handler({
    auth: { uid: "current-parent" },
    data: { uid: "another-parent" },
  });

  assert.deepEqual(revoked, ["current-parent"]);
  assert.deepEqual(response, { success: true });
});
