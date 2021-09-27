import JWT from '../jwt.js';

const APPLICATION_KEY = 'APPLICATION_KEY';
const APPLICATION_SECRET = 'APPLICATION_SECRET';
const FROM_USER_ID = 'USER_ID';
const API_URL = 'https://ocra.api.sinch.com';

class SinchPhone {
    constructor(userId) {
        this.sinch = Sinch.getSinchClientBuilder().
            applicationKey(APPLICATION_KEY).
            userId(userId).
            environmentHost(API_URL).build();

        this.sinch.addListener(this);
        this.currentCall = null;
        this.audio = document.createElement('audio');
    }

    start() {
        this.sinch.start();
    }

    onClientStarted(sinch) {
        const callClient = sinch.callClient;
        this.getInput('to', 'call', async (destination) => {
            this.setText('call', 'Hangup');
            this.onOutboundCall(await callClient.callSip(destination));
        });
    }

    playAudio(call) {
        this.audio.autoplay = true;
        this.audio.srcObject = call.incomingStream;
    }

    pauseAudio() {
        this.audio.pause();
    }

    onOutboundCall(call) {
        this.currentCall = call;
        call.addListener({
            onCallProgressing: (call) => {
                this.setStatus('Call Progressing...');
                this.setColor('status','orange');
            },
            onCallEstablished: (call) => {
                this.playAudio(call);
                this.setStatus('Call Established');
                this.setColor('status', 'white');
            },
            onCallEnded: (call) => {
                this.setStatus('Call Disconnected');
                this.setColor('status', 'red');
                this.setText('call', 'Call');
                this.currentCall = undefined;
                this.pauseAudio();
            }
        });
    }

    onClientFailed(error) {
        console.log(error);
    }

    setStatus(text) {
        this.setText('status', text);
        console.log('Status: ' + text);
    }

    setText(id, text) {
        document.getElementById(id).innerHTML = text;
    }

    setColor(id, text) {
        document.getElementById(id).style.color = text;
    }
    onCredentialsRequired(sinch, clientRegistration) {
        const token = new JWT(APPLICATION_KEY, APPLICATION_SECRET, sinch.userId);
        token.toJwt().then(clientRegistration.register).catch((error) => console.log(error));
    }

    getInput(field, button, resolve) {
        document.getElementById(button).addEventListener('click', (e) => {
            e.preventDefault();
            if (this.currentCall)
                this.currentCall.hangup();
            else
                resolve(document.getElementById(field).value);
        });
    }
}

const sinchPhone = new SinchPhone(FROM_USER_ID);
sinchPhone.start();
