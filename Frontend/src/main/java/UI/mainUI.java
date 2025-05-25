package UI;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.server.PWA;

@SpringBootApplication
@Theme("mytheme")
@PWA(name = "My Shop", shortName = "Shop")
public class mainUI implements AppShellConfigurator {
    public static void main(String[] args) {
        SpringApplication.run(mainUI.class, args);
    }
}
