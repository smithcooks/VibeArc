import assert from "node:assert/strict";
import test from "node:test";
import worker from "./worker.js";

const env = { LASTFM_API_KEY: "test-key", LASTFM_SHARED_SECRET: "test-secret", VIBEARC_CLIENT_TOKEN: "client-token" };
const request = (route, body = {}, token = env.VIBEARC_CLIENT_TOKEN) => new Request(`https://signer.example/${route}`, {
  method: "POST", headers: { authorization: `Bearer ${token}`, "content-type": "application/json" }, body: JSON.stringify(body),
});
const song = { sessionKey: "session", artist: "Artist", track: "Song", duration: 180, timestamp: 1_800_000_000 };

test("unequal UTF-8 bearer lengths are rejected without throwing", async () => {
  const response = await worker.fetch(request("token", {}, "é"), { ...env, VIBEARC_CLIENT_TOKEN: "x" });
  assert.equal(response.status, 401);
});

test("authorization uses a signed GET and returns a Last.fm authorization URL", async () => {
  const original = globalThis.fetch;
  globalThis.fetch = async (url, options) => {
    assert.equal(options.method, "GET");
    const params = new URL(url).searchParams;
    assert.equal(params.get("method"), "auth.getToken");
    assert.match(params.get("api_sig"), /^[a-f0-9]{32}$/);
    return Response.json({ token: "token" });
  };
  try {
    const response = await worker.fetch(request("token"), env);
    assert.equal(response.status, 200);
    assert.equal(new URL((await response.json()).authorizationUrl).hostname, "www.last.fm");
  } finally { globalThis.fetch = original; }
});

test("ignored scrobbles and Now Playing are not reported as accepted", async () => {
  const original = globalThis.fetch;
  try {
    for (const route of ["scrobble", "now-playing"]) {
      globalThis.fetch = async () => Response.json(route === "scrobble"
        ? { scrobbles: { "@attr": { accepted: "0", ignored: "1" }, scrobble: { ignoredMessage: { code: "1", "#text": "Artist ignored" } } } }
        : { nowplaying: { ignoredMessage: { code: "2", "#text": "Track ignored" } } });
      const response = await worker.fetch(request(route, song), env);
      assert.equal(response.status, 422);
      assert.match((await response.json()).error, /ignored/i);
    }
    globalThis.fetch = async () => Response.json({ scrobbles: { "@attr": { accepted: "1", ignored: "0" }, scrobble: { ignoredMessage: { code: "0" } } } });
    assert.equal((await worker.fetch(request("scrobble", song), env)).status, 200);
    globalThis.fetch = async () => Response.json({ nowplaying: { ignoredMessage: { code: "0" } } });
    assert.equal((await worker.fetch(request("now-playing", song), env)).status, 200);
  } finally { globalThis.fetch = original; }
});
