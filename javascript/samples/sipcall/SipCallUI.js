import {
  OUTGOING_RINGTONE,
  ringtone,
  setText,
  showCallQualityWarningEventNotification,
} from "../common/common.js";

export default class SipCallUI {
  constructor() {
    this.audio = new Audio();
    setText("version", `Sinch - Version:  ${Sinch.version}`);
  }

  start() {
    this.setStatus("Sinch client is started");
  }

  onCallProgressing(call) {
    this.setStatus(`Call progressing ${call.remoteUserId}`);
    this.ringToneAudio = ringtone(OUTGOING_RINGTONE);
    this.ringToneAudio.play();
  }

  onCallRinging(call) {
    this.setStatus(`Call ringing ${call.remoteUserId}`);
  }

  onCallAnswered(call) {
    this.setStatus(
      `Call answered ${call.remoteUserId}. Establishing connection...`,
    );
    this.ringToneAudio?.pause();
  }

  onCallEstablished(call) {
    this.playAudio(call);
    this.setStatus(`Call established with ${call.remoteUserId}`);
  }

  onCallEnded(call) {
    this.setStatus(`Call ended ${call.remoteUserId}`);
    setText("call", "Call");
    this.ringToneAudio?.pause();
    this.resetCurrentCall();
  }

  onCallQualityWarningEvent(_call, callQualityWarningEvent) {
    showCallQualityWarningEventNotification(callQualityWarningEvent);
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
