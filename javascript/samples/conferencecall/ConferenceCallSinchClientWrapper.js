import {
  canAutoStart,
  getJwtToken,
  getUserId,
  getApplicationKey,
  getEnvironmentHost,
} from "../common/common.js";
import ConferenceCallUI from "./ConferenceCallUI.js";

export default class ConferenceCallSinchClientWrapper {
  constructor() {
    this.ui = new ConferenceCallUI(this);
    const sinchClient = Sinch.getSinchClientBuilder()
      .applicationKey(getApplicationKey())
      .userId(getUserId())
      .environmentHost(getEnvironmentHost())
      .build();

    sinchClient.addListener(this.#sinchClientListener());
    sinchClient.setSupportManagedPush().then(() => {
      this.attemptAutoStart();
    });

    this.sinchClient = sinchClient;
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

  async makeCall(conferenceId) {
    const call = await this.sinchClient.callClient.callConference(conferenceId);
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
