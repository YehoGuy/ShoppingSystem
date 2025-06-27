import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';

window.stompClient = null;

window.connectWebSocket = function (userId) {
    console.log("Connecting to WebSocket with userId: " + userId);
    const socket = new SockJS('http://localhost:8080/ws-notifications?userId=' + userId); // or whatever port your backend runs on
    stompClient = Stomp.over(() => socket); // ✅ pass a factory function
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
  notification.duration = 5000; // Automatically closes after 5s
  notification.theme = 'contrast'; // Uses built-in theme styling

  notification.renderer = (root) => {
    // Clear any previous content
    root.innerHTML = '';

    const content = document.createElement('div');
    content.style.padding = '1rem';
    content.style.backgroundColor = '#1976d2'; // Material blue 700
    content.style.color = 'white';
    content.style.borderRadius = '8px';
    content.style.boxShadow = '0 4px 12px rgba(0, 0, 0, 0.2)';
    content.style.textAlign = 'center';
    content.style.fontSize = 'var(--lumo-font-size-m)';
    content.style.fontWeight = '500';
    content.textContent = message;

    root.appendChild(content);
  };

  document.body.appendChild(notification);
  notification.open();

  // Remove from DOM after it closes to keep things clean
  setTimeout(() => {
    if (document.body.contains(notification)) {
      document.body.removeChild(notification);
    }
  }, notification.duration + 1000); // Wait a bit longer to be sure it's closed
};

window.disconnectWebSocket = function () {
    if (window.stompClient && window.stompClient.connected) {
        console.log("Disconnecting WebSocket");
        window.stompClient.disconnect(() => {
            console.log("WebSocket disconnected");
            window.stompClient = null;
        });
    }
};
