import { showNotification } from '@vaadin/notification';

const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

export function connectNotifications(userId) {
    stompClient.connect({}, function () {
        stompClient.subscribe(`/topic/notifications/${userId}`, function (notification) {
            showNotification("🔔 " + notification.body, {
                position: 'top-end',
                duration: 5000
            });
        });
    });
}
