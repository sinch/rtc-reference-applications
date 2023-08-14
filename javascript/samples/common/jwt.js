import Crypto from "./crypto.js";

export default class JWT {
  constructor(key, secret, username) {
    this.key = key;
    this.username = username;
    this.iat = new Date();
    this.base64Secret = secret;
  }

  // JWT header parameter kid as "kid": "hkdfv1-<YYYYMMDD>", e.g. "kid": "hkdfv1-20181206"
  // The prefix hkdfv1 comes from that the key derivation function used is a form of HKDF RFC-5869 https://tools.ietf.org/html/rfc5869 and will be used by the Authorization Server to select the same key derivation function and input(date).
  deriveApplicationKeyId(issuedAt) {
    return `hkdfv1-${this.formatDate(issuedAt)}`;
  }

  // Format date as YYYYMMDD
  formatDate(date) {
    return date.toISOString().replaceAll("-", "").substring(0, 8);
  }

  // JWT Headers
  headers() {
    return {
      alg: "HS256",
      type: "JWT",
      kid: this.deriveApplicationKeyId(this.iat),
    };
  }

  // JWT Payload
  payload() {
    const nonce = Crypto.convertArrayBufferToHex(Crypto.getRandomValues(16));
    return {
      iss: `//rtc.sinch.com/applications/${this.key}`,
      sub: `//rtc.sinch.com/applications/${this.key}/users/${this.username}`,
      iat: this.convertToSeconds(this.iat),
      exp: this.convertToSeconds(this.iat) + 600000,
      nonce,
    };
  }

  sortObject(object) {
    const sorted = {};
    Object.keys(object)
      .sort()
      .forEach((key) => {
        sorted[key] = object[key];
      });
    return sorted;
  }

  // Create signature from headers and payload
  async signToken(headers, payload, signingKey) {
    const signature = await Crypto.hmacSHA256(
      `${headers}.${payload}`,
      signingKey,
    );
    return this.makeURLSafe(Crypto.toBase64(new Uint8Array(signature)));
  }

  async toJwt() {
    const date = this.formatDate(this.iat);
    const signingKey = await Crypto.hmacSHA256(
      date,
      Crypto.fromBase64(this.base64Secret),
    );
    const encodedHeaders = this.convertObjectToBase64(
      this.sortObject(this.headers()),
    );
    const encodedPayload = this.convertObjectToBase64(
      this.sortObject(this.payload()),
    );
    const signature = await this.signToken(
      encodedHeaders,
      encodedPayload,
      signingKey,
    );
    return `${encodedHeaders}.${encodedPayload}.${signature}`;
  }

  convertObjectToBase64(str) {
    const bytes = Crypto.convertUTF8ToArrayBuffer(JSON.stringify(str));
    const base64 = Crypto.toBase64(bytes);
    return this.makeURLSafe(base64);
  }

  makeURLSafe(u) {
    // JWT RFC 7515 specifies that base64 encoding without padding should be used.
    return u.replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
  }

  convertToSeconds(date) {
    return Math.round(date.getTime() / 1000);
  }
}
