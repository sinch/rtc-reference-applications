
this.addEventListener('push', event => {
    console.log('ServiceWorker Push: ', event);
    let body = event.data.json();
    event.waitUntil(
        clients.matchAll({ includeUncontrolled: true, type: 'window' }).then(clients => {
            clients.forEach((c) => {
                c.postMessage({
                    visible: c.visibilityState == 'visible',
                    data: body,
                })
            });
        })
    );
});
