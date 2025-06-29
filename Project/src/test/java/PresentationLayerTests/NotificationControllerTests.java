package PresentationLayerTests;

import com.example.app.ApplicationLayer.NotificationService;
import com.example.app.PresentationLayer.Controller.NotificationController;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class NotificationControllerTests {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController controller;

    @Test
    void sendNotification_delegatesToService() {
        // given
        int userId = 42;
        String title = "Greetings";
        String message = "Hello there";

        // when
        controller.sendNotification(userId, title, message);

        // then
        verify(notificationService).sendToUser(userId, title, message);
    }
}
