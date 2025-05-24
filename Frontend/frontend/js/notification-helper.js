import '@vaadin/notification';

export function showVaadinNotification(message) {
    const notification = document.createElement('vaadin-notification');
    notification.renderer = function (root) {
        root.textContent = message;
    };
    notification.duration = 5000;
    notification.position = 'top-end';
    document.body.appendChild(notification);
    notification.opened = true;

    notification.addEventListener('opened-changed', (e) => {
        if (!e.detail.value) {
            notification.remove();
        }
    });
}
