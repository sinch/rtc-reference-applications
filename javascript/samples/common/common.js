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
export const ENVIRONMENT_HOST_KEY = `${STORAGE_KEY_BASE}environmenthost`;

const PERMISSION_STATUS_PROMPT = "prompt";
const PERMISSION_STATUS_DENIED = "denied";
const PERMISSION_STATUS_GRANTED = "granted";

/**
 * The recommended way to implement this authentication scheme is that the Application Secret
 * should be kept securely on your server-side backend, the signed token should be created and
 * signed on your server, then passed via a secure channel to the application instance and
 * Sinch client running on a device.
 */
export const setupLogin = async (
  applicationKey,
  applicationSecret,
  userId,
  environmentHost,
) => {
  const jwtToken = await new JWT(
    applicationKey,
    applicationSecret,
    userId,
  ).toJwt();
  localStorage.setItem(JWT_TOKEN_KEY, jwtToken);
  localStorage.setItem(APPLICATION_KEY, applicationKey);
  localStorage.setItem(USER_ID_KEY, userId);
  localStorage.setItem(ENVIRONMENT_HOST_KEY, environmentHost || API_URL);
};

export function getEnvironmentHost() {
  return localStorage.getItem(ENVIRONMENT_HOST_KEY) || API_URL;
}

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

const muteState = { isMuted: false, currentCall: null };

export function initMuteButton() {
  document.getElementById("mute").addEventListener("click", (e) => {
    e.preventDefault();
    if (!muteState.currentCall) return;
    if (muteState.isMuted) {
      muteState.currentCall.unmute();
      muteState.isMuted = false;
      setText("mute", "Mute");
    } else {
      muteState.currentCall.mute();
      muteState.isMuted = true;
      setText("mute", "Unmute");
    }
  });
}

export function enableMute(call) {
  muteState.currentCall = call;
  setState("mute", ENABLE);
}

export function resetMute() {
  muteState.isMuted = false;
  muteState.currentCall = null;
  setState("mute", DISABLE);
  setText("mute", "Mute");
}

const ECHO_PARAMS = { delaySeconds: 0.3, feedback: 0.4 };

// ---- Local echo -----------------------------------------------------------
// Applies an echo effect to the OUTGOING microphone audio via the SDK's local audio frame
// listener. The processing runs in the echo-processor AudioWorklet; when disabled the SDK
// restores the raw microphone. This is what the remote peer hears.

const localEchoState = { enabled: false, currentCall: null };
// Serializes local echo toggles so a rapid second click can't read a stale `enabled` while a
// previous setLocalAudioFrameListener call is still in flight.
let localEchoToggle = Promise.resolve();

const ECHO_LISTENER = {
  moduleUrl: "../common/audio/echo-processor.js",
  processorName: "echo-processor",
  processorOptions: ECHO_PARAMS,
};

async function applyLocalEchoToggle(button, call) {
  if (!call || localEchoState.currentCall !== call) return;
  const enable = !localEchoState.enabled;
  try {
    await call.setLocalAudioFrameListener(enable ? ECHO_LISTENER : null);
    if (localEchoState.currentCall !== call) return;
    localEchoState.enabled = enable;
    button.classList.toggle("btn-active", enable);
    showNotification({
      message: enable
        ? "Local echo enabled – the remote peer now hears your microphone with an echo effect."
        : "Local echo disabled.",
      isSuccess: true,
    });
  } catch (error) {
    console.error("Error toggling local echo:", error);
    if (localEchoState.currentCall !== call) return;
    showNotification({
      message: "Failed to toggle local echo.",
      isSuccess: false,
    });
  }
}

export function initEchoButton() {
  const echoButton = document.getElementById("local-echo");
  if (!echoButton) return;
  echoButton.addEventListener("click", () => {
    const call = localEchoState.currentCall;
    localEchoToggle = localEchoToggle
      .then(() => applyLocalEchoToggle(echoButton, call))
      .catch(() => undefined);
  });
}

export function enableEcho(call) {
  localEchoState.currentCall = call;
  localEchoState.enabled = false;
  document.getElementById("local-echo")?.classList.remove("btn-active");
  setState("local-echo", ENABLE);
}

export function resetEcho() {
  if (localEchoState.currentCall && localEchoState.enabled) {
    localEchoState.currentCall
      .setLocalAudioFrameListener(null)
      .catch(() => undefined);
  }
  localEchoState.enabled = false;
  localEchoState.currentCall = null;
  document.getElementById("local-echo")?.classList.remove("btn-active");
  setState("local-echo", DISABLE);
}

// ---- Remote echo (reference-app level) --------------------
// Applies an echo effect to the INCOMING (remote) audio purely on the SDK host app side by routing the
// call's incomingStream through a Web Audio graph. This only affects what the LOCAL user hears;
// the SDK is not involved.
//
// The processed audio is played back through the sample's own
// media element, so the selected output device (element.setSinkId) is honored and stays in sync
// with the audioOutput selector. The raw remote stream is kept on a separate muted sink element so
// the Web Audio source keeps pulling frames (Chrome remote-track quirk).

const remoteEchoState = {
  enabled: false,
  currentCall: null,
  mediaElement: null,
  audioContext: null,
  rawSink: null,
};

function stopRemoteEcho() {
  const { audioContext, mediaElement, currentCall, rawSink } = remoteEchoState;
  if (audioContext) {
    audioContext.close();
    remoteEchoState.audioContext = null;
  }
  if (rawSink) {
    rawSink.srcObject = null;
    remoteEchoState.rawSink = null;
  }
  if (mediaElement && currentCall?.incomingStream) {
    mediaElement.srcObject = currentCall.incomingStream;
  }
}

// Returns true if the echo graph was successfully started and is now playing.
function startRemoteEcho() {
  const { currentCall, mediaElement } = remoteEchoState;
  const stream = currentCall?.incomingStream;
  if (!stream || !mediaElement) return false;
  // Close any previously opened context/sink before creating a new one, so we never leak a
  // context or run two graphs on the same incoming stream.
  stopRemoteEcho();
  try {
    const ctx = new AudioContext();
    remoteEchoState.audioContext = ctx;

    const source = ctx.createMediaStreamSource(stream);
    const dest = ctx.createMediaStreamDestination();
    const delay = ctx.createDelay(1.0);
    delay.delayTime.value = ECHO_PARAMS.delaySeconds;
    const feedback = ctx.createGain();
    feedback.gain.value = ECHO_PARAMS.feedback;

    source.connect(dest);
    source.connect(delay);
    delay.connect(feedback);
    feedback.connect(delay);
    delay.connect(dest);

    mediaElement.srcObject = new MediaStream([
      ...dest.stream.getAudioTracks(),
      ...stream.getVideoTracks(),
    ]);

    const rawSink = new Audio();
    rawSink.muted = true;
    rawSink.srcObject = stream;
    rawSink.play().catch(() => undefined);
    remoteEchoState.rawSink = rawSink;

    ctx.resume();
    return true;
  } catch (error) {
    console.error("Error starting remote echo:", error);
    stopRemoteEcho();
    return false;
  }
}

export function initRemoteEchoButton() {
  const button = document.getElementById("remote-echo");
  if (!button) return;
  button.addEventListener("click", () => {
    if (!remoteEchoState.currentCall) return;
    const enable = !remoteEchoState.enabled;
    if (enable && !startRemoteEcho()) {
      showNotification({
        message: "Could not enable remote echo.",
        isSuccess: false,
      });
      return;
    }
    if (!enable) stopRemoteEcho();
    remoteEchoState.enabled = enable;
    button.classList.toggle("btn-active", enable);
    showNotification({
      message: enable
        ? "Remote echo enabled – you now hear the remote audio with an echo effect."
        : "Remote echo disabled.",
      isSuccess: true,
    });
  });
}

export function enableRemoteEcho(call, mediaElement) {
  stopRemoteEcho();
  remoteEchoState.currentCall = call;
  remoteEchoState.mediaElement = mediaElement;
  remoteEchoState.enabled = false;
  document.getElementById("remote-echo")?.classList.remove("btn-active");
  setState("remote-echo", ENABLE);
}

// The muted raw sink above (Chrome remote-track quirk workaround) makes WebRTC report a
// zero inbound audio level, which triggers a false ZeroInboundAudioLevel warning while
// remote echo is active. Check this in onCallQualityWarningEvent to filter it out.
export function isRemoteEchoActive() {
  return remoteEchoState.enabled;
}

export function resetRemoteEcho() {
  stopRemoteEcho();
  remoteEchoState.currentCall = null;
  remoteEchoState.mediaElement = null;
  remoteEchoState.enabled = false;
  document.getElementById("remote-echo")?.classList.remove("btn-active");
  setState("remote-echo", DISABLE);
}

const BW_WORKER_URL = "../common/video/bw-worker.js";

// ---- Local B&W ------------------------------------------------------------
// Turns the OUTGOING camera video black and white via the SDK's local video frame listener. The
// conversion runs in bw-worker; when disabled the SDK restores the raw camera. This is what the
// remote peer sees. Requires a browser with the Insertable Streams API.

const localBwState = { enabled: false, currentCall: null };
// Serializes local B&W toggles so a rapid second click can't read a stale `enabled` while a
// previous setLocalVideoFrameListener call is still in flight.
let localBwToggle = Promise.resolve();

const BW_LISTENER = { workerUrl: BW_WORKER_URL };

async function applyLocalBwToggle(button, call) {
  if (!call || localBwState.currentCall !== call) return;
  const enable = !localBwState.enabled;
  try {
    await call.setLocalVideoFrameListener(enable ? BW_LISTENER : null);
    if (localBwState.currentCall !== call) return;
    localBwState.enabled = enable;
    button.classList.toggle("btn-active", enable);
    showNotification({
      message: enable
        ? "Local B&W enabled – the remote peer now sees your camera in black and white."
        : "Local B&W disabled.",
      isSuccess: true,
    });
  } catch (error) {
    console.error("Error toggling local B&W:", error);
    if (localBwState.currentCall !== call) return;
    showNotification({
      message: `Failed to toggle local B&W. ${error?.message ?? ""}`,
      isSuccess: false,
    });
  }
}

export function initLocalBwButton() {
  const button = document.getElementById("local-bw");
  if (!button) return;
  button.addEventListener("click", () => {
    const call = localBwState.currentCall;
    localBwToggle = localBwToggle
      .then(() => applyLocalBwToggle(button, call))
      .catch(() => undefined);
  });
}

export function enableLocalBw(call) {
  localBwState.currentCall = call;
  localBwState.enabled = false;
  document.getElementById("local-bw")?.classList.remove("btn-active");
  setState("local-bw", ENABLE);
}

export function resetLocalBw() {
  if (localBwState.currentCall && localBwState.enabled) {
    localBwState.currentCall
      .setLocalVideoFrameListener(null)
      .catch(() => undefined);
  }
  localBwState.enabled = false;
  localBwState.currentCall = null;
  document.getElementById("local-bw")?.classList.remove("btn-active");
  setState("local-bw", DISABLE);
}

// ---- Remote B&W (reference-app level) -------------------------------------
// Turns the INCOMING video black and white purely on the SDK host app side, by running the
// received track through the same bw-worker with Insertable Streams. This only affects what the
// LOCAL user sees; the SDK is not involved. Requires the Insertable Streams API.
//
// Remote B&W and remote echo both drive the incoming media element, so whichever is enabled last
// replaces the other's stream.

const remoteBwState = {
  enabled: false,
  currentCall: null,
  mediaElement: null,
  worker: null,
  displayStream: null,
};

// The element plays a stream this sample owns, so toggling only swaps its video track. Assigning
// `srcObject` per toggle instead makes the element reload, which shows up as a flash.
function remoteDisplayStream() {
  const { mediaElement, currentCall, displayStream } = remoteBwState;
  if (displayStream && mediaElement.srcObject === displayStream) {
    return displayStream;
  }
  const source = mediaElement.srcObject ?? currentCall.incomingStream;
  remoteBwState.displayStream = new MediaStream(source.getTracks());
  mediaElement.srcObject = remoteBwState.displayStream;
  return remoteBwState.displayStream;
}

function setRemoteVideoTrack(track) {
  const stream = remoteDisplayStream();
  const previous = stream
    .getVideoTracks()
    .filter((existing) => existing !== track);
  stream.addTrack(track);
  previous.forEach((existing) => stream.removeTrack(existing));
}

function stopRemoteBw() {
  const { worker, mediaElement, currentCall } = remoteBwState;
  if (worker) {
    worker.terminate();
    remoteBwState.worker = null;
  }
  const rawVideoTrack = currentCall?.incomingStream?.getVideoTracks()[0];
  if (mediaElement && rawVideoTrack) setRemoteVideoTrack(rawVideoTrack);
}

/* global MediaStreamTrackProcessor, MediaStreamTrackGenerator */
// Returns true if the conversion was successfully started and is now playing.
function startRemoteBw() {
  const { currentCall, mediaElement } = remoteBwState;
  const videoTrack = currentCall?.incomingStream?.getVideoTracks()[0];
  if (!videoTrack || !mediaElement) return false;
  try {
    const worker = new Worker(BW_WORKER_URL);
    const processor = new MediaStreamTrackProcessor({ track: videoTrack });
    const generator = new MediaStreamTrackGenerator({ kind: "video" });
    worker.postMessage(
      {
        type: "transform",
        readable: processor.readable,
        writable: generator.writable,
      },
      [processor.readable, generator.writable],
    );
    // Release any previous worker so we never run two conversions on one track.
    remoteBwState.worker?.terminate();
    remoteBwState.worker = worker;
    setRemoteVideoTrack(generator);
    return true;
  } catch (error) {
    console.error("Error starting remote B&W:", error);
    stopRemoteBw();
    return false;
  }
}

export function initRemoteBwButton() {
  const button = document.getElementById("remote-bw");
  if (!button) return;
  button.addEventListener("click", () => {
    if (!remoteBwState.currentCall) return;
    const enable = !remoteBwState.enabled;
    if (enable && !startRemoteBw()) {
      showNotification({
        message: "Could not enable remote B&W.",
        isSuccess: false,
      });
      return;
    }
    if (!enable) stopRemoteBw();
    remoteBwState.enabled = enable;
    button.classList.toggle("btn-active", enable);
    showNotification({
      message: enable
        ? "Remote B&W enabled – you now see the remote video in black and white."
        : "Remote B&W disabled.",
      isSuccess: true,
    });
  });
}

export function enableRemoteBw(call, mediaElement) {
  stopRemoteBw();
  remoteBwState.currentCall = call;
  remoteBwState.mediaElement = mediaElement;
  remoteBwState.displayStream = null;
  remoteBwState.enabled = false;
  remoteDisplayStream();
  document.getElementById("remote-bw")?.classList.remove("btn-active");
  setState("remote-bw", ENABLE);
}

export function resetRemoteBw() {
  stopRemoteBw();
  remoteBwState.currentCall = null;
  remoteBwState.mediaElement = null;
  remoteBwState.displayStream = null;
  remoteBwState.enabled = false;
  document.getElementById("remote-bw")?.classList.remove("btn-active");
  setState("remote-bw", DISABLE);
}
