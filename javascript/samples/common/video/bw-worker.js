/* global VideoFrame */
/**
 * Worker that turns the video frames it is handed black and white.
 *
 * Used two ways in this sample:
 *  - Local B&W: the SDK starts this worker from
 *    `call.setLocalVideoFrameListener({ workerUrl })` and hands it the outgoing camera, so the
 *    remote peer sees black and white.
 *  - Remote B&W: the sample starts it itself and hands it the received video track, so only this
 *    browser sees the remote peer in black and white.
 *
 * Protocol: post `{ type: "ready" }`, then pipe every `{ type: "transform", readable, writable }`.
 * A pipe ends on its own when its readable closes, which is how a replaced transform retires.
 */

function blackAndWhiteTransform() {
  let canvas;
  let context;
  return new TransformStream({
    transform(frame, controller) {
      let converted;
      try {
        if (
          !canvas ||
          canvas.width !== frame.displayWidth ||
          canvas.height !== frame.displayHeight
        ) {
          canvas = new OffscreenCanvas(frame.displayWidth, frame.displayHeight);
          context = canvas.getContext("2d");
          context.filter = "grayscale(1)";
        }
        context.drawImage(frame, 0, 0);
        converted = new VideoFrame(canvas, { timestamp: frame.timestamp });
        controller.enqueue(converted);
      } catch (error) {
        converted?.close();
        throw error;
      } finally {
        frame.close();
      }
    },
  });
}

this.addEventListener("message", (event) => {
  const { type, readable, writable } = event.data ?? {};
  if (type !== "transform") return;
  readable
    .pipeThrough(blackAndWhiteTransform())
    .pipeTo(writable)
    .catch((error) => console.error("B&W transform stopped:", error));
});

this.postMessage({ type: "ready" });
