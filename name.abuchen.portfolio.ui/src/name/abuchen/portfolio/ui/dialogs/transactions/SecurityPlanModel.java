package name.abuchen.portfolio.ui.dialogs.transactions;

import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.SecurityPlan;
import name.abuchen.portfolio.model.Portfolio;
import name.abuchen.portfolio.model.Security;
import name.abuchen.portfolio.ui.Messages;

public class SecurityPlanModel extends AccountPlanModel
{

    private Security security;
    private Portfolio portfolio;

    public SecurityPlanModel(Client client)
    {
        super(client);
    }

    public void applyChanges()
    {
        if (account == null)
            throw new UnsupportedOperationException(Messages.MsgMissingAccount);
        if (security == null)
            throw new UnsupportedOperationException(Messages.MsgMissingSecurity);
        if (portfolio == null)
            throw new UnsupportedOperationException(Messages.MsgMissingPortfolio);
        SecurityPlan plan = source;
        if (plan == null)
        {
            plan = new SecurityPlan();
            this.client.addPlan(plan);
        }
        plan.setSecurity(security);
        plan.setPortfolio(portfolio);
        plan.setName(name);
        plan.setAccount(account.equals(DELIVERY) ? null : account);
        plan.setStart(start);
        plan.setInterval(interval);
        plan.setAmount(amount);
        plan.setFees(fees);
    }

    @Override
    public void setSource(SecurityPlan plan)
    {
        super.setSource(plan);
        this.security = plan.getSecurity();
        this.portfolio = plan.getPortfolio();

    }

    public Security getSecurity()
    {
        return security;
    }

    public void setSecurity(Security security)
    {
        String oldSecurityCurrency = getSecurityCurrencyCode();
        String oldTransactionCurrency = getTransactionCurrencyCode();
        firePropertyChange(Properties.security.name(), this.security, this.security = security);
        firePropertyChange(Properties.securityCurrencyCode.name(), oldSecurityCurrency, getSecurityCurrencyCode());
        firePropertyChange(Properties.transactionCurrencyCode.name(), oldTransactionCurrency,
                        getTransactionCurrencyCode());
    }

    public Portfolio getPortfolio()
    {
        return portfolio;
    }

    public void setPortfolio(Portfolio portfolio)
    {
        String oldTransactionCurrency = getTransactionCurrencyCode();
        firePropertyChange(Properties.portfolio.name(), this.portfolio, this.portfolio = portfolio);
        firePropertyChange(Properties.transactionCurrencyCode.name(), oldTransactionCurrency,
                        getTransactionCurrencyCode());
    }

    public String getSecurityCurrencyCode()
    {
        return security != null ? security.getCurrencyCode() : ""; //$NON-NLS-1$
    }

}
