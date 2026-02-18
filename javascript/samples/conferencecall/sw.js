function broadcastMessageToClients(clients, body) {
  clients.forEach((client) => {
    client.postMessage({
      visible: client.visibilityState === "visible",
      data: body,
    });
  });
}

function showNotification(body) {
  const title = "New incoming call";

  const icon = "../common/style/favicon-180x180.png";
  const options = {
    body: title,
    icon,
    data: {
      body,
    },
  };
  return this.registration.showNotification(title, options);
}

function postMessageToClients(message) {
  return clients
    .matchAll({ type: "window", includeUncontrolled: true })
    .then((clients) => {
      broadcastMessageToClients(clients, message);
    });
}

function delay(ms) {
  return new Promise((resolve) => {
    setTimeout(resolve, ms);
  });
}

this.addEventListener("push", (event) => {
  console.log("ServiceWorker Push: ", event);
  const body = event.data.json();
  event.waitUntil(
    clients
      .matchAll({ includeUncontrolled: true, type: "window" })
      .then((clients) => {
        if (clients.length > 0) {
          broadcastMessageToClients(clients, body);
          let anyClientFocused = false;
          clients.forEach((client) => {
            if (client.visibilityState === "visible") {
              anyClientFocused = true;
            }
          });
          if (!anyClientFocused) {
            return showNotification(body);
          }
          return Promise.resolve();
        }
        return showNotification(body);
      }),
  );
});

/**
 * IMPORTANT: Please note that this event is not supported on Safari on iOS.
 * See: https://developer.mozilla.org/en-US/docs/Web/API/ServiceWorkerGlobalScope/notificationclick_event#browser_compatibility
 *
 * As a result, the reference app will not handle push notifications correctly when the application is terminated on iOS.
 * (It will work correctly only when the app is in the background or foreground).
 * The reason for this is that, when terminated, iOS always routes to the "start URL" on notification click event (the login screen).
 *
 * A possible workaround is to handle this logic within the login screen, but for the sake of simplicity, this is not implemented in the sample.
 */
this.addEventListener("notificationclick", (event) => {
  console.log("ServiceWorker Notification click: ", event);
  event.preventDefault();
  event.notification.close();

  // See: https://github.com/airbnb/javascript/issues/1632
  // eslint-disable-next-line no-restricted-globals
  const url = new URL("./", self.location).href;

  event.waitUntil(
    clients
      .matchAll({ type: "window", includeUncontrolled: true })
      .then((clientList) => {
        // If there are any clients open, just focus on it.
        // Note that we can't focus directly when the push is received as some of the clients don't allow to focus a window
        // without user interaction.
        const clientToFocus = clientList.find((client) => "focus" in client);
        if (clientToFocus) {
          return clientToFocus.focus();
        }

        // If there are not opened clients, open a new one and post message that containts incoming call payload.
        if (clients.openWindow) {
          return clients
            .openWindow(url)
            .then(() => delay(1000))
            .then(() => postMessageToClients(event.notification.data.body));
        }
        return Promise.resolve();
      }),
  );
});
