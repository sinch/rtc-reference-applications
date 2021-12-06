import {
  generateJwtToken,
  APPLICATION_KEY,
  setText,
  API_URL,
} from "../common/common.js";
import SipCallUI from "./SipCallUI.js";

export default class SipCallSinchClientWrapper {
  constructor(userId) {
    this.sinchClient = Sinch.getSinchClientBuilder()
      .applicationKey(APPLICATION_KEY)
      .userId(userId)
      .environmentHost(API_URL)
      .build();

    this.sinchClient.addListener(this.#sinchClientListener());
    this.ui = new SipCallUI();
  }

  start() {
    this.sinchClient.start();
    this.ui.start();
  }

  #sinchClientListener() {
    return {
      onClientStarted: (sinchClient) => {
        const { callClient } = sinchClient;
        this.ui.getInput("to", "call", async (destination) => {
          setText("call", "Hangup");
          const call = await callClient.callSip(destination);
          this.#outboundCall(call);
        });
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
        console.error(error);
      },
    };
  }

  #outboundCall(currentCall) {
    this.ui.setCurrentCall(currentCall);
    currentCall.addListener({
      // eslint-disable-next-line no-unused-vars
      onCallProgressing: (call) => {
        this.ui.onCallProgressing();
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
