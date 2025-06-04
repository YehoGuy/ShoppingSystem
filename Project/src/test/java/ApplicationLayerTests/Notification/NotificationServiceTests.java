package ApplicationLayerTests.Notification;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.example.app.ApplicationLayer.NotificationService;
import com.example.app.ApplicationLayer.User.UserService;
import com.example.app.DomainLayer.Member;
import com.example.app.DomainLayer.User;

public class NotificationServiceTests {
    private SimpMessagingTemplate messagingTemplate;
    private UserService userService;
    private NotificationService notificationService;

    @BeforeEach
    public void setUp() {
        messagingTemplate = mock(SimpMessagingTemplate.class);// Mock or real implementation
        userService = mock(UserService.class); // Mock or real implementation
        notificationService = new NotificationService(messagingTemplate);
        notificationService.setService(userService);
    }

    @Test
    public void testSendToUser_userIsConnected() {
        int userId = 1;
        String title = "Test Title";
        String message = "Test Message";

        // Create a connected member mock
        Member member = mock(Member.class);
        when(userService.getUserById(userId)).thenReturn(member);
        when(member.isConnected()).thenReturn(true);

        notificationService.sendToUser(userId, title, message);

        // Verify that the message was sent through messagingTemplate (once per your
        // current logic)
        verify(messagingTemplate, times(1)).convertAndSend("/topic/notifications/" + userId, message);
        // Because user is connected, userService.addNotification should NOT be called
        verify(userService, never()).addNotification(anyInt(), anyString(), anyString());
    }

    @Test
    void testSendToUser_WhenUserIsNotConnected() {
        int userId = 2;
        String title = "Test Title";
        String message = "Test Message";

        Member member = mock(Member.class);
        when(userService.getUserById(userId)).thenReturn(member);
        when(member.isConnected()).thenReturn(false);

        notificationService.sendToUser(userId, title, message);

        // Verify message was sent anyway (based on your code's last line)
        verify(messagingTemplate, never()).convertAndSend("/topic/notifications/" + userId, message);
        // Verify addNotification was called because member is not connected
        verify(userService, times(1)).addNotification(userId, title, message);
    }

    @Test
    void testSendToUser_WhenUserIsNotMember() {
        int userId = 3;
        String title = "Title";
        String message = "Message";

        User user = mock(User.class);
        when(userService.getUserById(userId)).thenReturn(user);

        notificationService.sendToUser(userId, title, message);

        // Verify messagingTemplate sends the message anyway (last line)
        verify(messagingTemplate, never()).convertAndSend("/topic/notifications/" + userId, message);
        // addNotification should not be called (user is not Member)
        verify(userService, never()).addNotification(anyInt(), anyString(), anyString());
    }

    @Test
    void testSendToUser_WhenExceptionIsThrown() {
        int userId = 4;
        String title = "Title";
        String message = "Message";

        when(userService.getUserById(userId)).thenThrow(new RuntimeException("DB error"));

        try {
            notificationService.sendToUser(userId, title, message);
        } catch (RuntimeException e) {
            // Expected exception
            assert (e.getMessage().contains("Error sending notification"));
        }

        // messagingTemplate should never be called because getUserById failed
        verify(messagingTemplate, never()).convertAndSend(anyString(), anyString());
    }
}
