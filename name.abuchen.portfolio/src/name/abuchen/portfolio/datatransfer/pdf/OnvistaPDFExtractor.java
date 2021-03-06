package name.abuchen.portfolio.datatransfer.pdf;

import java.io.IOException;

import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Block;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.DocumentType;
import name.abuchen.portfolio.datatransfer.pdf.PDFParser.Transaction;
import name.abuchen.portfolio.model.AccountTransaction;
import name.abuchen.portfolio.model.BuySellEntry;
import name.abuchen.portfolio.model.Client;
import name.abuchen.portfolio.model.PortfolioTransaction;
import name.abuchen.portfolio.model.Transaction.Unit;
import name.abuchen.portfolio.money.Money;

@SuppressWarnings("nls")
public class OnvistaPDFExtractor extends AbstractPDFExtractor
{

    public OnvistaPDFExtractor(Client client) throws IOException
    {
        super(client);

        addBankIdentifier(""); //$NON-NLS-1$

        addBuyTransaction();
        addSellTransaction();
        addChangeTransaction();
        addPayingTransaction();
        addDividendTransaction();
        addBackOfProfitsTransaction();
        addTransferInTransaction();
    }

    private void addBuyTransaction()
    {
        DocumentType type = new DocumentType("Wir haben für Sie gekauft");
        this.addDocumentTyp(type);

        Block block = new Block("Wir haben für Sie gekauft(.*)");
        type.addBlock(block);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<BuySellEntry>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        block.set(pdfTransaction);
        pdfTransaction.section("name", "isin") //
                        .find("Gattungsbezeichnung ISIN") //
                        .match("(?<name>.*) (?<isin>[^ ]\\S*)$") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("notation", "shares") //
                        .find("Nominal Kurs") //
                        .match("(?<notation>^\\w{3}+) (?<shares>\\d{1,3}(\\.\\d{3})*(,\\d{3})?)(.*)") //
                        .assign((t, v) -> {
                            String notation = v.get("notation");
                            if (notation != null && !notation.equalsIgnoreCase("STK"))
                            {
                                // Prozent-Notierung, Workaround..
                                t.setShares((asShares(v.get("shares")) / 100));
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                        })

                        .section("date", "amount", "currency") //
                        .find("Wert(\\s+)Konto-Nr. Betrag zu Ihren Lasten(\\s*)$")
                        // 14.01.2015 172305047 EUR 59,55
                        // Wert Konto-Nr. Betrag zu Ihren Lasten
                        // 01.06.2011 172305047 EUR 6,40
                        .match("(?<date>\\d+.\\d+.\\d{4}+) (\\d{6,12}) (?<currency>\\w{3}+) (?<amount>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)$")
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .wrap(t -> new BuySellEntryItem(t));
        addFeesSections(pdfTransaction);
    }

    private void addSellTransaction()
    {
        DocumentType type = new DocumentType("Wir haben für Sie verkauft");
        this.addDocumentTyp(type);

        Block block = new Block("Wir haben für Sie verkauft(.*)");
        type.addBlock(block);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<BuySellEntry>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.SELL);
            return entry;
        });

        block.set(pdfTransaction);
        pdfTransaction.section("name", "isin") //
                        .find("Gattungsbezeichnung ISIN") //
                        .match("(?<name>.*) (?<isin>[^ ]\\S*)$") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("notation", "shares").find("Nominal Kurs")
                        .match("(?<notation>^\\w{3}+) (?<shares>\\d{1,3}(\\.\\d{3})*(,\\d{3})?)(.*)") //
                        .assign((t, v) -> {
                            String notation = v.get("notation");
                            if (notation != null && !notation.equalsIgnoreCase("STK"))
                            {
                                // Prozent-Notierung, Workaround..
                                t.setShares((asShares(v.get("shares")) / 100));
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                        })

                        .section("date", "amount", "currency")
                        .find("Wert(\\s+)Konto-Nr. Betrag zu Ihren Gunsten(\\s*)$")
                        // 12.04.2011 172305047 EUR 21,45
                        .match("(?<date>\\d+.\\d+.\\d{4}+) (\\d{6,12}) (?<currency>\\w{3}+) (?<amount>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)")
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .wrap(t -> new BuySellEntryItem(t));
        addTaxesSectionsBuySell(pdfTransaction);
        addFeesSections(pdfTransaction);
    }

    private void addChangeTransaction()
    {
        DocumentType type = new DocumentType("Bestätigung");
        this.addDocumentTyp(type);

        Block block = new Block("Bestätigung(.*)");
        type.addBlock(block);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<BuySellEntry>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.BUY);
            return entry;
        });

        block.set(pdfTransaction);
        pdfTransaction.section("name", "isin") //
                        .find("Gattungsbezeichnung ISIN") //
                        .match("(?<name>.*) (?<isin>[^ ]\\S*)$") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("notation", "shares") //
                        .find("Nominal Kurs")
                        .match("(?<notation>^\\w{3}+) (?<shares>\\d{1,3}(\\.\\d{3})*(,\\d{3})?)(.*)") //
                        .assign((t, v) -> {
                            String notation = v.get("notation");
                            if (notation != null && !notation.equalsIgnoreCase("STK"))
                            {
                                // Prozent-Notierung, Workaround..
                                t.setShares((asShares(v.get("shares")) / 100));
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                        })

                        .section("date", "amount", "currency") //
                        .find("Wert(\\s+)Konto-Nr. Betrag zu Ihren Lasten(\\s*)$")
                        // 14.01.2015 172305047 EUR 59,55
                        // Wert Konto-Nr. Betrag zu Ihren Lasten
                        // 01.06.2011 172305047 EUR 6,40
                        .match("(?<date>\\d+.\\d+.\\d{4}+) (\\d{6,12}) (?<currency>\\w{3}+) (?<amount>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)$")
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        }).wrap(t -> new BuySellEntryItem(t));
        addFeesSections(pdfTransaction);
    }

    private void addPayingTransaction()
    {
        DocumentType type = new DocumentType("Gutschriftsanzeige");
        this.addDocumentTyp(type);

        Block block = new Block("Gutschriftsanzeige(.*)");
        type.addBlock(block);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<BuySellEntry>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.SELL);
            return entry;
        });

        block.set(pdfTransaction);
        pdfTransaction.section("name", "isin") //
                        .find("Gattungsbezeichnung (.*) ISIN") //
                        .match("(?<name>.*) (.*) (?<isin>[^ ]\\S*)$") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("notation", "shares") //
                        .find("Nominal Einlösung(.*)$") //
                        .match("(?<notation>^\\w{3}+) (?<shares>\\d{1,3}(\\.\\d{3})*(,\\d{3})?)(.*)$")
                        .assign((t, v) -> {
                            String notation = v.get("notation");
                            if (notation != null && !notation.equalsIgnoreCase("STK"))
                            {
                                // Prozent-Notierung, Workaround..
                                t.setShares((asShares(v.get("shares")) / 100));
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                        })

                        .section("date", "amount", "currency") //
                        .find("Wert(\\s+)Konto-Nr. Betrag zu Ihren Gunsten$")
                        // 17.11.2014 172305047 EUR 51,85
                        .match("(?<date>\\d+.\\d+.\\d{4}+) (\\d{6,12}) (?<currency>\\w{3}+) (?<amount>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)")
                        .assign((t, v) -> {
                            t.setDate(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .wrap(t -> new BuySellEntryItem(t));
        addTaxesSectionsBuySell(pdfTransaction);
    }

    private void addDividendTransaction()
    {
        DocumentType type = new DocumentType("Erträgnisgutschrift");
        this.addDocumentTyp(type);

        // Erträgnisgutschrift allein ist nicht gut hier, da es schon in der
        // Kopfzeile steht..
        Block block = new Block("Dividendengutschrift.*|Kupongutschrift.*|Erträgnisgutschrift.*(\\d+.\\d+.\\d{4})");
        type.addBlock(block);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>();
        pdfTransaction.subject(() -> {
            AccountTransaction transaction = new AccountTransaction();
            transaction.setType(AccountTransaction.Type.DIVIDENDS);
            return transaction;
        });

        block.set(pdfTransaction);
        pdfTransaction

                        .section("name", "isin") //
                        .find("Gattungsbezeichnung(.*) ISIN")
                        // Commerzbank AG Inhaber-Aktien o.N. DE000CBK1001
                        // 5,5% TUI AG Wandelanl.v.2009(2014) 17.11.2014
                        // 17.11.2010 DE000TUAG117
                        .match("(?<name>.*?) (\\d+.\\d+.\\d{4} ){0,2}(?<isin>[^ ]\\S*)$") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("notation", "shares", "date", "amount", "currency")
                        // .find("Nominal (Ex-Tag )?Zahltag (.*etrag pro
                        // .*)?(Zinssatz.*)?")
                        // STK 25,000 17.05.2013 17.05.2013 EUR 0,700000
                        // Leistungen aus dem steuerlichen Einlagenkonto (§27
                        // KStG) EUR 17,50
                        .match("(?<notation>^\\w{3}+) (?<shares>\\d{1,3}(\\.\\d{3})*(,\\d{3})?) (\\d+.\\d+.\\d{4}+) (?<date>\\d+.\\d+.\\d{4}+)?(.*)")
                        .match("(?<date>\\d+.\\d+.\\d{4}+)?(\\d{6,12})?(.{7,58} )?(?<currency>\\w{3}+) (?<amount>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)")
                        .assign((t, v) -> {
                            String notation = v.get("notation");
                            if (notation != null && !notation.equalsIgnoreCase("STK"))
                            {
                                // Prozent-Notierung, Workaround..
                                t.setShares((asShares(v.get("shares")) / 100));
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                            t.setDate(asDate(v.get("date")));
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .wrap(t -> new TransactionItem(t));

        addTaxesSectionsAccount(pdfTransaction);

        // optional: Reinvestierung in:
        block = new Block("Reinvestierung.*");
        type.addBlock(block);
        block.set(new Transaction<BuySellEntry>()

                        .subject(() -> {
                            BuySellEntry entry = new BuySellEntry();
                            entry.setType(PortfolioTransaction.Type.BUY);
                            return entry;
                        })

                        .section("date")
                        .match("(^\\w{3}+) (\\d{1,3}(\\.\\d{3})*(,\\d{3})?) (\\d+.\\d+.\\d{4}+) (?<date>\\d+.\\d+.\\d{4}+)?(.*)")
                        .assign((t, v) -> t.setDate(asDate(v.get("date"))))

                        .section("name", "isin") //
                        .find("Die Dividende wurde wie folgt in neue Aktien reinvestiert:")
                        .find("Gattungsbezeichnung ISIN") //
                        .match("(?<name>.*) (?<isin>[^ ]\\S*)$") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("notation", "shares", "amount", "currency") //
                        .find("Nominal Reinvestierungspreis")
                        // STK 25,000 EUR 0,700000
                        .match("(?<notation>^\\w{3}+) (?<shares>\\d{1,3}(\\.\\d{3})*(,\\d{3})?) (?<currency>\\w{3}+) (?<amount>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(.*)")
                        .assign((t, v) -> {
                            String notation = v.get("notation");
                            if (notation != null && !notation.equalsIgnoreCase("STK"))
                            {
                                // Prozent-Notierung, Workaround..
                                t.setShares((asShares(v.get("shares")) / 100));
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        })

                        .wrap(t -> new BuySellEntryItem(t)));
    }

    private void addBackOfProfitsTransaction()
    {
        DocumentType type = new DocumentType("Ertragsthesaurierung");
        this.addDocumentTyp(type);

        Block block = new Block("Ertragsthesaurierung(.*)");
        type.addBlock(block);

        Transaction<AccountTransaction> pdfTransaction = new Transaction<AccountTransaction>();
        pdfTransaction.subject(() -> {
            AccountTransaction transaction = new AccountTransaction();
            transaction.setType(AccountTransaction.Type.DIVIDENDS);
            return transaction;
        });

        block.set(pdfTransaction);
        pdfTransaction.section("name", "isin") //
                        .find("Gattungsbezeichnung(.*) ISIN")
                        // Commerzbank AG Inhaber-Aktien o.N. DE000CBK1001
                        // 5,5% TUI AG Wandelanl.v.2009(2014) 17.11.2014
                        // 17.11.2010 DE000TUAG117
                        .match("(?<name>.*?) (\\d+.\\d+.\\d{4} ){0,2}(?<isin>[^ ]\\S*)$") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("notation", "shares", "date")
                        .find("Nominal (Ex-Tag )?Zahltag (.*etrag pro .*)?(Zinssatz.*)?")
                        // STK 28,000 02.03.2015 04.03.2015 EUR 0,088340
                        .match("(?<notation>^\\w{3}+) (?<shares>\\d{1,3}(\\.\\d{3})*(,\\d{3})?) (\\d+.\\d+.\\d{4}+) (?<date>\\d+.\\d+.\\d{4}+)(.*)")
                        .assign((t, v) -> {
                            String notation = v.get("notation");
                            if (notation != null && !notation.equalsIgnoreCase("STK"))
                            {
                                // Prozent-Notierung, Workaround..
                                t.setShares((asShares(v.get("shares")) / 100));
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                            t.setDate(asDate(v.get("date")));
                        })

                        .section("amount", "currency")
                        .match("Steuerpflichtiger Betrag (.*) (?<currency>\\w{3}+) (?<amount>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)")
                        .assign((t, v) -> {
                            t.setAmount(asAmount(v.get("amount")));
                            t.setCurrencyCode(asCurrencyCode(v.get("currency")));
                        }).wrap(t -> new TransactionItem(t));
        addTaxesSectionsAccount(pdfTransaction);
    }

    private void addTransferInTransaction()
    {
        DocumentType type = new DocumentType("Wir erhielten zu Gunsten Ihres Depots");
        this.addDocumentTyp(type);

        Block block = new Block("Wir erhielten zu Gunsten Ihres Depots(.*)");
        type.addBlock(block);

        Transaction<BuySellEntry> pdfTransaction = new Transaction<BuySellEntry>();
        pdfTransaction.subject(() -> {
            BuySellEntry entry = new BuySellEntry();
            entry.setType(PortfolioTransaction.Type.TRANSFER_IN);
            return entry;
        });

        block.set(pdfTransaction);
        pdfTransaction.section("name", "isin") //
                        .find("Gattungsbezeichnung ISIN") //
                        .match("(?<name>.*) (?<isin>[^ ]\\S*)$") //
                        .assign((t, v) -> {
                            t.setSecurity(getOrCreateSecurity(v));
                        })

                        .section("notation", "shares", "date") //
                        .find("Nominal Schlusstag Wert")
                        // STK 28,000 02.12.2011 02.12.2011
                        .match("(?<notation>^\\w{3}+) (?<shares>\\d{1,3}(\\.\\d{3})*(,\\d{3})?) (\\d+.\\d+.\\d{4}+) (?<date>\\d+.\\d+.\\d{4}+)(.*)")
                        .assign((t, v) -> {
                            String notation = v.get("notation");
                            if (notation != null && !notation.equalsIgnoreCase("STK"))
                            {
                                // Prozent-Notierung, Workaround..
                                t.setShares((asShares(v.get("shares")) / 100));
                            }
                            else
                            {
                                t.setShares(asShares(v.get("shares")));
                            }
                            t.setDate(asDate(v.get("date")));
                            t.setCurrencyCode(asCurrencyCode(
                                            t.getPortfolioTransaction().getSecurity().getCurrencyCode()));
                        })

                        .wrap(t -> new BuySellEntryItem(t));
        addFeesSections(pdfTransaction);
    }

    private void addTaxesSectionsAccount(Transaction<AccountTransaction> pdfTransaction)
    {
        pdfTransaction.section("tax").optional()
                        .match("^Kapitalertragsteuer (?<currency>\\w{3}+) (?<tax>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(-)")
                        .assign((t, v) -> {
                            t.addUnit(new Unit(Unit.Type.TAX,
                                            Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")))));
                        }).section("soli").optional()
                        .match("^Solidaritätszuschlag (?<currency>\\w{3}+) (?<soli>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(-)")
                        .assign((t, v) -> {
                            t.addUnit(new Unit(Unit.Type.TAX,
                                            Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("soli")))));
                        }).section("kirchenst").optional()
                        .match("^Kirchensteuer (?<currency>\\w{3}+) (?<kirchenst>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(-)")
                        .assign((t, v) -> {
                            t.addUnit(new Unit(Unit.Type.TAX,
                                            Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("kirchenst")))));
                        });
    }

    private void addTaxesSectionsBuySell(Transaction<BuySellEntry> pdfTransaction)
    {
        pdfTransaction.section("tax").optional()
                        // Kapitalertragsteuer EUR 4,22-
                        .match("^Kapitalertragsteuer (?<currency>\\w{3}+) (?<tax>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(-)")
                        .assign((t, v) -> {
                            t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX,
                                            Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("tax")))));
                        }).section("soli").optional()
                        .match("^Solidaritätszuschlag (?<currency>\\w{3}+) (?<soli>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(-)")
                        .assign((t, v) -> {
                            t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX,
                                            Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("soli")))));
                        }).section("kirchenst").optional()
                        .match("^Kirchensteuer (?<currency>\\w{3}+) (?<kirchenst>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(-)")
                        .assign((t, v) -> {
                            t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.TAX,
                                            Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("kirchenst")))));
                        });

    }

    private void addFeesSections(Transaction<BuySellEntry> pdfTransaction)
    {
        pdfTransaction.section("brokerage").optional()
                        .match("(^.*)(Orderprovision) (\\w{3}+) (?<brokerage>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(-)")
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("brokerage"))))))
                        .section("stockfees").optional()
                        .match("(^.*) (B\\Drsengeb\\Dhr) (\\w{3}+) (?<stockfees>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(-)")
                        .assign((t, v) -> t.getPortfolioTransaction()
                                        .addUnit(new Unit(Unit.Type.FEE,
                                                        Money.of(asCurrencyCode(v.get("currency")),
                                                                        asAmount(v.get("stockfees"))))))
                        .section("agent").optional()
                        .match("(^.*)(Maklercourtage)(\\s+)(\\w{3}+) (?<agent>\\d{1,3}(\\.\\d{3})*(,\\d{2})?)(-)")
                        .assign((t, v) -> t.getPortfolioTransaction().addUnit(new Unit(Unit.Type.FEE,
                                        Money.of(asCurrencyCode(v.get("currency")), asAmount(v.get("agent"))))));

    }

    @Override
    public String getLabel()
    {
        return "Onvista"; //$NON-NLS-1$
    }
}
