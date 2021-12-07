import {
  generateJwtToken,
  API_URL,
  APPLICATION_KEY,
  setText,
} from "../common/common.js";
import NumberCallUI from "./NumberCallUI.js";

export default class NumberCallSinchClientWrapper {
  constructor(userId) {
    this.ui = new NumberCallUI(this);
    this.userId = userId;
  }

  startSinchClient(callerIdentifier) {
    this.sinchClient = Sinch.getSinchClientBuilder()
      .applicationKey(APPLICATION_KEY)
      .userId(this.userId)
      .callerIdentifier(callerIdentifier)
      .environmentHost(API_URL)
      .build();

    this.sinchClient.addListener(this.#sinchClientListener());
    this.sinchClient.start();
  }

  #sinchClientListener() {
    return {
      onClientStarted: (sinchClient) => {
        const { callClient } = sinchClient;
        this.ui.getInput("to", "call", async (destination) => {
          setText("call", "Hangup");
          const call = await callClient.callPhoneNumber(destination);
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
        console.log("Sinch - Start client failed");
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
