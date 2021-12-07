import JWT from "./jwt.js";
import { getSecret, getKey } from "./token.js";

const APPLICATION_SECRET = getSecret();
export const APPLICATION_KEY = getKey();
export const API_URL = "https://ocra.api.sinch.com";
export const INCOMING_RINGTONE = "INCOMING_RINGTONE";
export const OUTGOING_RINGTONE = "OUTGOING_RINGTONE";
export const HIDE = "HIDE";
export const SHOW = "SHOW";
export const ENABLE = "ENABLE";
export const DISABLE = "DISABLE";
export const CALLING = "CALLING";
export const IDLE = "IDLE";

/**
 * The recommended way to implement this authentication scheme is that the Application Secret
 * should be kept securely on your server-side backend, the signed token should be created and
 * signed on your server, then passed via a secure channel to the application instance and
 * Sinch client running on a device.
 */
export const generateJwtToken = (userId) =>
  new JWT(APPLICATION_KEY, APPLICATION_SECRET, userId).toJwt();

export const ringtone = (type) => {
  if (type === INCOMING_RINGTONE) {
    const audio = new Audio("../common/sounds/incoming-ringtone.wav");
    audio.loop = true;
    return audio;
  }
  if (type === OUTGOING_RINGTONE) {
    const audio = new Audio("../common/sounds/outgoing-ringtone.wav");
    audio.loop = true;
    return audio;
  }
  throw new Error("Ringtone type not supported");
};

export const setState = (id, state) => {
  if (state === ENABLE) {
    document.getElementById(id).removeAttribute("disabled", true);
  } else if (state === DISABLE) {
    document.getElementById(id).setAttribute("disabled", true);
  } else {
    throw new Error("State not supported");
  }
};

export const setVisibility = (id, state, type) => {
  if (state === HIDE) {
    document.getElementById(id).style.display = "none";
  } else if (state === SHOW) {
    document.getElementById(id).style.display = type ?? "block";
  } else {
    throw new Error("State not supported");
  }
};

export const setAnswerPulse = (state) => {
  const answerElementClassList = document.getElementById("answer").classList;
  if (state === CALLING) {
    answerElementClassList.add("pulse");
  } else if (state === IDLE) {
    answerElementClassList.remove("pulse");
  } else {
    throw new Error("State not supported");
  }
};

export const setText = (id, text) => {
  document.getElementById(id).innerHTML = text;
};

export const userId = () => {
  const date = new Date();
  return `in-app-demo-consumer-${date.getTime()}`;
};
