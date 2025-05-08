import {
  canAutoStart,
  getJwtToken,
  getUserId,
  getApplicationKey,
  API_URL,
} from "../common/common.js";
import VoiceCallUI from "./VoiceCallUI.js";

export default class VoiceCallSinchClientWrapper {
  constructor() {
    this.ui = new VoiceCallUI(this);
    const sinchClient = Sinch.getSinchClientBuilder()
      .applicationKey(getApplicationKey())
      .userId(getUserId())
      .environmentHost(API_URL)
      .build();

    sinchClient.addListener(this.#sinchClientListener());
    sinchClient.setSupportManagedPush().then(() => {
      this.attemptAutoStart();
    });

    this.sinchClient = sinchClient;
  }

  async makeCall(callee) {
    const call = await this.sinchClient.callClient.callUser(callee);
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
