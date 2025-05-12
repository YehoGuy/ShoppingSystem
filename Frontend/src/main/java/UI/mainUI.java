package UI;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.vaadin.flow.component.page.AppShellConfigurator;

@SpringBootApplication
public class mainUI implements AppShellConfigurator {
    public static void main(String[] args) {
        SpringApplication.run(mainUI.class, args);
    }
}
