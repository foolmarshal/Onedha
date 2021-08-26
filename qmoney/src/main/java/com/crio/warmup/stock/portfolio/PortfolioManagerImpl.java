
package com.crio.warmup.stock.portfolio;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;

import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.crio.warmup.stock.quotes.StockQuotesService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.management.RuntimeErrorException;

import org.springframework.web.client.RestTemplate;

public class PortfolioManagerImpl implements PortfolioManager {

  private StockQuotesService stockQuoteService;

  // Caution: Do not delete or modify the constructor, or else your build will
  // break!
  // This is absolutely necessary for backward compatibility
  protected PortfolioManagerImpl(RestTemplate restTemplate) {
  }

  protected PortfolioManagerImpl(StockQuotesService stockQuoteService) {
    this.stockQuoteService = stockQuoteService;
  }

  // TODO: CRIO_TASK_MODULE_REFACTOR
  // 1. Now we want to convert our code into a module, so we will not call it from
  // main anymore.
  // Copy your code from Module#3
  // PortfolioManagerApplication#calculateAnnualizedReturn
  // into #calculateAnnualizedReturn function here and ensure it follows the
  // method signature.
  // 2. Logic to read Json file and convert them into Objects will not be required
  // further as our
  // clients will take care of it, going forward.

  // Note:
  // Make sure to exercise the tests inside PortfolioManagerTest using command
  // below:
  // ./gradlew test --tests PortfolioManagerTest

  // CHECKSTYLE:OFF

  private Comparator<AnnualizedReturn> getComparator() {
    return Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
  }

  // CHECKSTYLE:OFF

  // TODO: CRIO_TASK_MODULE_REFACTOR
  // Extract the logic to call Tiingo third-party APIs to a separate function.
  // Remember to fill out the buildUri function and use that.

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
    List<Candle> startToEndQuotes = getStockQuote(portfolioTrade.getSymbol(), portfolioTrade.getPurchaseDate(),
        endDate);

    if (startToEndQuotes.size() > 0) {
      Double buyPrice = startToEndQuotes.get(0).getOpen();
      Double sellPrice = startToEndQuotes.get(startToEndQuotes.size() - 1).getClose();
      buySellPrices.add(buyPrice);
      buySellPrices.add(sellPrice);
    }

    return buySellPrices;
  }

  public AnnualizedReturn calculateSingleAnnualizedReturn(LocalDate endDate, PortfolioTrade trade, Double buyPrice,
      Double sellPrice) {

    Double totalReturn = (sellPrice - buyPrice) / buyPrice;
    LocalDate purchaseDate = trade.getPurchaseDate();
    final Double numDaysInYear = 365.0;
    Double durationYears = (double) purchaseDate.until(endDate, ChronoUnit.DAYS) / numDaysInYear;
    Double annualizedReturn = Math.pow(1 + totalReturn, 1 / durationYears) - 1;

    return new AnnualizedReturn(trade.getSymbol(), annualizedReturn, totalReturn);
  }

  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturn(List<PortfolioTrade> portfolioTrades, LocalDate endDate) {
    List<AnnualizedReturn> annualizedReturns = new ArrayList<AnnualizedReturn>();
    portfolioTrades.stream().forEach(portfolioTrade -> {
      try {
        List<Double> buyAndSellPrices = getBuyAndSellPrices(portfolioTrade, endDate);
        if (buyAndSellPrices.size() == 0) {
          throw new RuntimeException(portfolioTrade.getSymbol() + " has no prices between purchase and sell dates.");
        } else {
          Double buyPrice = buyAndSellPrices.get(0);
          Double sellPrice = buyAndSellPrices.get(1);
          AnnualizedReturn annualizedReturn = calculateSingleAnnualizedReturn(endDate, portfolioTrade, buyPrice,
              sellPrice);
          annualizedReturns.add(annualizedReturn);
        }
      } catch (JsonProcessingException e) {
        e.printStackTrace();
      } catch (RuntimeException e) {
        e.printStackTrace();
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
    Collections.sort(annualizedReturns, getComparator());
    return annualizedReturns;
  }

  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturnParallel(List<PortfolioTrade> portfolioTrades,
      LocalDate endDate, int numThreads) throws InterruptedException, StockQuoteServiceException {
    // TODO Auto-generated method stub
    List<AnnualizedReturn> annualizedReturns = new ArrayList<AnnualizedReturn>();
    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    List<Future<AnnualizedReturn>> tasks = new ArrayList<Future<AnnualizedReturn>>();
    portfolioTrades.stream().forEach(portfolioTrade -> {
      Future<AnnualizedReturn> future = executor.submit(
        new MyCallable(this.stockQuoteService, portfolioTrade, endDate));
      tasks.add(future);
    });
    
    try {
      tasks.stream().forEach(task -> {
        try {
          annualizedReturns.add(task.get());
        } catch (InterruptedException | ExecutionException | RuntimeException e) {
          throw new RuntimeException(e.getMessage());
        }
      });
      Collections.sort(annualizedReturns, getComparator());
      return annualizedReturns;
    } catch (RuntimeException e) {
      e.printStackTrace();
      throw new StockQuoteServiceException(e.getMessage());
    } finally {
      executor.shutdown();
    }
}
  
  // ¶TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  //  Modify the function #getStockQuote and start delegating to calls to
  //  stockQuoteService provided via newly added constructor of the class.
  //  You also have a liberty to completely get rid of that function itself, however, make sure
  //  that you do not delete the #getStockQuote function.
}
