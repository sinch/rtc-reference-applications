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
  showCallQualityWarningEventNotification,
  initDeviceSelectors,
  setMediaSource,
  setAudioOutput,
  sleep,
} from "../common/common.js";

export default class VideoCallUI {
  constructor(sinchClientWrapper) {
    this.sinchClientWrapper = sinchClientWrapper;
    this.handleMakeCallClick();
    this.handleManualStartClick();
    this.handleDeviceSelectors();
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
    this.playStream(call.incomingStream, "incoming", false);
    this.playStream(call.outgoingStream, "outgoing", false);
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
    this.playStream(call.incomingStream, "incoming", false);
    this.playStream(call.outgoingStream, "outgoing", false);
    this.muteVideoStream("outgoing-video");
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
    setVisibility("videos-container", SHOW);
    setVisibility("calldestination", HIDE);
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

  onCallQualityWarningEvent(_call, callQualityWarningEvent) {
    showCallQualityWarningEventNotification(callQualityWarningEvent);
  }

  playStream(stream, direction, mute = true) {
    const videoElement = document.getElementById(`${direction}-video`);
    videoElement.setAttribute("class", `${direction}-video`);
    videoElement.playsInline = true;
    videoElement.srcObject = stream;
    videoElement.muted = mute;
    videoElement.setAttribute("playsinline", ""); // Standard attribute
    videoElement.setAttribute("webkit-playsinline", "");
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

  handleManualStartClick() {
    document
      .getElementById("startclientbutton")
      .addEventListener("click", () => this.sinchClientWrapper.startManually());
  }

  handleDeviceSelectors() {
    initDeviceSelectors();
    const audioElement = document.getElementById("incoming-video");
    document
      .getElementById("audioSource")
      .addEventListener("change", () =>
        setMediaSource(this.sinchClientWrapper.sinchClient, false),
      );
    document
      .getElementById("audioOutput")
      .addEventListener("change", () => setAudioOutput(audioElement));
    document
      .getElementById("videoSource")
      .addEventListener("change", () =>
        setMediaSource(this.sinchClientWrapper.sinchClient, true),
      );
    navigator.mediaDevices.ondevicechange = initDeviceSelectors;
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
    this.handleAnswer = async () => {
      setState("answer", DISABLE);
      try {
        await call.answer();
      } catch (error) {
        console.error("Failed to answer call:", error);
        call.hangup();
        await sleep(50);
        this.setStatus(
          `Failed to answer call from ${call.remoteUserId}. Message: ${
            error?.message ?? ""
          }`,
        );
      }
    };
    answerElement.addEventListener("click", this.handleAnswer);
  }

  muteVideoStream(id) {
    const video = document.getElementById(id);
    video.muted = true;
  }

  removeVideoStream(id) {
    console.log("Action: Remove videosteam ==>", id);
    const videostream = document.getElementById(id);
    if (videostream) {
      videostream.srcObject = null;
    }
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
