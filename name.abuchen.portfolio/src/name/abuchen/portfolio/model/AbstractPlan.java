package name.abuchen.portfolio.model;

import java.time.LocalDate;
import java.util.List;

public abstract class AbstractPlan implements Named, Adaptable
{
    String name;
    String note;
    Account account;

    LocalDate start;
    int interval = 1;

    long amount;
    long fees;

    List<Transaction> transactions;

    public AbstractPlan()
    {
        // needed for xstream de-serialization
    }

    public AbstractPlan(String name)
    {
        this.name = name;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public void setName(String name)
    {
        this.name = name;
    }

    @Override
    public String getNote()
    {
        return note;
    }

    @Override
    public void setNote(String note)
    {
        this.note = note;
    }

    public Account getAccount()
    {
        return account;
    }

    public void setAccount(Account account)
    {
        this.account = account;
    }

    public LocalDate getStart()
    {
        return start;
    }

    public void setStart(LocalDate start)
    {
        this.start = start;
    }

    public int getInterval()
    {
        return interval;
    }

    public void setInterval(int interval)
    {
        this.interval = interval;
    }

    public long getAmount()
    {
        return amount;
    }

    public void setAmount(long amount)
    {
        this.amount = amount;
    }

    public long getFees()
    {
        return fees;
    }

    public void setFees(long fees)
    {
        this.fees = fees;
    }

    public List<Transaction> getTransactions()
    {
        return transactions;
    }

    public void removeTransaction(PortfolioTransaction transaction)
    {
        transactions.remove(transaction);
    }

    public String getCurrencyCode()
    {
        return account.getCurrencyCode();
    }

    @Override
    public <T> T adapt(Class<T> type)
    {
        if (type == Account.class)
            return type.cast(account);
        else
            return null;
    }

    /**
     * Returns the date of the last transaction generated
     */
    LocalDate getLastDate()
    {
        LocalDate last = null;
        for (Transaction t : transactions)
        {
            LocalDate date = t.getDate();
            if (last == null || last.isBefore(date))
                last = date;
        }

        return last;
    }

    /**
     * Returns the date for the next transaction to be generated based on the
     * interval
     */
    LocalDate next(LocalDate date)
    {
        LocalDate startLocalDate = start;

        LocalDate next = date.plusMonths(interval);

        // correct day of month (say the transactions are to be generated on the
        // 31st, but the month has only 30 days)
        next = next.withDayOfMonth(Math.min(next.lengthOfMonth(), startLocalDate.getDayOfMonth()));
        return next;
    }

}
