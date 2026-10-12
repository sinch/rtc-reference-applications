import {
  getJwtToken,
  getUserId,
  getApplicationKey,
  setText,
  getEnvironmentHost,
} from "../common/common.js";
import SipCallUI from "./SipCallUI.js";

export default class SipCallSinchClientWrapper {
  constructor() {
    this.sinchClient = Sinch.getSinchClientBuilder()
      .applicationKey(getApplicationKey())
      .userId(getUserId())
      .environmentHost(getEnvironmentHost())
      .build();

    this.sinchClient.addListener(this.#sinchClientListener());
    this.ui = new SipCallUI(this);
  }

  start() {
    this.sinchClient.start();
    this.ui.start();
  }

  #sinchClientListener() {
    return {
      onClientStarted: () => {
        this.ui.getInput("to", "call", async (destination) => {
          setText("call", "Hangup");
          const call = await this.sinchClient.callClient.callSip(destination);
          this.#outboundCall(call);
        });
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
        console.error(error);
      },
    };
  }

  #outboundCall(currentCall) {
    this.ui.setCurrentCall(currentCall);
    currentCall.addListener({
      // eslint-disable-next-line no-unused-vars
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
