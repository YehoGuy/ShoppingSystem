import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.PWA;

/**
 * Use the @PWA annotation instead of @Theme for a simpler configuration
 */
@PWA(name = "My App", shortName = "App")
public class AppShell implements AppShellConfigurator {
    // Implementation is not needed
}