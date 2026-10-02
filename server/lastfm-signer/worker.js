import { createHash, timingSafeEqual } from "node:crypto";

const API = "https://ws.audioscrobbler.com/2.0/";

function json(value, status = 200) {
  return new Response(JSON.stringify(value), {
    status,
    headers: { "content-type": "application/json; charset=utf-8", "cache-control": "no-store" },
  });
}

function authorized(request, expected) {
  const actual = request.headers.get("authorization")?.replace(/^Bearer\s+/i, "") || "";
  if (!actual || !expected) return false;
  const actualBytes = Buffer.from(actual), expectedBytes = Buffer.from(expected);
  return actualBytes.length === expectedBytes.length && timingSafeEqual(actualBytes, expectedBytes);
}

function signature(parameters, secret) {
  const text = Object.keys(parameters).sort().map((key) => `${key}${parameters[key]}`).join("") + secret;
  return createHash("md5").update(text, "utf8").digest("hex");
}

async function lastFm(env, method, parameters = {}, authenticated = false) {
  const signed = { method, api_key: env.LASTFM_API_KEY, ...parameters };
  if (authenticated) signed.api_sig = signature(signed, env.LASTFM_SHARED_SECRET);
  const body = new URLSearchParams({ ...signed, format: "json" });
  const write = method === "track.scrobble" || method === "track.updateNowPlaying";
  const response = await fetch(write ? API : `${API}?${body}`, {
    method: write ? "POST" : "GET", ...(write ? { body } : {}),
    headers: { "user-agent": "VibeArc-LastFm-Signer/1" }, signal: AbortSignal.timeout(15_000),
  });
  const text = await response.text();
  if (!response.ok) throw new Error(`Last.fm request failed (${response.status})`);
  const parsed = JSON.parse(text);
  if (parsed.error) throw new Error(parsed.message || `Last.fm error ${parsed.error}`);
  return { parsed, text };
}

export default {
  async fetch(request, env) {
    try {
      if (request.method !== "POST") return json({ error: "POST required" }, 405);
      if (!authorized(request, env.VIBEARC_CLIENT_TOKEN)) return json({ error: "Unauthorized" }, 401);
      if (!env.LASTFM_API_KEY || !env.LASTFM_SHARED_SECRET) return json({ error: "Signer is not configured" }, 503);
      const route = new URL(request.url).pathname.replace(/^\/+|\/+$/g, "");
      const input = await request.json();
      if (route === "token") {
        const { parsed } = await lastFm(env, "auth.getToken", {}, true);
        return json({ token: parsed.token, authorizationUrl: `https://www.last.fm/api/auth/?api_key=${encodeURIComponent(env.LASTFM_API_KEY)}&token=${encodeURIComponent(parsed.token)}` });
      }
      if (route === "session") {
        const { parsed } = await lastFm(env, "auth.getSession", { token: String(input.token || "") }, true);
        return json({ username: parsed.session.name, sessionKey: parsed.session.key });
      }
      if (route === "profile") {
        const user = String(input.username || "").slice(0, 64);
        const [info, top, recent] = await Promise.all([
          lastFm(env, "user.getInfo", { user }),
          lastFm(env, "user.getTopTracks", { user, limit: "20", period: "overall" }),
          lastFm(env, "user.getRecentTracks", { user, limit: "20", extended: "0" }),
        ]);
        return json({ info: info.text, top: top.text, recent: recent.text });
      }
      if (route === "similar") {
        const result = await lastFm(env, "track.getSimilar", {
          artist: String(input.artist || "").slice(0, 256),
          track: String(input.track || "").slice(0, 256),
          autocorrect: "1",
          limit: "20",
        });
        return json({ similar: result.text });
      }
      if (route === "now-playing" || route === "scrobble") {
        const parameters = {
          sk: String(input.sessionKey || ""),
          artist: String(input.artist || "").slice(0, 256),
          track: String(input.track || "").slice(0, 256),
          album: String(input.album || "").slice(0, 256),
          duration: String(Math.max(0, Number(input.duration) || 0)),
        };
        if (!parameters.sk || !parameters.artist || !parameters.track) return json({ error: "Missing track or session" }, 400);
        if (route === "scrobble") parameters.timestamp = String(Math.max(1, Number(input.timestamp) || 0));
        const { parsed } = await lastFm(env, route === "scrobble" ? "track.scrobble" : "track.updateNowPlaying", parameters, true);
        const result = route === "scrobble" ? parsed.scrobbles?.scrobble : parsed.nowplaying;
        const ignored = (Array.isArray(result) ? result[0] : result)?.ignoredMessage;
        if (!ignored || Number(ignored.code) !== 0 || (route === "scrobble" && Number(parsed.scrobbles?.["@attr"]?.accepted) !== 1)) {
          return json({ error: ignored?.["#text"] || "Last.fm ignored this submission" }, 422);
        }
        return json({ ok: true });
      }
      return json({ error: "Unknown route" }, 404);
    } catch (error) {
      return json({ error: error instanceof Error ? error.message : "Request failed" }, 502);
    }
  },
};
