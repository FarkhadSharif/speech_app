"use strict";

const { initializeApp } = require("firebase-admin/app");
const { getAuth } = require("firebase-admin/auth");
const { onCall } = require("firebase-functions/v2/https");
const { createRevokeOwnSessionsHandler } = require("./session-revocation");

initializeApp();

exports.revokeOwnSessions = onCall(
  { region: "us-central1" },
  createRevokeOwnSessionsHandler(getAuth()),
);
