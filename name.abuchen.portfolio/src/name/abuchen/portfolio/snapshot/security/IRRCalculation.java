package name.abuchen.portfolio.snapshot.security;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import name.abuchen.portfolio.math.IRR;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.CurrencyConverter;
import name.abuchen.portfolio.money.Values;

/* package */class IRRCalculation extends Calculation
{
    private List<LocalDate> dates = new ArrayList<LocalDate>();
    private List<Double> values = new ArrayList<Double>();

    @Override
    public void visit(CurrencyConverter converter, DividendInitialTransaction t)
    {
        dates.add(t.getDate());
        values.add(-t.getMonetaryAmount().with(converter.at(t.getDate())).getAmount() / Values.Amount.divider());
    }

    @Override
    public void visit(CurrencyConverter converter, DividendFinalTransaction t)
    {
        dates.add(t.getDate());
        values.add(t.getMonetaryAmount().with(converter.at(t.getDate())).getAmount() / Values.Amount.divider());
    }

    @Override
    public void visit(CurrencyConverter converter, DividendTransaction t)
    {
        dates.add(t.getDate());
        values.add(t.getMonetaryAmount().with(converter.at(t.getDate())).getAmount() / Values.Amount.divider());
    }

    @Override
    public void visit(CurrencyConverter converter, AccountTransaction t)
    {
        // ignore tax refunds when calculating the irr for a single security
    }

    @Override
    public void visit(CurrencyConverter converter, PortfolioTransaction t)
    {
        dates.add(t.getDate());
        long taxes = t.getUnitSum(Unit.Type.TAX, converter).getAmount();
        long amount = t.getMonetaryAmount().with(converter.at(t.getDate())).getAmount();
        switch (t.getType())
        {
            case BUY:
            case DELIVERY_INBOUND:
            case TRANSFER_IN:
                values.add((-amount + taxes) / Values.Amount.divider());
                break;
            case SELL:
            case DELIVERY_OUTBOUND:
            case TRANSFER_OUT:
                values.add((amount + taxes) / Values.Amount.divider());
                break;
            default:
                throw new UnsupportedOperationException();
        }
    }

    public double getIRR()
    {
        // see #457: if the reporting period contains only tax refunds, dates
        // (and values) can be empty and no IRR can be calculated
        if (dates.size() == 0)
            return Double.NaN;

        return IRR.calculate(dates, values);
    }
}
