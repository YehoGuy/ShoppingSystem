import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';

window.connectWebSocket = function (userId) {
    console.log("Connecting to WebSocket with userId: " + userId);
    const socket = new SockJS('http://localhost:8080/ws-notifications?userId=' + userId); // or whatever port your backend runs on
    const stompClient = Stomp.over(() => socket); // ✅ pass a factory function
    console.log("WebSocket created for userId: " + userId);

    stompClient.connect({}, function (frame) {
        console.log('Connected: ' + frame);
        console.log("Subscribing to: /user/" + userId + "/notifications");
        stompClient.subscribe("/user/" + userId + "/notifications", function (message) {
            showNotification(message.body);
            console.log("Notification received: " + message.body);
        });

        stompClient.send("/app/register", {}, userId);

    }, function (error) {
        console.error('STOMP error: ' + error);
    });
};

const showNotification = (message) => {
    const notification = document.createElement('vaadin-notification');
    notification.position = 'top-stretch';
    notification.duration = 10000;
    const content = document.createElement('div');
    content.style.padding = '1em';
    content.style.background = '#ff9800';
    content.style.color = 'white';
    content.style.textAlign = 'center';
    content.style.fontSize = 'var(--lumo-font-size-m)';
    content.textContent = message;
    notification.renderer = (root) => {
        root.innerHTML = '';
        root.appendChild(content);
    };
    document.body.appendChild(notification);
    notification.open();
    setTimeout(() => document.body.removeChild(notification), 5000);
};

