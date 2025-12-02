import JWT from "./jwt.js";

export const API_URL = "https://ocra.api.sinch.com";
export const INCOMING_RINGTONE = "INCOMING_RINGTONE";
export const OUTGOING_RINGTONE = "OUTGOING_RINGTONE";
export const HIDE = "HIDE";
export const SHOW = "SHOW";
export const ENABLE = "ENABLE";
export const DISABLE = "DISABLE";
export const CALLING = "CALLING";
export const IDLE = "IDLE";
export const STORAGE_KEY_BASE = "sinch:referenceapp:";
export const JWT_TOKEN_KEY = `${STORAGE_KEY_BASE}jwttoken`;
export const APPLICATION_KEY = `${STORAGE_KEY_BASE}applicationkey`;
export const USER_ID_KEY = `${STORAGE_KEY_BASE}userid`;

const PERMISSION_STATUS_PROMPT = "prompt";
const PERMISSION_STATUS_DENIED = "denied";
const PERMISSION_STATUS_GRANTED = "granted";

/**
 * The recommended way to implement this authentication scheme is that the Application Secret
 * should be kept securely on your server-side backend, the signed token should be created and
 * signed on your server, then passed via a secure channel to the application instance and
 * Sinch client running on a device.
 */
export const setupLogin = async (applicationKey, applicationSecret, userId) => {
  const jwtToken = await new JWT(
    applicationKey,
    applicationSecret,
    userId,
  ).toJwt();
  localStorage.setItem(JWT_TOKEN_KEY, jwtToken);
  localStorage.setItem(APPLICATION_KEY, applicationKey);
  localStorage.setItem(USER_ID_KEY, userId);
};

export const getJwtToken = async () => {
  const jwtToken = localStorage.getItem(JWT_TOKEN_KEY);
  if (!jwtToken) {
    throw new Error("JWTToken doesn't exist");
  }
  return jwtToken;
};

export const getApplicationKey = () => {
  const applicationKey = localStorage.getItem(APPLICATION_KEY);
  if (!applicationKey) {
    throw new Error("ApplicationKey doesn't exist");
  }
  return applicationKey;
};

export const getUserId = () => {
  const userId = localStorage.getItem(USER_ID_KEY);
  if (!userId) {
    throw new Error("UserId doesn't exist");
  }
  return userId;
};

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
  const element = document.getElementById(id);
  if (!element) throw new Error(`Element with id "${id}" not found`);

  if (state === ENABLE) {
    element.removeAttribute("disabled");
  } else if (state === DISABLE) {
    element.setAttribute("disabled", true);
  } else {
    throw new Error("State not supported");
  }
  if (window.M && window.M.FormSelect) {
    const instance = window.M.FormSelect.getInstance(element);
    if (instance) {
      window.M.FormSelect.init(element);
    }
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

export const showNotification = ({ message, isSuccess }) => {
  const container = document.getElementById("notification-container");
  if (!container) {
    console.error("Notification container not found");
    return;
  }

  const notification = document.createElement("div");
  notification.textContent = message;
  notification.classList.add("notification");

  if (isSuccess) {
    notification.classList.add("notification-success");
  } else {
    notification.classList.add("notification-error");
  }

  container.appendChild(notification);

  const dismissDurationMs = 300;
  const notificationVisibleDurationMs = 3000;
  setTimeout(() => {
    notification.style.opacity = "0";
    setTimeout(() => notification.remove(), dismissDurationMs);
  }, notificationVisibleDurationMs);
};

export const buildCallQualityWarningMessage = (callQualityWarningEvent) => {
  let message = "Call quality warning ";

  if (callQualityWarningEvent.type === "Trigger") {
    message += `triggered: ${callQualityWarningEvent.name}.`;
  } else {
    message += `recovered: ${callQualityWarningEvent.name}.`;
  }

  if (callQualityWarningEvent.mediaStreamType === "Audio") {
    message += " Associated media stream: Audio";
  } else if (callQualityWarningEvent.mediaStreamType === "Video") {
    message += " Associated media stream: Video";
  }
  return message;
};

export const showCallQualityWarningEventNotification = (
  callQualityWarningEvent,
) => {
  showNotification({
    message: buildCallQualityWarningMessage(callQualityWarningEvent),
    isSuccess: callQualityWarningEvent.type === "Recover",
  });
};

export const isIOSBrowserNotInStandaloneMode = () => {
  const isIOS = /iphone|ipad|ipod/i.test(window.navigator.userAgent);
  const isStandalone = window.navigator.standalone === true;
  return isIOS && !isStandalone;
};

async function getPushPermissionStatus() {
  if (navigator.permissions) {
    try {
      const status = await navigator.permissions.query({
        name: "push",
        userVisibleOnly: true,
      });

      return status.state;
    } catch (err) {
      console.warn(
        "Permissions check using `navigator.permissions` API for push failed:",
        err,
      );
    }
  }
  return typeof Notification !== "undefined"
    ? Notification.permission
    : "unknown";
}

export async function isPushPermissionStatusPrompt() {
  const status = await getPushPermissionStatus();
  return status === PERMISSION_STATUS_PROMPT;
}

export async function isPushPermissionStatusDenied() {
  const status = await getPushPermissionStatus();
  return status === PERMISSION_STATUS_DENIED;
}

/**
 * Verifies whether the Sinch client can be automatically started.
 *
 * During Sinch client initialization, the browser may prompt the user for push notification permissions
 * if they haven't been granted yet. In some browsers, this can result in the error:
 * `Push notification prompting can only be done from a user gesture.`
 *
 * @returns True if the Sinch client can be automatically started; false otherwise.
 */
export async function canAutoStart() {
  const status = await getPushPermissionStatus();
  if (status === PERMISSION_STATUS_GRANTED) {
    return true;
  }

  const ua = window.navigator.userAgent.toLowerCase();
  // Chrome user agent contains "chrome" but so does Edge and Opera
  const isChrome =
    ua.includes("chrome") && !ua.includes("edg") && !ua.includes("opr");
  return isChrome;
}

export function populateDeviceSelectors(devices) {
  const audioInputSelect = document.querySelector("select#audioSource");
  const audioOutputSelect = document.querySelector("select#audioOutput");
  const videoSelect = document.querySelector("select#videoSource");
  const handleVideo = videoSelect !== null && videoSelect !== undefined;
  const selectors = [audioInputSelect, audioOutputSelect, videoSelect].filter(
    Boolean,
  );
  const values = selectors.map((select) => select.value);

  selectors.forEach((select) => {
    const optionsToRemove = Array.from(select.querySelectorAll("option")).slice(
      1,
    );
    optionsToRemove.forEach((opt) => select.removeChild(opt));
  });
  const validDevices = devices.filter((d) => d.deviceId !== "");

  for (let i = 0; i !== validDevices.length; i += 1) {
    const deviceInfo = validDevices[i];
    const option = document.createElement("option");
    option.value = deviceInfo.deviceId;
    if (deviceInfo.kind === "audioinput") {
      option.text =
        deviceInfo.label || `microphone ${audioInputSelect.length + 1}`;
      audioInputSelect.appendChild(option);
    } else if (deviceInfo.kind === "audiooutput") {
      option.text =
        deviceInfo.label || `speaker ${audioOutputSelect.length + 1}`;
      audioOutputSelect.appendChild(option);
    } else if (deviceInfo.kind === "videoinput" && handleVideo) {
      option.text = deviceInfo.label || `camera ${videoSelect.length + 1}`;
      videoSelect.appendChild(option);
    }
  }
  selectors.forEach((s, selectorIndex) => {
    const selector = s;
    if (
      Array.prototype.slice
        .call(selector.childNodes)
        .some((n) => n.value === values[selectorIndex])
    ) {
      selector.value = values[selectorIndex];
    }
  });
  if (window.M && window.M.FormSelect) {
    selectors.forEach((selector) => {
      window.M.FormSelect.init(selector);
    });
  }
}

export function initDeviceSelectors() {
  navigator.mediaDevices
    .enumerateDevices()
    .then(populateDeviceSelectors)
    .catch((error) => {
      console.error("Error while getting devices", error);
    });
}

export function setMediaSource(sinchClient, video) {
  const mediaElementId = video ? "videoSource" : "audioSource";
  const mediaInputSelect = document.getElementById(mediaElementId);
  const mediaInputId = mediaInputSelect.value;

  try {
    if (mediaInputId) {
      if (video) {
        sinchClient.callClient.setVideoTrackConstraints({
          deviceId: { exact: mediaInputId },
        });
      } else {
        sinchClient.callClient.setAudioTrackConstraints({
          deviceId: { exact: mediaInputId },
        });
      }
      console.log(
        `Media constraints applied for ${video ? "video" : "audio"}: ${
          mediaInputSelect.options[mediaInputSelect.selectedIndex].text
        }`,
      );
    }
  } catch (error) {
    console.error("Error applying media constraints:", error);
  }
}

export function setAudioOutput(audioElement) {
  const audioOutputSelect = document.querySelector("select#audioOutput");
  const audioOutputId = audioOutputSelect.value;

  try {
    if (audioOutputId) {
      audioElement.setSinkId(audioOutputId);
      console.log(
        `Audio output set to: ${
          audioOutputSelect.options[audioOutputSelect.selectedIndex].text
        }`,
      );
    }
  } catch (error) {
    console.error("Error setting audio output:", error);
  }
}

export const sleep = (ms) =>
  new Promise((resolve) => {
    setTimeout(resolve, ms);
  });
