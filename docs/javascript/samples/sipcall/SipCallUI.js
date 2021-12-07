import { OUTGOING_RINGTONE, ringtone, setText } from "../common/common.js";

export default class SipCallUI {
  constructor() {
    this.audio = new Audio();
    setText("version", `Sinch - Version:  ${Sinch.version}`);
  }

  start() {
    this.setStatus("Sinch client is started");
  }

  onCallProgressing() {
    this.setStatus("Call Progressing...");

    this.ringToneAudio = ringtone(OUTGOING_RINGTONE);
    this.ringToneAudio.play();
  }

  onCallEstablished(call) {
    this.playAudio(call);
    this.setStatus("Call Established");
    this.ringToneAudio?.pause();
  }

  onCallEnded() {
    this.setStatus("Call Disconnected");
    setText("call", "Call");
    this.ringToneAudio?.pause();
    this.resetCurrentCall();
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

  resetCurrentCall() {
    this.currentCall = null;
  }

  setCurrentCall(call) {
    this.currentCall = call;
  }

  playAudio(call) {
    this.audio.autoplay = true;
    this.audio.srcObject = call.incomingStream;
  }

  setStatus(text) {
    setText("statusheader", text);
    console.log(text);
  }
}
