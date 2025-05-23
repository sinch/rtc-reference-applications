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
  setText,
  showCallQualityWarningEventNotification,
} from "../common/common.js";

export default class VoiceCallUI {
  constructor(sinchPhone) {
    this.sinchPhone = sinchPhone;
    this.audio = new Audio();
    this.handleMakeCallClick();
    this.handleManualStartClick();
    setText("version", `Sinch - Version:  ${Sinch.version}`);
  }

  onIncomingCall(call) {
    this.handleAnswerClick(call);
    this.handleHangupClick(call);
    this.setStatus(`Incoming call from ${call.remoteUserId}`);
    this.playRingtone(INCOMING_RINGTONE);
    setState("call", DISABLE);
    setState("hangup", ENABLE);
    setState("answer", ENABLE);
    setAnswerPulse(CALLING);
    this.playStream(call.incomingStream);
  }

  onClientStarted(sinchClient) {
    this.setStatus(`Sinch - Client started for ${sinchClient.userId}`);
    setState("call", ENABLE);
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

  onCallRinging(call) {
    this.setStatus(`Call ringing ${call.remoteUserId}`);
  }

  onCallAnswered(call) {
    this.setStatus(
      `Call answered ${call.remoteUserId}. Establishing connection...`,
    );
    this.pauseRingtone();
    setState("call", DISABLE);
    setState("answer", DISABLE);
    setState("hangup", ENABLE);
    setAnswerPulse(IDLE);
  }

  onCallEstablished(call) {
    this.setStatus(`Call established with ${call.remoteUserId}`);
  }

  onCallEnded(call) {
    this.setStatus(`Call ended ${call.remoteUserId}`);
    this.pauseRingtone();
    setState("hangup", DISABLE);
    setState("call", ENABLE);
    setState("answer", DISABLE);
    setAnswerPulse(IDLE);
  }

  onCallQualityWarningEvent(_call, callQualityWarningEvent) {
    showCallQualityWarningEventNotification(callQualityWarningEvent);
  }

  playStream(stream) {
    this.audio.srcObject = stream;
    this.audio.autoplay = true;
  }

  setStatus(text) {
    setText("statusheader", text);
    console.log(`Status: ${text}`);
  }

  handleMakeCallClick() {
    document
      .getElementById("call")
      .addEventListener("click", () => this.makeCall());
  }

  handleManualStartClick() {
    document
      .getElementById("startclientbutton")
      .addEventListener("click", () => this.sinchPhone.startManually());
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
    this.handleAnswer = () => {
      setState("answer", DISABLE);
      call.answer();
    };
    answerElement.addEventListener("click", this.handleAnswer);
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

  setManualStartButtonVisible(isVisible) {
    const startClientButton = document.getElementById("startclientbutton");
    startClientButton.style.display = isVisible ? "block" : "none";
  }
}
