import {
  canAutoStart,
  getJwtToken,
  getApplicationKey,
  getUserId,
  getEnvironmentHost,
} from "../common/common.js";
import VideoCallUI from "./VideoCallUI.js";

export default class VideoCallSinchClientWrapper {
  constructor() {
    if ("serviceWorker" in navigator) {
      console.log("Service worker started");
      console.log(navigator);
    } else {
      console.error("Service worker not started");
    }
    this.ui = new VideoCallUI(this);

    this.sinchClient = Sinch.getSinchClientBuilder()
      .applicationKey(getApplicationKey())
      .userId(getUserId())
      .environmentHost(getEnvironmentHost())
      .build();
    this.sinchClient.addListener(this.#sinchClientListener());
    this.sinchClient.setSupportManagedPush().then(() => {
      this.attemptAutoStart();
    });
  }

  async makeCall(callee) {
    const call = await this.sinchClient.callClient.callUserVideo(callee);
    this.ui.onOutboundCall(call);
    this.#callListeners(call);
  }

  async attemptAutoStart() {
    if (await canAutoStart()) {
      this.sinchClient.start();
    } else {
      this.ui.setManualStartButtonVisible(true);
    }
  }

  startManually() {
    this.sinchClient.start();
    this.ui.setManualStartButtonVisible(false);
  }

  #sinchClientListener() {
    return {
      onClientStarted: (sinchClient) => {
        const { callClient } = sinchClient;
        callClient.addListener({
          onIncomingCall: (client, call) => {
            this.ui.onIncomingCall(call);
            this.#callListeners(call);
          },
        });

        this.ui.onClientStarted(sinchClient);
      },

      onCredentialsRequired: (sinchClient, clientRegistration) => {
        getJwtToken()
          .then(clientRegistration.register)
          .catch((error) => {
            clientRegistration.registerFailed();
            console.error(error);
          });
      },

      onClientFailed: (sinchClient, error) => {
        console.log("Sinch - Start client failed");
        console.error(error);
      },
    };
  }

  #callListeners(currentCall) {
    currentCall.addListener({
      onCallProgressing: (call) => {
        this.ui.onCallProgressing(call);
      },
      onCallRinging: (call) => {
        this.ui.onCallRinging(call);
      },
      onCallAnswered: (call) => {
        this.ui.onCallAnswered(call);
      },
      onCallEstablished: (call) => {
        this.ui.onCallEstablished(call);
      },
      onCallEnded: (call) => {
        this.ui.onCallEnded(call);
      },
      onCallQualityWarningEvent: (call, callQualityWarningEvent) => {
        this.ui.onCallQualityWarningEvent(call, callQualityWarningEvent);
      },
    });
  }
}
