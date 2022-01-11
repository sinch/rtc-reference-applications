import {
  OUTGOING_RINGTONE,
  ringtone,
  setText,
  setVisibility,
  HIDE,
  SHOW,
} from "../common/common.js";

export default class NumberCallUI {
  constructor(sinchPhone) {
    this.audio = new Audio();
    this.setupDialerInput();
    this.handleStartClientClick(sinchPhone);
    setText("version", `Sinch - Version:  ${Sinch.version}`);
    setVisibility("sinchclient", SHOW);
    setVisibility("call-destination", HIDE);
  }

  handleStartClientClick(sinchPhone) {
    document.getElementById("start-client").addEventListener("click", () => {
      sinchPhone.startSinchClient(this.getCallerIdentifier());
      setVisibility("sinchclient", HIDE);
      setVisibility("call-destination", SHOW);
      setVisibility("dialer", SHOW, "grid");
      this.setStatus("Sinch client is started");
    });
  }

  onCallProgressing() {
    this.setStatus("Call Progressing...");
    this.ringToneAudio = ringtone(OUTGOING_RINGTONE);
    this.ringToneAudio.play();
  }

  onCallEstablished(call) {
    this.playAudio(call);
    this.setStatus(`Call Established with ${call.remoteUserId}`);
    this.ringToneAudio?.pause();
  }

  onCallEnded() {
    this.setStatus("Call Disconnected");
    setText("call", "Call");
    this.ringToneAudio?.pause();
    this.resetCurrentCall();
  }

  playAudio(call) {
    this.audio.autoplay = true;
    this.audio.srcObject = call.incomingStream;
  }

  setupDialerInput() {
    const dialerInput = document.querySelectorAll(".btn-dialer");
    dialerInput.forEach((button) => {
      button.addEventListener("click", (event) => {
        const tone = event.target.innerHTML;
        const toValue = document.getElementById("to").value;
        const targetId = event.target.id;
        let newToValue = toValue + tone;
        if (targetId === "clear") {
          newToValue = toValue.slice(0, -1);
        }
        document.getElementById("to").value = newToValue;
        if (this.currentCall && targetId.startsWith("dtmf")) {
          console.log(`Placeholder for SendDTMF ==>${tone}`);
          new Audio(`../common/sounds/${event.target.id}.mp3`).play();
          this.currentCall.sendDtmf(tone);
        }
      });
    });
  }

  getInput(field, button, resolve) {
    document.getElementById(button).addEventListener("click", (e) => {
      e.preventDefault();
      if (this.currentCall) {
        this.currentCall.hangup();
      } else {
        resolve(document.getElementById(field).value);
      }
    });
  }

  getCallerIdentifier() {
    return document.getElementById("caller-identifier").value;
  }

  resetCurrentCall() {
    this.currentCall = null;
  }

  setCurrentCall(call) {
    this.currentCall = call;
  }

  setStatus(text) {
    setText("statusheader", text);
    console.log(text);
  }
}
