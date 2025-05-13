package PresentationLayerTests;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import com.example.app.ApplicationLayer.Message.MessageService;
import com.example.app.PresentationLayer.Controller.MessageController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Comprehensive slice tests for MessageController.
 */
@WebMvcTest(controllers = MessageController.class)
@ContextConfiguration(classes = MessageControllerTests.TestBootApp.class)
@AutoConfigureMockMvc(addFilters = false)
public class MessageControllerTests {

    @SpringBootApplication(scanBasePackages = "PresentationLayer")
    static class TestBootApp {}

    @Autowired
    private MockMvc mvc;

    @MockBean
    private MessageService messageService;

    @Nested
    @DisplayName("1. SEND TO USER")
    class SendToUserTests {
        @Test
        void success_returns202() throws Exception {
            when(messageService.sendMessageToUser("tok", 5, "Hello", 0))
                .thenReturn("msg-123");

            mvc.perform(post("/api/messages/user")
                    .param("authToken", "tok")
                    .param("receiverId", "5")
                    .param("content", "Hello")
                    .param("previousMessageId", "0"))
               .andExpect(status().isAccepted())
               .andExpect(content().string("msg-123"));
        }

        @Test
        void badRequest_onErrorResult_returns400() throws Exception {
            when(messageService.sendMessageToUser(anyString(), anyInt(), anyString(), anyInt()))
                .thenReturn("Error: blocked");

            mvc.perform(post("/api/messages/user")
                    .param("authToken", "tok")
                    .param("receiverId", "5")
                    .param("content", "Hello")
                    .param("previousMessageId", "0"))
               .andExpect(status().isBadRequest())
               .andExpect(content().string("Error: blocked"));
        }
    }

    @Nested
    @DisplayName("2. SEND TO SHOP")
    class SendToShopTests {
        @Test
        void success_returns202() throws Exception {
            when(messageService.sendMessageToShop("tok", 10, "Order?", 2))
                .thenReturn("shop-msg-789");

            mvc.perform(post("/api/messages/shop")
                    .param("authToken", "tok")
                    .param("receiverId", "10")
                    .param("content", "Order?")
                    .param("previousMessageId", "2"))
               .andExpect(status().isAccepted())
               .andExpect(content().string("shop-msg-789"));
        }

        @Test
        void badRequest_onErrorResult_returns400() throws Exception {
            when(messageService.sendMessageToShop(anyString(), anyInt(), anyString(), anyInt()))
                .thenReturn("Error: shop closed");

            mvc.perform(post("/api/messages/shop")
                    .param("authToken", "tok")
                    .param("receiverId", "10")
                    .param("content", "Order?")
                    .param("previousMessageId", "2"))
               .andExpect(status().isBadRequest())
               .andExpect(content().string("Error: shop closed"));
        }
    }

    @Nested
    @DisplayName("3. DELETE MESSAGE")
    class DeleteMessageTests {
        @Test
        void success_returns200() throws Exception {
            when(messageService.deleteMessage("tok", 42)).thenReturn("deleted");

            mvc.perform(delete("/api/messages/42")
                    .param("authToken", "tok"))
               .andExpect(status().isOk())
               .andExpect(content().string("deleted"));
        }

        @Test
        void badRequest_onErrorResult_returns400() throws Exception {
            when(messageService.deleteMessage(anyString(), anyInt()))
                .thenReturn("Error: no rights");

            mvc.perform(delete("/api/messages/42")
                    .param("authToken", "tok"))
               .andExpect(status().isBadRequest())
               .andExpect(content().string("Error: no rights"));
        }
    }

    @Nested
    @DisplayName("4. UPDATE MESSAGE")
    class UpdateMessageTests {
        @Test
        void success_returns200() throws Exception {
            when(messageService.updateMessage("tok", 7, "New content"))
                .thenReturn("updated");

            mvc.perform(patch("/api/messages/7")
                    .param("authToken", "tok")
                    .param("content", "New content"))
               .andExpect(status().isOk())
               .andExpect(content().string("updated"));
        }

        @Test
        void badRequest_onErrorResult_returns400() throws Exception {
            when(messageService.updateMessage(anyString(), anyInt(), anyString()))
                .thenReturn("Error: too late");

            mvc.perform(patch("/api/messages/7")
                    .param("authToken", "tok")
                    .param("content", "New content"))
               .andExpect(status().isBadRequest())
               .andExpect(content().string("Error: too late"));
        }
    }

    @Nested
    @DisplayName("5. GET CONVERSATION")
    class GetConversationTests {
        @Test
        void success_returns200AndConversation() throws Exception {
            when(messageService.getFullConversation("tok", 15))
                .thenReturn("msg1 -> msg2 -> msg3");

            mvc.perform(get("/api/messages/15/conversation")
                    .param("authToken", "tok"))
               .andExpect(status().isOk())
               .andExpect(content().string("msg1 -> msg2 -> msg3"));
        }
    }

    @Nested
    @DisplayName("6. GET BY SENDER")
    class GetBySenderTests {
        @Test
        void success_returns200AndMessages() throws Exception {
            when(messageService.getMessagesBySenderId("tok", 3))
                .thenReturn("m1,m2");

            mvc.perform(get("/api/messages/sender/3")
                    .param("authToken", "tok"))
               .andExpect(status().isOk())
               .andExpect(content().string("m1,m2"));
        }
    }

    @Nested
    @DisplayName("7. GET BY RECEIVER")
    class GetByReceiverTests {
        @Test
        void success_returns200AndMessages() throws Exception {
            when(messageService.getMessagesByReceiverId("tok", 4))
                .thenReturn("mA,mB");

            mvc.perform(get("/api/messages/receiver/4")
                    .param("authToken", "tok"))
               .andExpect(status().isOk())
               .andExpect(content().string("mA,mB"));
        }
    }

    @Nested
    @DisplayName("8. GET BY ID")
    class GetByIdTests {
        @Test
        void success_returns200() throws Exception {
            when(messageService.getMessageById("tok", 9))
                .thenReturn("single message");

            mvc.perform(get("/api/messages/9")
                    .param("authToken", "tok"))
               .andExpect(status().isOk())
               .andExpect(content().string("single message"));
        }

        @Test
        void notFound_returns404() throws Exception {
            when(messageService.getMessageById(anyString(), anyInt()))
                .thenReturn("Message not found!");

            mvc.perform(get("/api/messages/9")
                    .param("authToken", "tok"))
               .andExpect(status().isNotFound())
               .andExpect(content().string("Message not found!"));
        }
    }

    @Nested
    @DisplayName("9. GET PREVIOUS MESSAGE")
    class GetPreviousTests {
        @Test
        void success_returns200() throws Exception {
            when(messageService.getPreviousMessage("tok", 9))
                .thenReturn("prev message");

            mvc.perform(get("/api/messages/9/previous")
                    .param("authToken", "tok"))
               .andExpect(status().isOk())
               .andExpect(content().string("prev message"));
        }

        @Test
        void notFound_returns404() throws Exception {
            when(messageService.getPreviousMessage(anyString(), anyInt()))
                .thenReturn("No previous message found!");

            mvc.perform(get("/api/messages/9/previous")
                    .param("authToken", "tok"))
               .andExpect(status().isNotFound())
               .andExpect(content().string("No previous message found!"));
        }
    }
}
