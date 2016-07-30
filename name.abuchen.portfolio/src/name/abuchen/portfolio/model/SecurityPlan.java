package name.abuchen.portfolio.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Money;
import name.abuchen.portfolio.money.Values;

public class SecurityPlan extends AbstractPlan
{
    private Security security;
    private Portfolio portfolio;

    public SecurityPlan()
    {
        // needed for xstream de-serialization
    }

    public SecurityPlan(String name)
    {
        this.name = name;
    }

    public Security getSecurity()
    {
        return security;
    }

    public void setSecurity(Security security)
    {
        this.security = security;
    }

    public Portfolio getPortfolio()
    {
        return portfolio;
    }

    public void setPortfolio(Portfolio portfolio)
    {
        this.portfolio = portfolio;
    }

    public void removeTransaction(PortfolioTransaction transaction)
    {
        transactions.remove(transaction);
    }

    public String getCurrencyCode()
    {
        return account != null ? account.getCurrencyCode() : portfolio.getReferenceAccount().getCurrencyCode();
    }

    @Override
    public <T> T adapt(Class<T> type)
    {
        if (type == Security.class)
            return type.cast(security);
        else if (type == Account.class)
            return type.cast(account);
        else if (type == Portfolio.class)
            return type.cast(portfolio);
        else
            return null;
    }

    public List<PortfolioTransaction> generateTransactions(CurrencyConverter converter)
    {
        LocalDate transactionDate = null;

        if (transactions.isEmpty())
            transactionDate = start;
        else
            transactionDate = next(getLastDate());

        List<PortfolioTransaction> newlyCreated = new ArrayList<PortfolioTransaction>();

        LocalDate now = LocalDate.now();

        while (!transactionDate.isAfter(now))
        {
            PortfolioTransaction transaction = createTransaction(converter, transactionDate);

            transactions.add(transaction);
            newlyCreated.add(transaction);

            transactionDate = next(transactionDate);
        }

        return newlyCreated;
    }

    private PortfolioTransaction createTransaction(CurrencyConverter converter, LocalDate tDate)
    {
        String targetCurrencyCode = getCurrencyCode();
        boolean needsCurrencyConversion = !targetCurrencyCode.equals(security.getCurrencyCode());

        Transaction.Unit forex = null;
        long price = getSecurity().getSecurityPrice(tDate).getValue();
        long availableAmount = amount - fees;

        if (needsCurrencyConversion)
        {
            Money availableMoney = Money.of(targetCurrencyCode, amount - fees);
            availableAmount = converter.with(security.getCurrencyCode()).convert(tDate, availableMoney).getAmount();

            forex = new Transaction.Unit(Unit.Type.GROSS_VALUE, //
                            availableMoney, //
                            Money.of(security.getCurrencyCode(), availableAmount), //
                            converter.with(targetCurrencyCode).getRate(tDate, security.getCurrencyCode()).getValue());
        }

        long shares = Math.round(availableAmount * Values.Share.factor() * Values.Quote.factorToMoney()
                        / (double) price);

        if (account != null)
        {
            // create buy transaction

            BuySellEntry entry = new BuySellEntry(portfolio, account);
            entry.setType(PortfolioTransaction.Type.BUY);
            entry.setDate(tDate);
            entry.setShares(shares);
            entry.setCurrencyCode(targetCurrencyCode);
            entry.setAmount(amount);
            entry.setSecurity(getSecurity());

            if (fees != 0)
                entry.getPortfolioTransaction().addUnit(
                                new Transaction.Unit(Unit.Type.FEE, Money.of(targetCurrencyCode, fees)));

            if (forex != null)
                entry.getPortfolioTransaction().addUnit(forex);

            entry.insert();
            return entry.getPortfolioTransaction();
        }
        else
        {
            // create inbound delivery

            PortfolioTransaction transaction = new PortfolioTransaction();
            transaction.setDate(tDate);
            transaction.setType(PortfolioTransaction.Type.DELIVERY_INBOUND);
            transaction.setSecurity(security);
            transaction.setCurrencyCode(targetCurrencyCode);
            transaction.setAmount(amount);
            transaction.setShares(shares);

            if (fees != 0)
                transaction.addUnit(new Transaction.Unit(Unit.Type.FEE, Money.of(targetCurrencyCode, fees)));

            if (forex != null)
                transaction.addUnit(forex);
            portfolio.addTransaction(transaction);
            return transaction;
        }
    }
}
