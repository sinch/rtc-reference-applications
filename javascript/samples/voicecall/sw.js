this.addEventListener("push", (event) => {
  console.log("ServiceWorker Push: ", event);
  const body = event.data.json();
  console.log(body);
  event.waitUntil(
    clients
      .matchAll({ includeUncontrolled: true, type: "window" })
      .then((clients) => {
        clients.forEach((client) => {
          client.postMessage({
            visible: client.visibilityState === "visible",
            data: body,
          });
        });
      })
  );
});
