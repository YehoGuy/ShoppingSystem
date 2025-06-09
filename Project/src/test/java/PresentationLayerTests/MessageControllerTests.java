package PresentationLayerTests;

import java.util.Arrays;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.app.ApplicationLayer.Message.MessageService;
import com.example.app.DomainLayer.Message;
import com.example.app.PresentationLayer.Controller.MessageController;

/**
 * Comprehensive slice tests for MessageController.
 */
@WebMvcTest(controllers = MessageController.class)
@ContextConfiguration(classes = MessageControllerTests.TestBootApp.class)
@AutoConfigureMockMvc(addFilters = false)
public class MessageControllerTests {

    @SpringBootApplication(scanBasePackages = "com.example.app.PresentationLayer")
    static class TestBootApp {
    }

    @Autowired
    private static MockMvc mvc;

    @MockBean
    private static MessageService messageService;

    @Nested
    @DisplayName("1. SEND TO USER")
    static class SendToUserTests {
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
    static class SendToShopTests {
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
    static class DeleteMessageTests {
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
    static class UpdateMessageTests {
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
    static class GetConversationTests {
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
    static class GetBySenderTests {
        @Test
        void success_returns200AndMessages() throws Exception {
        Message m1 = new Message(1, 3, 4, "Hello", "2025-05-07T13:45:30Z", true, 0);
        Message m2 = new Message(2, 3, 4, "Hello", "2025-05-07T13:45:30Z", true, 0);
        when(messageService.getMessagesBySenderId("tok", 3))
                .thenReturn(Arrays.asList(m1, m2));

        mvc.perform(get("/api/messages/sender/3")
                .param("authToken", "tok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].messageId",   is(1)))
                .andExpect(jsonPath("$[0].senderId",    is(3)))
                .andExpect(jsonPath("$[0].receiverId",  is(4)))
                .andExpect(jsonPath("$[0].content",     is("Hello")))
                .andExpect(jsonPath("$[1].messageId",   is(2)))
                .andExpect(jsonPath("$[1].senderId",    is(3)))
                .andExpect(jsonPath("$[1].receiverId",  is(4)))
                .andExpect(jsonPath("$[1].content",     is("Hello")));
        }
    }

    @Nested
    @DisplayName("7. GET BY RECEIVER")
    static class GetByReceiverTests {
        @Test
        void success_returns200AndMessages() throws Exception {
                Message mA = new Message(1, 3, 4, "Hello", "2025-05-07T13:45:30Z", true, 0);
                Message mB = new Message(2, 3, 4, "Hello", "2025-05-07T13:45:30Z", true, 0);
                when(messageService.getMessagesByReceiverId("tok"))
                        .thenReturn(Arrays.asList(mA, mB));

                 mvc.perform(get("/api/messages/receiver/4")
                .param("authToken", "tok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].messageId",   is(1)))
                .andExpect(jsonPath("$[0].senderId",    is(3)))
                .andExpect(jsonPath("$[0].receiverId",  is(4)))
                .andExpect(jsonPath("$[0].content",     is("Hello")))
                .andExpect(jsonPath("$[1].messageId",   is(2)))
                .andExpect(jsonPath("$[1].senderId",    is(3)))
                .andExpect(jsonPath("$[1].receiverId",  is(4)))
                .andExpect(jsonPath("$[1].content",     is("Hello")));
        }
    }

    @Nested
    @DisplayName("8. GET BY ID")
    static class GetByIdTests {
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
    static class GetPreviousTests {
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
