import {
  setupLogin,
  isIOSBrowserNotInStandaloneMode,
  isPushPermissionStatusDenied,
  API_URL,
} from "./common/common.js";

const keyVisibilityElem = document.getElementById("keyVisibility");
const secretVisibilityElem = document.getElementById("secretVisibility");
const videoCallBtn = document.getElementById("videocall");
const voiceCallBtn = document.getElementById("voicecall");
const numberCallBtn = document.getElementById("numbercall");
const sipCallBtn = document.getElementById("sipcall");
const conferenceCallBtn = document.getElementById("conferencecall");
const erroContainer = document.getElementById("errorContainer");
const errorMessage = document.getElementById("errorMessage");
const loginForm = document.getElementById("loginForm");
const environmentHostInput = document.getElementById("environment-host");

environmentHostInput.value = API_URL;

const loadConfig = async () => {
  try {
    const response = await fetch("./config.json");
    if (!response.ok) return;
    const configs = await response.json();
    if (!Array.isArray(configs) || configs.length === 0) return;

    const select = document.getElementById("env-preset");
    const presetGroup = document.getElementById("presetGroup");

    configs.forEach((cfg, i) => {
      const option = document.createElement("option");
      option.value = i;
      option.textContent = cfg.name;
      select.appendChild(option);
    });

    presetGroup.style.display = "block";

    const applyPreset = (idx) => {
      if (idx === "") {
        document.getElementById("key").value = "";
        document.getElementById("secret").value = "";
        document.getElementById("environment-host").value = API_URL;
      } else {
        const cfg = configs[parseInt(idx, 10)];
        document.getElementById("key").value = cfg.appKey ?? "";
        document.getElementById("secret").value = cfg.appSecret ?? "";
        document.getElementById("environment-host").value =
          cfg.environment ?? API_URL;
      }
    };

    select.value = "0";
    applyPreset("0");

    select.addEventListener("change", () => applyPreset(select.value));
  } catch {
    // config.json not found or invalid — manual entry only
  }
};

export const demo = function (event) {
  event.preventDefault();
  const type = this.id;
  const applicationKey = document.getElementById("key").value;
  const applicationSecret = document.getElementById("secret").value;
  const userid = document.getElementById("userid").value;
  const environmentHost = document.getElementById("environment-host").value;
  setupLogin(applicationKey, applicationSecret, userid, environmentHost).then(
    () => {
      if (window) {
        window.location.href = `${type}/index.html`;
      }
    },
  );
};

const showError = (message) => {
  erroContainer.style.display = "block";
  loginForm.style.display = "none";
  errorMessage.innerHTML = message;
};

[
  videoCallBtn,
  voiceCallBtn,
  numberCallBtn,
  sipCallBtn,
  conferenceCallBtn,
].forEach((btn) => {
  btn.addEventListener("click", demo);
});

export const toggleSecretVisibility = (id, visibilityElementId) => () => {
  const elem = document.getElementById(id);
  const visibilityElement = document.getElementById(visibilityElementId);

  if (!elem || !visibilityElement) return;
  if (elem.type === "password") {
    elem.type = "text";
    visibilityElement.innerHTML = "visibility_off";
  } else {
    elem.type = "password";
    visibilityElement.innerHTML = "visibility";
  }
};

keyVisibilityElem.addEventListener(
  "click",
  toggleSecretVisibility("key", "keyVisibility"),
);

secretVisibilityElem.addEventListener(
  "click",
  toggleSecretVisibility("secret", "secretVisibility"),
);

if (isIOSBrowserNotInStandaloneMode()) {
  showError(
    "The application is not running in standalone mode. Please open this page in Safari browser and add to home screen to run the app.",
  );
}

if (await isPushPermissionStatusDenied()) {
  showError(
    "Push notifications are denied. Please enable push notifications in the browser settings.",
  );
}

loadConfig();
