package ApplicationLayerTests;

import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.example.app.ApplicationLayer.NotificationService;
import com.example.app.ApplicationLayer.User.UserService;
import com.example.app.DomainLayer.Member;
import com.example.app.WebSocket.SessionUserRegistry;

public class NotificationServiceTests {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private SessionUserRegistry registry;

    @Mock
    private UserService userService;

    @Mock
    private Member member;

    private NotificationService notificationService;

    private final Integer userId = 42;
    private final String title = "Test Title";
    private final String message = "Test Message";

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        notificationService = new NotificationService(messagingTemplate);
        // Inject registry via reflection
        Field registryField = NotificationService.class.getDeclaredField("registry");
        registryField.setAccessible(true);
        registryField.set(notificationService, registry);
        // Inject userService via setter
        notificationService.setService(userService);
    }

    @Test
    void sendToUser_whenUserNotConnected_shouldAddNotification() {
        when(registry.getSessionsForUser(userId)).thenReturn(List.of("session1"));
        when(userService.getUserById(userId)).thenReturn(member);
        when(member.isConnected()).thenReturn(false);

        notificationService.sendToUser(userId, title, message);

        verify(userService).addNotification(userId, title, message);
        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any(), Mockito.<java.util.Map<String, Object>>any());
    }

    @Test
    void sendToUser_whenUserConnected_shouldSendMessageToAllSessions() {
        List<String> sessions = List.of("sessA", "sessB");
        when(registry.getSessionsForUser(userId)).thenReturn(sessions);
        when(userService.getUserById(userId)).thenReturn(member);
        when(member.isConnected()).thenReturn(true);

        notificationService.sendToUser(userId, title, message);

        String payload = title + "\n" + message;
        verify(messagingTemplate, times(sessions.size()))
            .convertAndSendToUser(eq(userId.toString()), eq("/notifications"), eq(payload), Mockito.<java.util.Map<String, Object>>any());
    }
}
