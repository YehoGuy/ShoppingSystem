import { showVaadinNotification } from "./notification-helper";

const socket = new SockJS('/ws');
const stompClient = Stomp.over(socket);

export function connectNotifications(userId) {
    stompClient.connect({}, function () {
        stompClient.subscribe(`/topic/notifications/${userId}`, function (notification) {
            showVaadinNotification("🔔 " + notification.body, {
                position: 'top-end',
                duration: 5000
            });
        });
    });
}
