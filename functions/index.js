const admin = require("firebase-admin");
const { onRequest } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const logger = require("firebase-functions/logger");

if (!admin.apps.length) {
  admin.initializeApp();
}

const COROS_CLIENT_SECRET = defineSecret("COROS_CLIENT_SECRET");
const STRAVA_CLIENT_SECRET = defineSecret("STRAVA_CLIENT_SECRET");
const GARMIN_CLIENT_SECRET = defineSecret("GARMIN_CLIENT_SECRET");

const MAX_TOKEN_LENGTH = 4096;
const MAX_VERIFIER_LENGTH = 256;
const PROVIDER_TIMEOUT_MS = 15000;

/**
 * Strict per-provider allowlist of official OAuth token endpoints and
 * registered mobile deep-link redirect URIs. Caller-supplied URLs are never accepted.
 */
const PROVIDER_CONFIG = Object.freeze({
  coros: {
    tokenUrl: "https://open.coros.com/oauth2/accesstoken",
    refreshUrl: "https://open.coros.com/oauth2/refresh-token",
    allowedRedirectUri: "summitpace://coros/callback",
    clientIdEnv: "COROS_CLIENT_ID",
    secretParam: COROS_CLIENT_SECRET,
  },
  strava: {
    tokenUrl: "https://www.strava.com/oauth/token",
    refreshUrl: "https://www.strava.com/oauth/token",
    allowedRedirectUri: "summitpace://strava/callback",
    clientIdEnv: "STRAVA_CLIENT_ID",
    secretParam: STRAVA_CLIENT_SECRET,
  },
  garmin: {
    tokenUrl: "https://diauth.garmin.com/di-oauth2-service/oauth/token",
    refreshUrl: "https://diauth.garmin.com/di-oauth2-service/oauth/token",
    allowedRedirectUri: "summitpace://garmin/callback",
    clientIdEnv: "GARMIN_CLIENT_ID",
    secretParam: GARMIN_CLIENT_SECRET,
  },
});

function setSecurityHeaders(res) {
  res.set("Cache-Control", "no-store, no-cache, must-revalidate, private");
  res.set("Pragma", "no-cache");
  res.set("X-Content-Type-Options", "nosniff");
}

function sendJsonError(res, status, code, message) {
  setSecurityHeaders(res);
  res.status(status).json({ error: code, message });
}

/**
 * Verifies the caller's Firebase Authentication ID token before performing any
 * privileged OAuth secret operation.
 */
async function authenticateCaller(req, res) {
  if (req.method !== "POST") {
    sendJsonError(res, 405, "method_not_allowed", "Only POST requests are supported.");
    return null;
  }

  const authHeader = req.get("Authorization") || "";
  if (!authHeader.startsWith("Bearer ")) {
    sendJsonError(res, 401, "unauthenticated", "Missing Firebase Auth Bearer token.");
    return null;
  }

  const idToken = authHeader.slice("Bearer ".length).trim();
  if (!idToken || idToken.length > 8192) {
    sendJsonError(res, 401, "unauthenticated", "Invalid Firebase Auth Bearer token.");
    return null;
  }

  try {
    return await admin.auth().verifyIdToken(idToken, true);
  } catch (err) {
    logger.warn("Rejected OAuth proxy request due to invalid Firebase ID token.");
    sendJsonError(res, 401, "unauthenticated", "Firebase Auth verification failed.");
    return null;
  }
}

function resolveProviderConfig(rawProvider) {
  if (typeof rawProvider !== "string") return null;
  const key = rawProvider.trim().toLowerCase();
  return Object.prototype.hasOwnProperty.call(PROVIDER_CONFIG, key)
    ? { key, ...PROVIDER_CONFIG[key] }
    : null;
}

function resolveCredentials(providerCfg) {
  const clientId = (process.env[providerCfg.clientIdEnv] || "").trim();
  const clientSecret = (providerCfg.secretParam.value() || "").trim();
  const idValid = clientId.length > 0 && !clientId.startsWith("YOUR_");
  const secretValid = clientSecret.length > 0 && !clientSecret.startsWith("YOUR_");
  if (!idValid || !secretValid) {
    return null;
  }
  return { clientId, clientSecret };
}

function normalizeTokenPayload(providerKey, raw) {
  const nowSec = Math.floor(Date.now() / 1000);
  const accessToken =
    typeof raw.access_token === "string"
      ? raw.access_token
      : typeof raw.accessToken === "string"
        ? raw.accessToken
        : "";
  const refreshToken =
    typeof raw.refresh_token === "string"
      ? raw.refresh_token
      : typeof raw.refreshToken === "string"
        ? raw.refreshToken
        : null;
  const expiresIn = Number.isFinite(Number(raw.expires_in))
    ? Number(raw.expires_in)
    : Number.isFinite(Number(raw.expires_at))
      ? Math.max(0, Number(raw.expires_at) - nowSec)
      : null;
  const expiresAt = Number.isFinite(Number(raw.expires_at))
    ? Number(raw.expires_at)
    : expiresIn !== null
      ? nowSec + expiresIn
      : null;
  const openId =
    typeof raw.openId === "string"
      ? raw.openId
      : typeof raw.open_id === "string"
        ? raw.open_id
        : null;

  return {
    provider: providerKey,
    access_token: accessToken,
    refresh_token: refreshToken,
    expires_in: expiresIn,
    expires_at: expiresAt,
    open_id: openId,
    token_type: typeof raw.token_type === "string" ? raw.token_type : "Bearer",
    scope: typeof raw.scope === "string" ? raw.scope : null,
  };
}

async function postFormToProvider(url, params) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), PROVIDER_TIMEOUT_MS);
  try {
    const response = await fetch(url, {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
        Accept: "application/json",
      },
      body: new URLSearchParams(params).toString(),
      signal: controller.signal,
    });
    const text = await response.text();
    let json = null;
    try {
      json = text ? JSON.parse(text) : {};
    } catch (_) {
      json = null;
    }
    return { ok: response.ok, status: response.status, json };
  } finally {
    clearTimeout(timeout);
  }
}

/**
 * HTTPS Cloud Function: Exchanges an OAuth2 authorization code for COROS, Strava, or Garmin
 * using server-side client secrets stored in Firebase Secret Manager.
 */
exports.exchangeOAuthToken = onRequest(
  {
    region: "us-central1",
    timeoutSeconds: 30,
    maxInstances: 20,
    secrets: [COROS_CLIENT_SECRET, STRAVA_CLIENT_SECRET, GARMIN_CLIENT_SECRET],
  },
  async (req, res) => {
    const decodedUser = await authenticateCaller(req, res);
    if (!decodedUser) return;

    const body = req.body && typeof req.body === "object" ? req.body : {};
    const providerCfg = resolveProviderConfig(body.provider);
    if (!providerCfg) {
      sendJsonError(res, 400, "invalid_provider", "Provider must be one of: coros, strava, garmin.");
      return;
    }

    const code = typeof body.code === "string" ? body.code.trim() : "";
    if (!code || code.length > MAX_TOKEN_LENGTH) {
      sendJsonError(res, 400, "invalid_code", "A valid authorization code is required.");
      return;
    }

    const redirectUri =
      typeof body.redirectUri === "string" && body.redirectUri.trim().length > 0
        ? body.redirectUri.trim()
        : providerCfg.allowedRedirectUri;
    if (redirectUri !== providerCfg.allowedRedirectUri) {
      sendJsonError(res, 400, "invalid_redirect_uri", "Redirect URI does not match the registered callback.");
      return;
    }

    const codeVerifier = typeof body.codeVerifier === "string" ? body.codeVerifier.trim() : "";
    if (codeVerifier.length > MAX_VERIFIER_LENGTH) {
      sendJsonError(res, 400, "invalid_code_verifier", "PKCE codeVerifier exceeds maximum length.");
      return;
    }

    const creds = resolveCredentials(providerCfg);
    if (!creds) {
      logger.error("OAuth provider credentials are not configured on server.", {
        provider: providerCfg.key,
      });
      sendJsonError(
        res,
        503,
        "provider_not_configured",
        `${providerCfg.key.toUpperCase()} server OAuth credentials are not configured in Firebase Cloud Functions.`
      );
      return;
    }

    const formParams = {
      client_id: creds.clientId,
      client_secret: creds.clientSecret,
      code,
      grant_type: "authorization_code",
      redirect_uri: providerCfg.allowedRedirectUri,
    };
    if (providerCfg.key === "garmin" && codeVerifier.length > 0) {
      formParams.code_verifier = codeVerifier;
    }

    try {
      const upstream = await postFormToProvider(providerCfg.tokenUrl, formParams);
      if (!upstream.ok || !upstream.json) {
        logger.warn("OAuth provider rejected token exchange.", {
          provider: providerCfg.key,
          uid: decodedUser.uid,
          upstreamStatus: upstream.status,
        });
        sendJsonError(
          res,
          502,
          "upstream_exchange_failed",
          `Unable to complete ${providerCfg.key.toUpperCase()} authorization (HTTP ${upstream.status}).`
        );
        return;
      }

      const normalized = normalizeTokenPayload(providerCfg.key, upstream.json);
      if (!normalized.access_token) {
        sendJsonError(
          res,
          502,
          "missing_access_token",
          `${providerCfg.key.toUpperCase()} did not return an access token.`
        );
        return;
      }

      setSecurityHeaders(res);
      res.status(200).json(normalized);
    } catch (err) {
      logger.error("OAuth token exchange request failed.", {
        provider: providerCfg.key,
        uid: decodedUser.uid,
      });
      sendJsonError(res, 502, "upstream_network_error", "Could not reach the OAuth provider.");
    }
  }
);

/**
 * HTTPS Cloud Function: Refreshes an expired OAuth2 access token for COROS, Strava, or Garmin
 * using server-side client secrets stored in Firebase Secret Manager.
 */
exports.refreshOAuthToken = onRequest(
  {
    region: "us-central1",
    timeoutSeconds: 30,
    maxInstances: 20,
    secrets: [COROS_CLIENT_SECRET, STRAVA_CLIENT_SECRET, GARMIN_CLIENT_SECRET],
  },
  async (req, res) => {
    const decodedUser = await authenticateCaller(req, res);
    if (!decodedUser) return;

    const body = req.body && typeof req.body === "object" ? req.body : {};
    const providerCfg = resolveProviderConfig(body.provider);
    if (!providerCfg) {
      sendJsonError(res, 400, "invalid_provider", "Provider must be one of: coros, strava, garmin.");
      return;
    }

    const refreshToken = typeof body.refreshToken === "string" ? body.refreshToken.trim() : "";
    if (!refreshToken || refreshToken.length > MAX_TOKEN_LENGTH) {
      sendJsonError(res, 400, "invalid_refresh_token", "A valid refresh token is required.");
      return;
    }

    const creds = resolveCredentials(providerCfg);
    if (!creds) {
      logger.error("OAuth provider credentials are not configured on server.", {
        provider: providerCfg.key,
      });
      sendJsonError(
        res,
        503,
        "provider_not_configured",
        `${providerCfg.key.toUpperCase()} server OAuth credentials are not configured in Firebase Cloud Functions.`
      );
      return;
    }

    const formParams = {
      client_id: creds.clientId,
      client_secret: creds.clientSecret,
      refresh_token: refreshToken,
      grant_type: "refresh_token",
    };

    try {
      const upstream = await postFormToProvider(providerCfg.refreshUrl, formParams);
      if (!upstream.ok || !upstream.json) {
        logger.warn("OAuth provider rejected token refresh.", {
          provider: providerCfg.key,
          uid: decodedUser.uid,
          upstreamStatus: upstream.status,
        });
        sendJsonError(
          res,
          502,
          "upstream_refresh_failed",
          `Unable to refresh ${providerCfg.key.toUpperCase()} session (HTTP ${upstream.status}).`
        );
        return;
      }

      const normalized = normalizeTokenPayload(providerCfg.key, upstream.json);
      if (!normalized.access_token) {
        sendJsonError(
          res,
          502,
          "missing_access_token",
          `${providerCfg.key.toUpperCase()} did not return a refreshed access token.`
        );
        return;
      }

      setSecurityHeaders(res);
      res.status(200).json(normalized);
    } catch (err) {
      logger.error("OAuth token refresh request failed.", {
        provider: providerCfg.key,
        uid: decodedUser.uid,
      });
      sendJsonError(res, 502, "upstream_network_error", "Could not reach the OAuth provider.");
    }
  }
);
