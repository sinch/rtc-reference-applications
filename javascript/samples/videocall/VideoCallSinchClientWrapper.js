import {
  generateJwtToken,
  APPLICATION_KEY,
  API_URL,
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
  }

  startSinchClient(userId) {
    console.log("Sinch - Starting client");

    this.sinchClient = Sinch.getSinchClientBuilder()
      .applicationKey(APPLICATION_KEY)
      .userId(userId)
      .environmentHost(API_URL)
      .build();
    this.sinchClient.addListener(this.#sinchClientListener());
    this.sinchClient.setSupportManagedPush();
    this.sinchClient.start();
  }

  async makeCall(callee) {
    const call = await this.sinchClient.callClient.callUserVideo(callee);
    this.ui.onOutboundCall(call);
    this.#callListeners(call);
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
        generateJwtToken(sinchClient.localUserId)
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
      onCallEstablished: (call) => {
        this.ui.onCallEstablished(call);
      },
      onCallEnded: (call) => {
        this.ui.onCallEnded(call);
      },
    });
  }
}
