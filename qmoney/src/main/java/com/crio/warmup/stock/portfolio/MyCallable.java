package com.crio.warmup.stock.portfolio;

import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.crio.warmup.stock.quotes.StockQuotesService;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class MyCallable implements Callable<AnnualizedReturn> {

  private PortfolioTrade portfolioTrade;
  private LocalDate endDate;
  private StockQuotesService stockQuoteService;

  protected MyCallable(StockQuotesService stockQuoteService,
      PortfolioTrade portfolioTrade, LocalDate endDate) {
    this.stockQuoteService = stockQuoteService;
    this.portfolioTrade = portfolioTrade;
    this.endDate = endDate;
  }

  public List<Candle> getStockQuote(String symbol, LocalDate from,
      LocalDate to)
      throws StockQuoteServiceException, JsonProcessingException, RuntimeException {
    try {
      return this.stockQuoteService.getStockQuote(symbol, from, to);
    } catch (StockQuoteServiceException e) {
      e.printStackTrace();
      throw e;
    } catch (JsonProcessingException e) {
      e.printStackTrace();
      throw e;
    } catch (RuntimeException e) {
      e.printStackTrace();
      throw e;
    }
  }

  protected String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
    final String apiKey = "570e8e6635bd024a3c7e93ff30e1f563719a55eb";
    String uriTemplate = "https://api.tiingo.com/tiingo/daily/" + symbol + "/prices?" + "startDate="
        + startDate.toString() + "&endDate=" + endDate.toString() + "&token=" + apiKey;
    return uriTemplate;
  }

  public List<Double> getBuyAndSellPrices(PortfolioTrade portfolioTrade, LocalDate endDate)
      throws JsonProcessingException, StockQuoteServiceException, RuntimeException {

    List<Double> buySellPrices = new ArrayList<Double>();
    List<Candle> startToEndQuotes = getStockQuote(
        portfolioTrade.getSymbol(), portfolioTrade.getPurchaseDate(),endDate);

    if (startToEndQuotes.size() > 0) {
      Double buyPrice = startToEndQuotes.get(0).getOpen();
      Double sellPrice = startToEndQuotes.get(startToEndQuotes.size() - 1).getClose();
      buySellPrices.add(buyPrice);
      buySellPrices.add(sellPrice);
    }

    return buySellPrices;
  }

  public AnnualizedReturn calculateSingleAnnualizedReturn(LocalDate endDate,
      PortfolioTrade trade, Double buyPrice, Double sellPrice) {

    Double totalReturn = (sellPrice - buyPrice) / buyPrice;
    LocalDate purchaseDate = trade.getPurchaseDate();
    final Double numDaysInYear = 365.0;
    Double durationYears = (double) purchaseDate.until(endDate, ChronoUnit.DAYS) / numDaysInYear;
    Double annualizedReturn = Math.pow(1 + totalReturn, 1 / durationYears) - 1;

    return new AnnualizedReturn(trade.getSymbol(), annualizedReturn, totalReturn);
  }

  @Override
  public AnnualizedReturn call() throws Exception {
    // TODO Auto-generated method stub
    try {
      List<Double> buyAndSellPrices = getBuyAndSellPrices(
          portfolioTrade, endDate);
      if (buyAndSellPrices.size() == 0) {
        throw new RuntimeException(portfolioTrade.getSymbol()
            + " has no prices between purchase and sell dates.");
      } else {
        Double buyPrice = buyAndSellPrices.get(0);
        Double sellPrice = buyAndSellPrices.get(1);
        AnnualizedReturn annualizedReturn = calculateSingleAnnualizedReturn(
            endDate, portfolioTrade, buyPrice, sellPrice);
        return annualizedReturn;
      }
    } catch (JsonProcessingException e) {
      e.printStackTrace();
      throw e;
    } catch (RuntimeException e) {
      e.printStackTrace();
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }
    
}
