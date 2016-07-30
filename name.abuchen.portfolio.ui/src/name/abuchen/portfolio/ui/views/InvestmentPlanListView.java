package name.abuchen.portfolio.ui.views;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.SecurityPlan;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.money.CurrencyConverterImpl;
import name.abuchen.portfolio.money.ExchangeRateProviderFactory;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Images;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.dialogs.transactions.AccountPlanDialog;
import name.abuchen.portfolio.ui.dialogs.transactions.OpenDialogAction;
import name.abuchen.portfolio.ui.dialogs.transactions.SecurityPlanDialog;
import name.abuchen.portfolio.ui.dialogs.transactions.AccountPlanModel;
import name.abuchen.portfolio.ui.util.AbstractDropDown;
import name.abuchen.portfolio.ui.util.viewers.Column;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.ColumnEditingSupport.ModificationListener;
import name.abuchen.portfolio.ui.util.viewers.ColumnViewerSorter;
import name.abuchen.portfolio.ui.util.viewers.DateEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.ListEditingSupport;
import name.abuchen.portfolio.ui.util.viewers.ShowHideColumnHelper;
import name.abuchen.portfolio.ui.util.viewers.ValueEditingSupport;
import name.abuchen.portfolio.ui.views.columns.NameColumn;
import name.abuchen.portfolio.ui.views.columns.NoteColumn;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;

public class InvestmentPlanListView extends AbstractListView implements ModificationListener
{
    private TableViewer plans;
    private PortfolioTransactionsViewer transactions;
    private ShowHideColumnHelper planColumns;

    @Inject
    private ExchangeRateProviderFactory factory;

    @Override
    protected String getDefaultTitle()
    {
        return Messages.LabelInvestmentPlans;
    }

    @Override
    public void notifyModelUpdated()
    {
        plans.setSelection(plans.getSelection());
    }

    @Override
    public void onModified(Object element, Object newValue, Object oldValue)
    {
        SecurityPlan plan = (SecurityPlan) element;
        if (plan.getAccount() != null && plan.getAccount().equals(AccountPlanModel.DELIVERY))
            plan.setAccount(null);

        markDirty();
    }

    @Override
    protected void addButtons(ToolBar toolBar)
    {
        addNewInvestmentPlanButton(toolBar);
        addConfigButton(toolBar);
    }

    private void addNewInvestmentPlanButton(ToolBar toolBar)
    {
        Action securityPlanAction = new OpenDialogAction(this, "Wertpapier Plan").type(SecurityPlanDialog.class)
                        .onSuccess(d -> {
                            markDirty();
                            plans.setInput(getClient().getPlans());
                        });
        Action accountPlanAction = new OpenDialogAction(this, "Konto Plan").type(AccountPlanDialog.class).onSuccess(
                        d -> {
                            markDirty();
                            plans.setInput(getClient().getPlans());
                        });
        new AbstractDropDown(toolBar, "InvestmentPlan", Images.INVESTMENTPLAN.image(), SWT.NONE)
        {

            @Override
            public void menuAboutToShow(IMenuManager manager)
            {

                manager.add(securityPlanAction);
                manager.add(accountPlanAction);
            }

        };
    }

    private void addConfigButton(final ToolBar toolBar)
    {
        new AbstractDropDown(toolBar, Messages.MenuShowHideColumns, Images.CONFIG.image(), SWT.NONE)
        {
            @Override
            public void menuAboutToShow(IMenuManager manager)
            {
                MenuManager m = new MenuManager(Messages.LabelInvestmentPlans);
                planColumns.menuAboutToShow(m);
                manager.add(m);

                m = new MenuManager(Messages.LabelTransactions);
                transactions.getColumnSupport().menuAboutToShow(m);
                manager.add(m);
            }
        };
    }

    @Override
    protected void createTopTable(Composite parent)
    {
        Composite container = new Composite(parent, SWT.NONE);
        TableColumnLayout layout = new TableColumnLayout();
        container.setLayout(layout);

        plans = new TableViewer(container, SWT.FULL_SELECTION);

        ColumnEditingSupport.prepare(plans);

        planColumns = new ShowHideColumnHelper(InvestmentPlanListView.class.getSimpleName() + "@top", //$NON-NLS-1$
                        getPreferenceStore(), plans, layout);

        addColumns(planColumns);

        planColumns.createColumns();
        plans.getTable().setHeaderVisible(true);
        plans.getTable().setLinesVisible(true);
        plans.setContentProvider(ArrayContentProvider.getInstance());
        plans.setInput(getClient().getPlans());

        plans.addSelectionChangedListener(new ISelectionChangedListener()
        {
            public void selectionChanged(SelectionChangedEvent event)
            {
                SecurityPlan plan = (SecurityPlan) ((IStructuredSelection) event.getSelection()).getFirstElement();

                if (plan != null)
                    transactions.setInput(plan.getPortfolio(), plan.getTransactions());
                else
                    transactions.setInput(null, null);

                transactions.refresh();
            }
        });

        hookContextMenu(plans.getTable(), new IMenuListener()
        {
            public void menuAboutToShow(IMenuManager manager)
            {
                fillPlansContextMenu(manager);
            }
        });
    }

    private void addColumns(ShowHideColumnHelper support)
    {
        Column column = new NameColumn("0", Messages.ColumnName, SWT.None, 100); //$NON-NLS-1$
        column.getEditingSupport().addListener(this);
        support.addColumn(column);

        column = new Column(Messages.ColumnSecurity, SWT.NONE, 250);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return ((SecurityPlan) e).getSecurity().getName();
            }

            @Override
            public Image getImage(Object element)
            {
                return Images.SECURITY.image();
            }
        });
        ColumnViewerSorter.create(Security.class, "name").attachTo(column); //$NON-NLS-1$
        List<Security> securities = new ArrayList<Security>(getClient().getSecurities());
        Collections.sort(securities, new Security.ByName());
        new ListEditingSupport(SecurityPlan.class, "security", securities).addListener(this).attachTo(column); //$NON-NLS-1$
        support.addColumn(column);

        column = new Column(Messages.ColumnPortfolio, SWT.None, 120);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return ((SecurityPlan) e).getPortfolio().getName();
            }

            @Override
            public Image getImage(Object element)
            {
                return Images.PORTFOLIO.image();
            }
        });
        ColumnViewerSorter.create(SecurityPlan.class, "portfolio").attachTo(column); //$NON-NLS-1$
        new ListEditingSupport(SecurityPlan.class, "portfolio", getClient().getActivePortfolios()).addListener(this) //$NON-NLS-1$
                        .attachTo(column);
        support.addColumn(column);

        column = new Column(Messages.ColumnAccount, SWT.None, 120);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                SecurityPlan plan = (SecurityPlan) e;
                return plan.getAccount() != null ? plan.getAccount().getName() : Messages.InvestmentPlanOptionDelivery;
            }

            @Override
            public Image getImage(Object e)
            {
                SecurityPlan plan = (SecurityPlan) e;
                return plan.getAccount() != null ? Images.ACCOUNT.image() : null;
            }
        });
        ColumnViewerSorter.create(Account.class, "name").attachTo(column); //$NON-NLS-1$
        List<Account> accounts = new ArrayList<Account>();
        accounts.add(AccountPlanModel.DELIVERY);
        accounts.addAll(getClient().getAccounts());
        new ListEditingSupport(SecurityPlan.class, "account", accounts).addListener(this).attachTo(column); //$NON-NLS-1$
        support.addColumn(column);

        column = new Column(Messages.ColumnStartDate, SWT.None, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return Values.Date.format(((SecurityPlan) e).getStart());
            }
        });
        ColumnViewerSorter.create(SecurityPlan.class, "start").attachTo(column); //$NON-NLS-1$
        new DateEditingSupport(SecurityPlan.class, "start").addListener(this).attachTo(column); //$NON-NLS-1$
        support.addColumn(column);

        column = new Column(Messages.ColumnInterval, SWT.None, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                return MessageFormat.format(Messages.InvestmentPlanIntervalLabel, ((SecurityPlan) e).getInterval());
            }
        });
        ColumnViewerSorter.create(SecurityPlan.class, "interval").attachTo(column); //$NON-NLS-1$
        List<Integer> available = new ArrayList<Integer>();
        for (int ii = 1; ii <= 12; ii++)
            available.add(ii);
        new ListEditingSupport(SecurityPlan.class, "interval", available).addListener(this).attachTo(column); //$NON-NLS-1$
        support.addColumn(column);

        column = new Column(Messages.ColumnAmount, SWT.RIGHT, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                SecurityPlan plan = (SecurityPlan) e;
                return Values.Money.format(Money.of(plan.getCurrencyCode(), plan.getAmount()));
            }
        });
        ColumnViewerSorter.create(SecurityPlan.class, "amount").attachTo(column); //$NON-NLS-1$
        new ValueEditingSupport(SecurityPlan.class, "amount", Values.Amount).addListener(this).attachTo(column); //$NON-NLS-1$
        support.addColumn(column);

        column = new Column(Messages.ColumnFees, SWT.RIGHT, 80);
        column.setLabelProvider(new ColumnLabelProvider()
        {
            @Override
            public String getText(Object e)
            {
                SecurityPlan plan = (SecurityPlan) e;
                return Values.Money.format(Money.of(plan.getCurrencyCode(), plan.getFees()));
            }
        });
        ColumnViewerSorter.create(SecurityPlan.class, "fees").attachTo(column); //$NON-NLS-1$
        new ValueEditingSupport(SecurityPlan.class, "fees", Values.Amount).addListener(this).attachTo(column); //$NON-NLS-1$
        support.addColumn(column);

        column = new NoteColumn();
        column.getEditingSupport().addListener(this);
        column.setVisible(false);
        support.addColumn(column);
    }

    private void fillPlansContextMenu(IMenuManager manager)
    {
        final SecurityPlan plan = (SecurityPlan) ((IStructuredSelection) plans.getSelection()).getFirstElement();
        if (plan == null)
            return;

        manager.add(new Action(Messages.InvestmentPlanMenuGenerateTransactions)
        {
            @Override
            public void run()
            {
                CurrencyConverterImpl converter = new CurrencyConverterImpl(factory, getClient().getBaseCurrency());
                List<PortfolioTransaction> latest = plan.generateTransactions(converter);
                markDirty();

                plans.refresh();
                transactions.markTransactions(latest);
                transactions.setInput(plan.getPortfolio(), plan.getTransactions());
            }
        });

        manager.add(new Separator());

        new OpenDialogAction(this, Messages.MenuEditInvestmentPlan) //
                        .type(SecurityPlanDialog.class, d -> d.setPlan(plan)) //
                        .onSuccess(d -> {
                            markDirty();
                            plans.setInput(getClient().getPlans());
                        }).addTo(manager);

        manager.add(new Action(Messages.InvestmentPlanMenuDelete)
        {
            @Override
            public void run()
            {
                getClient().removePlan(plan);
                markDirty();

                plans.setInput(getClient().getPlans());
                transactions.setInput(null, null);
            }
        });
    }

    @Override
    protected void createBottomTable(Composite parent)
    {
        transactions = new PortfolioTransactionsViewer(parent, this);
        transactions.setFullContextMenu(false);

        if (!getClient().getPlans().isEmpty())
            plans.setSelection(new StructuredSelection(plans.getElementAt(0)), true);
    }
}
