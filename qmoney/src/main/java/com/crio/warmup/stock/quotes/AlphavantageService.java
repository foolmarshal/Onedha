
package com.crio.warmup.stock.quotes;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;

import com.crio.warmup.stock.dto.AlphavantageCandle;
import com.crio.warmup.stock.dto.AlphavantageDailyResponse;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.web.client.RestTemplate;

public class AlphavantageService implements StockQuotesService {

  private RestTemplate restTemplate;

  protected AlphavantageService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }
  
  private ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }

  private Boolean dateBetweenFromAndTo(LocalDate objDate, LocalDate from, LocalDate to) {
    if (objDate.isEqual(from) || objDate.isEqual(to)) {
      return true;
    }
    return objDate.isAfter(from) && objDate.isBefore(to);
  }

  private Comparator<Candle> getComparator() {
    return Comparator.comparing(Candle::getDate);
  }

  @Override
  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
      throws JsonProcessingException, StockQuoteServiceException {
    String url = buildUri(symbol);
    ObjectMapper objectMapper = getObjectMapper();
    RestTemplate restTemplate = this.restTemplate;
    String jsonResponse = restTemplate.getForObject(url, String.class);
    AlphavantageDailyResponse response = objectMapper.readValue(
        jsonResponse, AlphavantageDailyResponse.class);
    if (response.getCandles() == null || response.getCandles().size() == 0) {
      throw new StockQuoteServiceException(jsonResponse);
    } else {
      List<Candle> startToEndStockQuotes = new ArrayList<Candle>();
      response.getCandles().entrySet().forEach(quote -> {
        if (dateBetweenFromAndTo(quote.getKey(), from, to)) {
          AlphavantageCandle alphavantageCandle = quote.getValue();
          alphavantageCandle.setDate(quote.getKey());
          startToEndStockQuotes.add(alphavantageCandle);
        }
      });
      Collections.sort(startToEndStockQuotes, getComparator());
      return startToEndStockQuotes;
    }
  }

  // TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  //  Implement the StockQuoteService interface as per the contracts. Call Alphavantage service
  //  to fetch daily adjusted data for last 20 years.
  //  Refer to documentation here: https://www.alphavantage.co/documentation/
  //  --
  //  The implementation of this functions will be doing following tasks:
  //    1. Build the appropriate url to communicate with third-party.
  //       The url should consider startDate and endDate if it is supported by the provider.
  //    2. Perform third-party communication with the url prepared in step#1
  //    3. Map the response and convert the same to List<Candle>
  //    4. If the provider does not support startDate and endDate, then the implementation
  //       should also filter the dates based on startDate and endDate. Make sure that
  //       result contains the records for for startDate and endDate after filtering.
  //    5. Return a sorted List<Candle> sorted ascending based on Candle#getDate
  // Note:
  // 1. Make sure you use {RestTemplate#getForObject(URI, String)} else the test will fail.
  // 2. Run the tests using command below and make sure it passes:
  //    ./gradlew test --tests AlphavantageServiceTest
  //CHECKSTYLE:OFF
    //CHECKSTYLE:ON
  // TODO: CRIO_TASK_MODULE_ADDITIONAL_REFACTOR
  //  1. Write a method to create appropriate url to call Alphavantage service. The method should
  //     be using configurations provided in the {@link @application.properties}.
  //  2. Use this method in #getStockQuote.

  protected String buildUri(String symbol) {
    final String apiKey = "ZN3RJEOP1G7O1NM0";
    String uriTemplate = "https://www.alphavantage.co/query"
            + "?function=TIME_SERIES_DAILY"
            + "&symbol=" + symbol
            + "&outputsize=full"
            + "&apikey=" + apiKey;
    return uriTemplate;
  }
  // TODO: CRIO_TASK_MODULE_EXCEPTIONS
  //   1. Update the method signature to match the signature change in the interface.
  //   2. Start throwing new StockQuoteServiceException when you get some invalid response from
  //      Alphavantage, or you encounter a runtime exception during Json parsing.
  //   3. Make sure that the exception propagates all the way from PortfolioManager, so that the
  //      external user's of our API are able to explicitly handle this exception upfront.
  //CHECKSTYLE:OFF

}

