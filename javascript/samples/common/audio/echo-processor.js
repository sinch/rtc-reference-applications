/* global sampleRate */
/**
 * AudioWorklet processor that applies echo effect to the local microphone input.
 *
 * This is the browser analog of modifying mic PCM in the Android/iOS local audio frame
 * listener: `process()` runs on the audio render thread, reads the input frames, and writes
 * the (echoed) result to the output. Writing zeros to `outCh` would substitute silence.
 *
 * Registered by the SDK when you call
 * `call.setLocalAudioFrameListener({ moduleUrl, processorName: "echo-processor", processorOptions })`.
 */
class EchoProcessor extends AudioWorkletProcessor {
  constructor(options) {
    super();
    const { delaySeconds = 0.3, feedback = 0.4 } =
      (options && options.processorOptions) || {};
    this.feedback = feedback;
    this.buffer = new Float32Array(
      Math.max(1, Math.floor(sampleRate * delaySeconds)),
    );
    this.pos = 0;
  }

  process(inputs, outputs) {
    const input = inputs[0];
    const output = outputs[0];
    if (!input || input.length === 0) {
      return true;
    }
    const inCh = input[0];
    const outCh = output[0];
    for (let i = 0; i < outCh.length; i += 1) {
      const delayed = this.buffer[this.pos];
      const sample = (inCh ? inCh[i] : 0) + delayed * this.feedback;
      outCh[i] = sample;
      this.buffer[this.pos] = sample;
      this.pos = (this.pos + 1) % this.buffer.length;
    }
    for (let ch = 1; ch < output.length; ch += 1) {
      output[ch].set(outCh);
    }
    return true;
  }
}

registerProcessor("echo-processor", EchoProcessor);
