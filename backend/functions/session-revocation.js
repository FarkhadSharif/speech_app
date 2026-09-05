"use strict";

const { HttpsError } = require("firebase-functions/v2/https");

function createRevokeOwnSessionsHandler(authClient) {
  return async (request) => {
    const uid = request.auth?.uid;
    if (!uid) {
      throw new HttpsError(
        "unauthenticated",
        "Authentication is required.",
      );
    }

    // Deliberately ignore request.data. The caller can revoke only its own uid.
    await authClient.revokeRefreshTokens(uid);
    return { success: true };
  };
}

module.exports = { createRevokeOwnSessionsHandler };
