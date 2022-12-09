import {
  CALLING,
  DISABLE,
  ENABLE,
  HIDE,
  IDLE,
  INCOMING_RINGTONE,
  OUTGOING_RINGTONE,
  ringtone,
  setAnswerPulse,
  setState,
  setVisibility,
  SHOW,
  setText,
} from "../common/common.js";

export default class VideoCallUI {
  constructor(sinchClientWrapper) {
    this.sinchClientWrapper = sinchClientWrapper;
    this.handleMakeCallClick();
    setState("call", DISABLE);
    setState("answer", DISABLE);
    setState("hangup", DISABLE);
    setText("version", `Sinch - Version:  ${Sinch.version}`);
  }

  onClientStarted(sinchClient) {
    this.setStatus(`Client started for ${sinchClient.userId}`);
    setText("statusclient-userid", `${sinchClient.userId}`);
    setState("call", ENABLE);
    setVisibility("callcontrol", SHOW);
    setVisibility("calldestination", SHOW);
  }

  async makeCall() {
    const callee = this.getCallee();
    this.setStatus(`Make call to ${callee}`);
    await this.sinchClientWrapper.makeCall(callee);
    setState("call", ENABLE);
    setState("answer", DISABLE);
    setState("hangup", ENABLE);
  }

  onOutboundCall(call) {
    this.playRingtone(OUTGOING_RINGTONE);
    this.handleHangupClick(call);
    this.playStream(call.incomingStream, "incoming", true, true);
    this.playStream(call.outgoingStream, "outgoing", false, false);
    this.muteVideoStream("outgoing-video");
    setState("hangup", ENABLE);
  }

  onIncomingCall(call) {
    this.setStatus(`Incoming call from ${call.remoteUserId}`);
    this.handleAnswerClick(call);
    this.handleHangupClick(call);
    this.playRingtone(INCOMING_RINGTONE);
    setState("call", DISABLE);
    setState("answer", ENABLE);
    setState("hangup", ENABLE);
    setAnswerPulse(CALLING);
    this.playStream(call.incomingStream, "incoming", false, false);
    this.playStream(call.outgoingStream, "outgoing", false, false);
    this.muteVideoStream("outgoing-video");
  }

  onCallProgressing(call) {
    this.setStatus(`Call progressing ${call.remoteUserId}`);
  }

  onCallEstablished(call) {
    this.setStatus(`Call established with ${call.remoteUserId}`);
    this.pauseRingtone();
    setVisibility("videos-container", SHOW);
    setVisibility("calldestination", HIDE);
    setState("call", DISABLE);
    setState("answer", DISABLE);
    setState("hangup", ENABLE);
    setAnswerPulse(IDLE);
  }

  onCallEnded(call) {
    this.setStatus(`Call ended ${call.remoteUserId}`);
    this.pauseRingtone();
    setVisibility("videos-container", HIDE);
    setVisibility("calldestination", SHOW);
    setState("call", ENABLE);
    setState("hangup", DISABLE);
    setState("answer", DISABLE);
    setAnswerPulse(IDLE);
    this.removeVideoStream("outgoing-video");
    this.removeVideoStream("incoming-video");
  }

  playStream(stream, direction, mute = true, emptyContainer = true) {
    const videoElement = document.createElement("video");
    videoElement.setAttribute("id", `${direction}-video`);
    videoElement.setAttribute("class", `${direction}-video`);
    videoElement.srcObject = stream;
    videoElement.autoplay = true;
    videoElement.playsinline = true;
    videoElement.muted = mute;

    const container = document.getElementById("videos-container");
    if (emptyContainer) {
      container.innerHTML = "";
    }

    container.appendChild(videoElement);
  }

  setStatus(text) {
    setText("statusheader", text);
    console.log(`Status: ${text}`);
  }

  setText(id, text) {
    document.getElementById(id).innerHTML = text;
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

  muteVideoStream(id) {
    const video = document.getElementById(id);
    video.muted = true;
  }

  removeVideoStream(id) {
    console.log("Action: Remove videosteam ==>", id);
    const videostream = document.getElementById(id);
    videostream?.remove();
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
