package name.abuchen.portfolio.ui.views.dashboard;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;

import name.abuchen.portfolio.model.Dashboard;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.ui.AbstractFinanceView;

public class DashboardView extends AbstractFinanceView
{
    @Inject
    private ExchangeRateProviderFactory exchangeRateFactory;

    private List<WidgetDelegate> delegates = new ArrayList<>();

    @Override
    protected String getTitle()
    {
        return "Dashboard";
    }

    @Override
    public void notifyModelUpdated()
    {
        updateWidgets();
    }

    @Override
    protected Control createBody(Composite parent)
    {
        Dashboard dashboard = getClient().getDashboards().findAny().orElseGet(() -> createDefaultDashboard());

        DashboardResources resources = new DashboardResources(parent);

        Composite container = new Composite(parent, SWT.NONE);
        container.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
        GridLayoutFactory.fillDefaults().numColumns(dashboard.getColumns().size()).equalWidth(true).spacing(10, 10)
                        .applyTo(container);

        for (Dashboard.Column column : dashboard.getColumns())
        {
            Composite composite = createColumn(container);
            composite.setData(column);

            for (Dashboard.Widget widget : column.getWidgets())
            {
                WidgetFactory factory = WidgetFactory.valueOf(widget.getType());
                if (factory == null)
                    continue;

                WidgetDelegate delegate = factory.create(widget);

                this.delegates.add(delegate);

                Composite element = delegate.createControl(composite, resources);
                element.setData(widget);
                addDragListener(element);
                addDropListener(element);

                for (Control child : element.getChildren())
                    addDragListener(child);

                GridDataFactory.fillDefaults().grab(true, false).applyTo(element);
            }
        }

        updateWidgets();

        return container;
    }

    private Dashboard createDefaultDashboard()
    {
        Dashboard dashboard = new Dashboard();
        dashboard.setName("Letztes Jahr");

        Dashboard.Column column = new Dashboard.Column();
        dashboard.getColumns().add(column);

        Dashboard.Widget widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.HEADING.name());
        widget.setLabel("Kennzahlen");
        column.getWidgets().add(widget);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.TTWROR.name());
        widget.setLabel("True-Time Weighted Rate of Return");
        column.getWidgets().add(widget);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.IRR.name());
        widget.setLabel("Interner Zinsfuß");
        column.getWidgets().add(widget);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.ABSOLUTE_CHANGE.name());
        widget.setLabel("Absolute Change");
        column.getWidgets().add(widget);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.DELTA.name());
        widget.setLabel("Delta");
        column.getWidgets().add(widget);

        column = new Dashboard.Column();
        dashboard.getColumns().add(column);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.HEADING.name());
        widget.setLabel("Risikokennzahlen");
        column.getWidgets().add(widget);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.MAXDRAWDOWN.name());
        widget.setLabel("Maximaler Drawdown");
        column.getWidgets().add(widget);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.MAXDRAWDOWNDURATION.name());
        widget.setLabel("Maximaler Drawdown Duration");
        column.getWidgets().add(widget);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.VOLATILITY.name());
        widget.setLabel("Volatilität");
        column.getWidgets().add(widget);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.SEMIVOLATILITY.name());
        widget.setLabel("Semivolatilität");
        column.getWidgets().add(widget);

        column = new Dashboard.Column();
        dashboard.getColumns().add(column);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.HEADING.name());
        widget.setLabel("Berechnung");
        column.getWidgets().add(widget);

        widget = new Dashboard.Widget();
        widget.setType(WidgetFactory.CALCULATION.name());
        widget.setLabel("Berechnung");
        column.getWidgets().add(widget);

        return dashboard;
    }

    private Composite createColumn(Composite composite)
    {
        Composite column = new Composite(composite, SWT.NONE);
        GridLayoutFactory.fillDefaults().numColumns(1).spacing(0, 0).applyTo(column);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(column);

        addDropListener(column);

        return column;
    }

    private void addDragListener(final Control control)
    {
        final LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();

        final DragSourceAdapter dragAdapter = new DragSourceAdapter()
        {
            @Override
            public void dragSetData(final DragSourceEvent event)
            {
                Control c = control;
                while (!(c.getData() instanceof Dashboard.Widget))
                    c = c.getParent();

                transfer.setSelection(new StructuredSelection(c));
            }

            @Override
            public void dragStart(DragSourceEvent dragSourceEvent)
            {
                Control control = ((DragSource) dragSourceEvent.getSource()).getControl();

                while (!(control.getData() instanceof Dashboard.Widget))
                    control = control.getParent();

                Point compositeSize = control.getSize();
                GC gc = new GC(control);
                Image image = new Image(Display.getCurrent(), compositeSize.x, compositeSize.y);
                gc.copyArea(image, 0, 0);
                dragSourceEvent.image = image;
            }
        };

        final DragSource dragSource = new DragSource(control, DND.DROP_MOVE | DND.DROP_COPY);
        dragSource.setTransfer(new Transfer[] { transfer });
        dragSource.addDragListener(dragAdapter);
    }

    private void addDropListener(final Composite parent)
    {
        LocalSelectionTransfer transfer = LocalSelectionTransfer.getTransfer();

        DropTargetAdapter dragAdapter = new DropTargetAdapter()
        {
            @Override
            public void drop(final DropTargetEvent event)
            {
                Control droppedObj = (Control) ((StructuredSelection) transfer.getSelection()).getFirstElement();

                if (droppedObj.equals(parent))
                    return;

                Composite oldParent = droppedObj.getParent();

                Composite droppedComposite = (Composite) droppedObj;

                Composite p = parent;
                while (!(p.getData() instanceof Dashboard.Column))
                    p = p.getParent();

                droppedComposite.setParent(p);

                if (parent.getData() instanceof Dashboard.Widget)
                    droppedComposite.moveAbove(parent);

                if (parent.getData() instanceof Dashboard.Column)
                    droppedComposite.moveBelow(null);

                oldParent.layout();
                p.layout();
            }

            @Override
            public void dragEnter(DropTargetEvent event)
            {
                parent.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_GRAY));
            }

            @Override
            public void dragLeave(DropTargetEvent event)
            {
                parent.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
            }
        };

        DropTarget dropTarget = new DropTarget(parent, DND.DROP_MOVE | DND.DROP_COPY);
        dropTarget.setTransfer(new Transfer[] { transfer });
        dropTarget.addDropListener(dragAdapter);
    }

    private void updateWidgets()
    {
        CurrencyConverter converter = new CurrencyConverterImpl(exchangeRateFactory, getClient().getBaseCurrency());
        DashboardData data = new DashboardData(getClient(), converter);
        delegates.forEach(d -> d.update(data));
    }

}