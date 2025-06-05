import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';

let stompClient = null;

export function connectWebSocket(userId, onMessageReceived) {
    const socket = new SockJS('/ws-notifications');
    stompClient = Stomp.over(socket);

    stompClient.connect({}, function (frame) {
        console.log('Connected: ' + frame);

        // Register userId with backend
        stompClient.send("/app/register", {}, userId);

        // Listen to personal notifications
        stompClient.subscribe("/user/notifications", function (message) {
            onMessageReceived(message.body);
        });
    });
}

