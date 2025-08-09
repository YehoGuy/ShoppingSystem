package UI;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeEnterEvent;

public abstract class BaseView extends VerticalLayout implements BeforeEnterObserver {

    protected final Div headerContainer = new Div();
    protected final Span subtitle = new Span();
    protected final H1 title = new H1();

    /**
     * @param titleText     Main heading text
     * @param subtitleText  Smaller subtitle below
     * @param leftIcon      e.g. "🔒"
     * @param rightIcon     e.g. "➡️"
     */
    protected BaseView(String titleText, String subtitleText, String leftIcon, String rightIcon) {
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        addHeader(titleText, subtitleText, leftIcon, rightIcon);
    }

    private void addHeader(String titleText, String subtitleText, String leftIcon, String rightIcon) {
        headerContainer.addClassName("view-header");
        // icons + title row
        Div row = new Div();
        row.addClassName("header-title-row");
        row.add(new Span(leftIcon), title, new Span(rightIcon));

        subtitle.setText(subtitleText);
        subtitle.addClassName("header-subtitle");

        title.setText(titleText);
        title.addClassName("header-title");

        headerContainer.add(row, subtitle);
        add(headerContainer);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // by default nothing—concrete views can override if they need auth checks, etc.
    }
}
