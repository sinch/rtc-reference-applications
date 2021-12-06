import {
  CALLING,
  DISABLE,
  ENABLE,
  IDLE,
  INCOMING_RINGTONE,
  OUTGOING_RINGTONE,
  ringtone,
  setAnswerPulse,
  setState,
  setVisibility,
  SHOW,
  HIDE,
  setText,
} from "../common/common.js";

export default class VoiceCallUI {
  constructor(sinchPhone) {
    this.sinchPhone = sinchPhone;
    this.audio = new Audio();
    this.handleStartClientClick(sinchPhone);
    this.handleMakeCallClick();
    setText("version", `Sinch - Version:  ${Sinch.version}`);
  }

  onIncomingCall(call) {
    this.handleAnswerClick(call);
    this.handleHangupClick(call);
    this.setStatus(`Incoming call from ${call.origin}`);
    this.playRingtone(INCOMING_RINGTONE);
    setState("call", DISABLE);
    setState("hangup", ENABLE);
    setState("answer", ENABLE);
    setAnswerPulse(CALLING);
    this.playStream(call.incomingStream);
  }

  onClientStarted(sinchClient) {
    this.setStatus(`Sinch - Client started for ${sinchClient.userId}`);
    setState("start-client", DISABLE);
    setState("call", ENABLE);
    setVisibility("sinchclient", HIDE);
    setVisibility("callcontrol", SHOW);
  }

  async makeCall() {
    const callee = this.getCallee();
    this.setStatus(`Make call to ${callee}`);
    await this.sinchPhone.makeCall(callee);
    setState("hangup", ENABLE);
    setState("call", DISABLE);
    setState("answer", DISABLE);
  }

  onOutboundCall(call) {
    this.handleHangupClick(call);
    this.playRingtone(OUTGOING_RINGTONE);
    this.playStream(call.incomingStream);
  }

  onCallProgressing(call) {
    this.setStatus(`Call progressing ${call.remoteUserId}`);
  }

  onCallEstablished(call) {
    this.setStatus(`Call established with ${call.remoteUserId}`);
    setState("call", DISABLE);
    setState("answer", DISABLE);
    setState("hangup", ENABLE);
    setAnswerPulse(IDLE);
    this.pauseRingtone();
  }

  onCallEnded(call) {
    this.setStatus(`Call ended ${call.remoteUserId}`);
    this.pauseRingtone();
    setState("hangup", DISABLE);
    setState("call", ENABLE);
    setState("answer", DISABLE);
    setAnswerPulse(IDLE);
  }

  playStream(stream) {
    this.audio.srcObject = stream;
    this.audio.autoplay = true;
  }

  setStatus(text) {
    setText("statusheader", text);
    console.log(`Status: ${text}`);
  }

  handleStartClientClick() {
    document.getElementById("start-client").addEventListener("click", () => {
      const userId = this.getUserId();
      this.sinchPhone.startSinchClient(userId);
    });
  }

  handleMakeCallClick() {
    document
      .getElementById("call")
      .addEventListener("click", () => this.makeCall());
  }

  handleHangupClick(call) {
    const hangupElement = document.getElementById("hangup");
    hangupElement.removeEventListener("click", this.handleHangup);
    this.handleHangup = () => call.hangup();
    hangupElement.addEventListener("click", this.handleHangup);
  }

  handleAnswerClick(call) {
    const answerElement = document.getElementById("answer");
    answerElement.removeEventListener("click", this.handleAnswer);
    this.handleAnswer = () => call.answer();
    answerElement.addEventListener("click", this.handleAnswer);
  }

  getUserId() {
    return document.getElementById("userId").value;
  }

  getCallee() {
    return document.getElementById("callee").value;
  }

  playRingtone(directionRingtone) {
    this.ringToneAudio = ringtone(directionRingtone);
    this.ringToneAudio.play();
  }

  pauseRingtone() {
    this.ringToneAudio?.pause();
  }
}
