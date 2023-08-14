export default class Crypto {
  static algorithm = { name: "HMAC", hash: { name: "SHA-256" } };

  static convertUTF8ToArrayBuffer(data) {
    return new TextEncoder().encode(data);
  }

  static getKey(secret) {
    return window.crypto.subtle.importKey("raw", secret, this.algorithm, true, [
      "sign",
      "verify",
    ]);
  }

  static async hmacSHA256(data, secret) {
    const key = await this.getKey(secret);
    return crypto.subtle.sign(
      this.algorithm,
      key,
      this.convertUTF8ToArrayBuffer(data),
    );
  }

  static getRandomValues(length) {
    return window.crypto.getRandomValues(new Uint8Array(length));
  }

  static convertArrayBufferToHex(data) {
    return Array.from(new Uint8Array(data), (byte) =>
      // eslint-disable-next-line no-bitwise
      `0${(byte & 0xff).toString(16)}`.slice(-2),
    ).join("");
  }

  static toBase64(data) {
    return btoa(String.fromCharCode.apply(null, data));
  }

  static fromBase64(data) {
    const padding = "=".repeat((4 - (data.length % 4)) % 4);
    const base64 = (data + padding).replace(/-/g, "+").replace(/_/g, "/");

    return new Uint8Array(Array.from(atob(base64)).map((c) => c.charCodeAt(0)));
  }
}
