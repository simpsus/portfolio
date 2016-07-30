package name.abuchen.portfolio.ui.dialogs.transactions;

import static name.abuchen.portfolio.ui.util.FormDataFactory.startingWith;
import static name.abuchen.portfolio.ui.util.SWTHelper.amountWidth;
import static name.abuchen.portfolio.ui.util.SWTHelper.currencyWidth;
import static name.abuchen.portfolio.ui.util.SWTHelper.widest;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.SecurityPlan;
import name.abuchen.portfolio.money.Values;
import name.abuchen.portfolio.ui.Messages;
import name.abuchen.portfolio.ui.dialogs.transactions.AccountPlanModel.Properties;
import name.abuchen.portfolio.ui.util.DateTimePicker;
import name.abuchen.portfolio.ui.util.SimpleDateTimeSelectionProperty;

import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class AccountPlanDialog extends AbstractTransactionDialog
{
    private Client client;

    @Inject
    public AccountPlanDialog(@Named(IServiceConstants.ACTIVE_SHELL) Shell parentShell, Client client)
    {
        super(parentShell);
        this.client = client;
        setModel(new AccountPlanModel(client));
    }

    private AccountPlanModel model()
    {
        return (AccountPlanModel) this.model;
    }

    @Override
    protected void createFormElements(Composite editArea)
    {
        //
        // input elements
        //

        // name

        Label lblName = new Label(editArea, SWT.LEFT);
        lblName.setText(Messages.ColumnName);
        Text valueName = new Text(editArea, SWT.BORDER);
        IValidator validator = value -> {
            String v = (String) value;
            return v != null && v.trim().length() > 0 ? ValidationStatus.ok() : ValidationStatus.error(MessageFormat
                            .format(Messages.MsgDialogInputRequired, Messages.ColumnName));
        };
        context.bindValue(WidgetProperties.text(SWT.Modify).observe(valueName),
                        BeanProperties.value(Properties.name.name()).observe(model),
                        new UpdateValueStrategy().setAfterConvertValidator(validator), null);

        // account

        ComboInput account = new ComboInput(editArea, Messages.ColumnAccount);
        List<Account> accounts = including(client.getActiveAccounts(), model().getAccount());
        accounts.add(0, AccountPlanModel.DELIVERY);
        account.value.setInput(accounts);
        account.bindValue(Properties.account.name(), Messages.MsgMissingAccount);
        account.bindCurrency(Properties.accountCurrencyCode.name());

        // date

        Label lblDate = new Label(editArea, SWT.RIGHT);
        lblDate.setText(Messages.ColumnDate);
        DateTimePicker valueDate = new DateTimePicker(editArea);
        context.bindValue(new SimpleDateTimeSelectionProperty().observe(valueDate.getControl()),
                        BeanProperties.value(Properties.start.name()).observe(model));

        // interval

        List<Integer> available = new ArrayList<Integer>();
        for (int ii = 1; ii <= 12; ii++)
            available.add(ii);

        ComboInput interval = new ComboInput(editArea, Messages.ColumnInterval);
        interval.value.setInput(available);
        interval.value.setLabelProvider(new LabelProvider()
        {
            @Override
            public String getText(Object element)
            {
                int interval = (Integer) element;
                return MessageFormat.format(Messages.InvestmentPlanIntervalLabel, interval);
            }
        });
        interval.bindValue(Properties.interval.name(),
                        MessageFormat.format(Messages.MsgDialogInputRequired, Messages.ColumnInterval));

        // amount

        Input amount = new Input(editArea, Messages.ColumnAmount);
        amount.bindValue(Properties.amount.name(), Messages.ColumnAmount, Values.Amount, true);
        amount.bindCurrency(Properties.transactionCurrencyCode.name());

        // fees

        Input fees = new Input(editArea, Messages.ColumnFees);
        fees.bindValue(Properties.fees.name(), Messages.ColumnAmount, Values.Amount, false);
        fees.bindCurrency(Properties.transactionCurrencyCode.name());

        //
        // form layout
        //

        int amountWidth = amountWidth(amount.value);
        int currencyWidth = currencyWidth(amount.currency);

        startingWith(valueName, lblName)
                        .width(3 * amountWidth)
                        //
                        .thenBelow(account.value.getControl())
                        .label(account.label)
                        .suffix(account.currency, currencyWidth)
                        //
                        .thenBelow(valueDate.getControl())
                        .label(lblDate)
                        //
                        .thenBelow(interval.value.getControl())
                        .label(interval.label)
                        //
                        .thenBelow(amount.value).width(amountWidth).label(amount.label)
                        .suffix(amount.currency, currencyWidth)
                        //
                        .thenBelow(fees.value).width(amountWidth).label(fees.label)
                        .suffix(fees.currency, currencyWidth); //

        int widest = widest(lblName, account.label, lblDate, interval.label, amount.label, fees.label);
        startingWith(lblName).width(widest);

        WarningMessages warnings = new WarningMessages(this);
        warnings.add(() -> model().getStart().isAfter(LocalDate.now()) ? Messages.MsgDateIsInTheFuture : null);
        model.addPropertyChangeListener(Properties.start.name(), e -> warnings.check());
    }

    public void setPlan(SecurityPlan plan)
    {
        model().setSource(plan);
    }
}
