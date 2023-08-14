import { setupLogin } from "./common/common.js";

const keyVisibilityElem = document.getElementById("keyVisibility");
const secretVisibilityElem = document.getElementById("secretVisibility");
const videoCallBtn = document.getElementById("videocall");
const voiceCallBtn = document.getElementById("voicecall");
const numberCallBtn = document.getElementById("numbercall");
const sipCallBtn = document.getElementById("sipcall");

export const demo = function (event) {
  event.preventDefault();
  const type = this.id;
  const applicationKey = document.getElementById("key").value;
  const applicationSecret = document.getElementById("secret").value;
  const userid = document.getElementById("userid").value;
  setupLogin(applicationKey, applicationSecret, userid).then(() => {
    if (window) {
      window.location.href = `${type}/index.html`;
    }
  });
};

[videoCallBtn, voiceCallBtn, numberCallBtn, sipCallBtn].forEach((btn) => {
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
