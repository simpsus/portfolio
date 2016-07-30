package name.abuchen.portfolio.ui.dialogs.transactions;

import java.time.LocalDate;

import name.abuchen.portfolio.model.Account;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.SecurityPlan;
import name.abuchen.portfolio.ui.Messages;

public class AccountPlanModel extends AbstractModel
{
    public enum Properties
    {
        name, security, securityCurrencyCode, portfolio, account, accountCurrencyCode, start, interval, amount, fees, transactionCurrencyCode;
    }

    public static final Account DELIVERY = new Account(Messages.InvestmentPlanOptionDelivery);

    protected final Client client;

    protected SecurityPlan source;

    protected String name;
    protected Account account;

    protected LocalDate start = LocalDate.now();

    protected int interval = 1;
    protected long amount;
    protected long fees;

    public AccountPlanModel(Client client)
    {
        this.client = client;
    }

    @Override
    public String getHeading()
    {
        return source != null ? Messages.InvestmentPlanTitleEditPlan : Messages.InvestmentPlanTitleNewPlan;
    }

    @Override
    public void applyChanges()
    {
        if (account == null)
            throw new UnsupportedOperationException(Messages.MsgMissingAccount);

        SecurityPlan plan = source;

        if (plan == null)
        {
            plan = new SecurityPlan();
            this.client.addPlan(plan);
        }
        plan.setName(name);
        plan.setAccount(account.equals(DELIVERY) ? null : account);
        plan.setStart(start);
        plan.setInterval(interval);
        plan.setAmount(amount);
        plan.setFees(fees);
    }

    @Override
    public void resetToNewTransaction()
    {
        this.source = null;

        setName(null);
        setAmount(0);
        setFees(0);
    }

    public void setSource(SecurityPlan plan)
    {
        this.source = plan;

        this.name = plan.getName();
        this.account = plan.getAccount() != null ? plan.getAccount() : DELIVERY;
        this.start = plan.getStart();
        this.interval = plan.getInterval();
        this.amount = plan.getAmount();
        this.fees = plan.getFees();
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        firePropertyChange(Properties.name.name(), this.name, this.name = name);
    }

    public Account getAccount()
    {
        return account;
    }

    public void setAccount(Account account)
    {
        String oldAccountCurrency = getAccountCurrencyCode();
        String oldTransactionCurrency = getTransactionCurrencyCode();
        firePropertyChange(Properties.account.name(), this.account, this.account = account);
        firePropertyChange(Properties.accountCurrencyCode.name(), oldAccountCurrency, getAccountCurrencyCode());
        firePropertyChange(Properties.transactionCurrencyCode.name(), oldTransactionCurrency,
                        getTransactionCurrencyCode());
    }

    public LocalDate getStart()
    {
        return start;
    }

    public void setStart(LocalDate start)
    {
        firePropertyChange(Properties.start.name(), this.start, this.start = start);
    }

    public int getInterval()
    {
        return interval;
    }

    public void setInterval(int interval)
    {
        firePropertyChange(Properties.interval.name(), this.interval, this.interval = interval);
    }

    public long getAmount()
    {
        return amount;
    }

    public void setAmount(long amount)
    {
        firePropertyChange(Properties.amount.name(), this.amount, this.amount = amount);
    }

    public long getFees()
    {
        return fees;
    }

    public void setFees(long fees)
    {
        firePropertyChange(Properties.fees.name(), this.fees, this.fees = fees);
    }

    public String getAccountCurrencyCode()
    {
        return account != null && !DELIVERY.equals(account) ? account.getCurrencyCode() : ""; //$NON-NLS-1$
    }

    public String getReferenceAccountCurrencyCode()
    {
        return account != null ? account.getCurrencyCode() : ""; //$NON-NLS-1$
    }

    public String getTransactionCurrencyCode()
    {
        // transactions will be generated in currency of the account unless it
        // is an inbound delivery (which will be created in the currency of the
        // reference account)
        return account != null && !DELIVERY.equals(account) ? account.getCurrencyCode()
                        : getReferenceAccountCurrencyCode();
    }
}
