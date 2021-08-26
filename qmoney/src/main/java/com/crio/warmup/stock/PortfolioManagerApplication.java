package com.crio.warmup.stock;

import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.dto.TotalReturnsDto;
import com.crio.warmup.stock.log.UncaughtExceptionHandler;
import com.crio.warmup.stock.portfolio.PortfolioManager;
import com.crio.warmup.stock.portfolio.PortfolioManagerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.web.client.RestTemplate;

public class PortfolioManagerApplication {

  // TODO: CRIO_TASK_MODULE_REST_API
  // Find out the closing price of each stock on the end_date and return the list
  // of all symbols in ascending order by its close value on end date.

  // Note:
  // 1. You may have to register on Tiingo to get the api_token.
  // 2. Look at args parameter and the module instructions carefully.
  // 2. You can copy relevant code from #mainReadFile to parse the Json.
  // 3. Use RestTemplate#getForObject in order to call the API,
  // and deserialize the results in List<Candle>

  public static void printJsonObject(Object object) throws IOException {
    Logger logger = Logger.getLogger(PortfolioManagerApplication.class.getCanonicalName());
    ObjectMapper mapper = new ObjectMapper();
    logger.info(mapper.writeValueAsString(object));
  }

  public static File resolveFileFromResources(String filename) throws URISyntaxException {
    return Paths.get(Thread.currentThread()
                          .getContextClassLoader()
                          .getResource(filename)
                          .toURI()
                    ).toFile();
  }

  public static ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }

  // TODO: CRIO_TASK_MODULE_JSON_PARSING
  // Follow the instructions provided in the task documentation and fill up the
  // correct values for
  // the variables provided. First value is provided for your reference.
  // A. Put a breakpoint on the first line inside mainReadFile() which says
  // return Collections.emptyList();
  // B. Then Debug the test #mainReadFile provided in
  // PortfoliomanagerApplicationTest.java
  // following the instructions to run the test.
  // Once you are able to run the test, perform following tasks and record the
  // output as a
  // String in the function below.
  // Use this link to see how to evaluate expressions -
  // https://code.visualstudio.com/docs/editor/debugging#_data-inspection
  // 1. evaluate the value of "args[0]" and set the value
  // to the variable named valueOfArgument0 (This is implemented for your
  // reference.)
  // 2. In the same window, evaluate the value of expression below and set it
  // to resultOfResolveFilePathArgs0
  // expression ==> resolveFileFromResources(args[0])
  // 3. In the same window, evaluate the value of expression below and set it
  // to toStringOfObjectMapper.
  // You might see some garbage numbers in the output. Dont worry, its expected.
  // expression ==> getObjectMapper().toString()
  // 4. Now Go to the debug window and open stack trace. Put the name of the
  // function you see at
  // second place from top to variable functionNameFromTestFileInStackTrace
  // 5. In the same window, you will see the line number of the function in the
  // stack trace window.
  // assign the same to lineNumberFromTestFileInStackTrace
  // Once you are done with above, just run the corresponding test and
  // make sure its working as expected. use below command to do the same.
  // ./gradlew test --tests PortfolioManagerApplicationTest.testDebugValues

  public static List<String> debugOutputs() {

    String valueOfArgument0 = "trades.json";
    String resultOfResolveFilePathArgs0 = 
        "/home/crio-user/workspace/aakashkumarjagia1997-ME_QMONEY/qmoney/bin/main/trades.json";
    String toStringOfObjectMapper = "com.fasterxml.jackson.databind.ObjectMapper@46268f08";
    String functionNameFromTestFileInStackTrace = "PortfolioManagerApplication.mainReadFile";
    String lineNumberFromTestFileInStackTrace = "22";

    return Arrays.asList(new String[] { valueOfArgument0,
                                        resultOfResolveFilePathArgs0,
                                        toStringOfObjectMapper,
                                        functionNameFromTestFileInStackTrace,
                                        lineNumberFromTestFileInStackTrace });
  }

  // TODO: CRIO_TASK_MODULE_JSON_PARSING
  // Read the json file provided in the argument[0]. The file will be available in
  // the classpath.
  // 1. Use #resolveFileFromResources to get actual file from classpath.
  // 2. Extract stock symbols from the json file with ObjectMapper provided by
  // #getObjectMapper.
  // 3. Return the list of all symbols in the same order as provided in json.

  // Note:
  // 1. There can be few unused imports, you will need to fix them to make the
  // build pass.
  // 2. You can use "./gradlew build" to check if your code builds successfully.

  public static List<String> mainReadFile(String[] args) throws IOException, URISyntaxException {
    String fileName = args[0].toString();
    String filePath = resolveFileFromResources(fileName).toString();
    ObjectMapper objectMapper = getObjectMapper();
    List<String> clientPortfolioTradeSymbols = new ArrayList<String>();
    try (FileInputStream fileInputStream = new FileInputStream(filePath)) {
      PortfolioTrade[] clientPortfolioTradeObject = objectMapper.readValue(
        fileInputStream,
        PortfolioTrade[].class
      );
      Arrays.stream(clientPortfolioTradeObject).forEach(portfolioTradeobject -> {
        clientPortfolioTradeSymbols.add(portfolioTradeobject.getSymbol());
      });
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return clientPortfolioTradeSymbols;
  }

  // Note:
  // 1. You may need to copy relevant code from #mainReadQuotes to parse the Json.
  // 2. Remember to get the latest quotes from Tiingo API.

  // Note:
  // Remember to confirm that you are getting same results for annualized returns
  // as in Module 3.

  public static String generateTiingoUrl(String tickerSymbol, String startDate,
                 String endDate, String token) {
    return "https://api.tiingo.com/tiingo/daily/" + tickerSymbol + "/"
            + "prices?" + "startDate=" + startDate + "&"
            + "endDate=" + endDate + "&"
            + "token=" + token;
  }

  public static List<Candle> getQuotes(String tickerSymbol, String startDate,
                 String endDate, String token)
      throws Exception {
    ObjectMapper objectMapper = getObjectMapper();
    RestTemplate restTemplate = new RestTemplate();
    String url = generateTiingoUrl(tickerSymbol, startDate, endDate, token);

    try {
      String response = restTemplate.getForObject(url, String.class);
      List<Candle> startToEndQuotes = new ArrayList<>();
      try {
        TiingoCandle[] tiingoQuotes = objectMapper.readValue(response, TiingoCandle[].class);
        Arrays.stream(tiingoQuotes).forEach(tiingoQuote -> {
          startToEndQuotes.add(tiingoQuote);
        });
        if (startToEndQuotes.size() == 0) {
          throw new RuntimeException("No prices between start and end dates");
        }
        return startToEndQuotes;
      } catch (Exception e) {
        e.printStackTrace();
        throw e;
      }
    } catch (RuntimeException e) {
      e.printStackTrace();
      throw e;
    }

  }

  public static Candle getEndDateQuote(String tickerSymbol, String startDate,
                         String endDate, String token)
      throws Exception {
    try {
      return getQuotes(tickerSymbol, startDate, endDate, token)
            .stream()
            .reduce((first, second) -> second)
            .get();
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }

  public static List<String> getSortedTradeSymbols(PortfolioTrade[] clientPortfolioTrades,
                                             String endDate) {
    List<String> tradeSymbols = new ArrayList<String>();
    List<TotalReturnsDto> tickerSymbolClosePrice = new ArrayList<TotalReturnsDto>();
    try {
      Arrays.stream(clientPortfolioTrades).forEach(portfolioTrade -> {
        String startDate = portfolioTrade.getPurchaseDate().toString();
        if (LocalDate.parse(startDate).compareTo(LocalDate.parse(endDate)) > 0) {
          throw new RuntimeException("start date greater than end date");
        } else {
          String tickerSymbol = portfolioTrade.getSymbol();
          String token = "570e8e6635bd024a3c7e93ff30e1f563719a55eb";
          Candle endDateQuote = new TiingoCandle();
          try {
            endDateQuote = getEndDateQuote(tickerSymbol, startDate, endDate, token);
            tickerSymbolClosePrice.add(
                new TotalReturnsDto(tickerSymbol, endDateQuote.getClose())
            );
          } catch (Exception e) {
            e.printStackTrace();
          }
        }
      });
      Collections.sort(tickerSymbolClosePrice, TotalReturnsDto.closePriceComparator);
      tickerSymbolClosePrice.stream().forEach(symbolClosePriceObject -> {
        tradeSymbols.add(symbolClosePriceObject.getSymbol());
      });
      return tradeSymbols;

    } catch (RuntimeException e) {
      e.printStackTrace();
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }

  }

  public static List<String> mainReadQuotes(String[] args) throws IOException, URISyntaxException {
    String fileName = args[0].toString();
    String endDate = args[1].toString();
    String filePath = resolveFileFromResources(fileName).toString();
    ObjectMapper objectMapper = getObjectMapper();
    try (FileInputStream fileInputStream = new FileInputStream(filePath)) {
      PortfolioTrade[] clientPortfolioTrades = objectMapper.readValue(
        fileInputStream,
        PortfolioTrade[].class
      );
      
      return getSortedTradeSymbols(clientPortfolioTrades, endDate);

    } catch (RuntimeException e) {
      e.printStackTrace();
      throw e;
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      throw e;
    } catch (IOException e) {
      e.printStackTrace();
      throw e;
    }

  }
  // TODO: CRIO_TASK_MODULE_CALCULATIONS
  //  Now that you have the list of PortfolioTrade and their data, calculate annualized returns
  //  for the stocks provided in the Json.
  //  Use the function you just wrote #calculateAnnualizedReturns.
  //  Return the list of AnnualizedReturns sorted by annualizedReturns in descending order.

  private static List<Double> getBuyAndSellPrices(PortfolioTrade portfolioTrade,
      String endDate) throws Exception {
        
    try {
      List<Double> buySellPrices =  new ArrayList<Double>();
      String token = "570e8e6635bd024a3c7e93ff30e1f563719a55eb";
      List<Candle> startToEndQuotes = getQuotes(portfolioTrade.getSymbol(),
          portfolioTrade.getPurchaseDate().toString(), endDate, token);
      
      if (startToEndQuotes.size() == 0) {
        throw new RuntimeException("No stock price between purchase date and sell date");
      } else {
        Double buyPrice = startToEndQuotes.get(0).getOpen();
        Double sellPrice = startToEndQuotes.get(startToEndQuotes.size() - 1).getClose();
        buySellPrices.add(buyPrice);
        buySellPrices.add(sellPrice);
        return buySellPrices;
      }

    } catch (Exception e) {
      e.printStackTrace();
      throw e;
    }

  }

  private static List<AnnualizedReturn> getAnnualizedReturns(
      PortfolioTrade[] clientPortfolioTrades, String endDate) {

    List<AnnualizedReturn> annualizedReturns = new ArrayList<AnnualizedReturn>();

    Arrays.stream(clientPortfolioTrades).forEach(clientPortfolioTrade -> {
      try {
        List<Double> buyAndSellPrices = getBuyAndSellPrices(clientPortfolioTrade, endDate);
        Double buyPrice = buyAndSellPrices.get(0);
        Double sellPrice = buyAndSellPrices.get(1);
        AnnualizedReturn annualizedReturn = calculateAnnualizedReturns(LocalDate.parse(endDate),
            clientPortfolioTrade, buyPrice, sellPrice);
        annualizedReturns.add(annualizedReturn);
      } catch (Exception e) {
        e.printStackTrace();
      }
    });

    if (annualizedReturns.size() < clientPortfolioTrades.length) {
      throw new RuntimeException("One of the Stock has no prices between purchase and sell dates");
    } else {
      Collections.sort(annualizedReturns, AnnualizedReturn.decAnnualizedReturnsComp);
      return annualizedReturns;
    }
        
  }
  
  public static List<AnnualizedReturn> mainCalculateSingleReturn(String[] args)
      throws IOException, URISyntaxException {
      
    String fileName = args[0].toString();
    String endDate = args[1].toString();
    String filePath = resolveFileFromResources(fileName).toString();
    ObjectMapper objectMapper = getObjectMapper();
    try (FileInputStream fileInputStream = new FileInputStream(filePath)) {
      
      PortfolioTrade[] clientPortfolioTrades = objectMapper.readValue(
        fileInputStream,
        PortfolioTrade[].class
      );
      
      return getAnnualizedReturns(clientPortfolioTrades, endDate);

    } catch (RuntimeException e) {
      e.printStackTrace();
      throw e;
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      throw e;
    } catch (IOException e) {
      e.printStackTrace();
      throw e;
    }

  }

  // TODO: CRIO_TASK_MODULE_CALCULATIONS
  //  Return the populated list of AnnualizedReturn for all stocks.
  //  Annualized returns should be calculated in two steps:
  //   1. Calculate totalReturn = (sell_value - buy_value) / buy_value.
  //      1.1 Store the same as totalReturns
  //   2. Calculate extrapolated annualized returns by scaling the same in years span.
  //      The formula is:
  //      annualized_returns = (1 + total_returns) ^ (1 / total_num_years) - 1
  //      2.1 Store the same as annualized_returns
  //  Test the same using below specified command. The build should be successful.
  //     ./gradlew test --tests PortfolioManagerApplicationTest.testCalculateAnnualizedReturn

  public static AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate,
      PortfolioTrade trade, Double buyPrice, Double sellPrice) {
        
    Double totalReturn  = (sellPrice - buyPrice) / buyPrice;
    LocalDate purchaseDate = trade.getPurchaseDate();
    Double durationYears = (double)purchaseDate.until(endDate, ChronoUnit.DAYS) / (double)(365.0);
    Double annualizedReturn = Math.pow(1 + totalReturn, 1 / durationYears) - 1;

    return new AnnualizedReturn(trade.getSymbol(), annualizedReturn, totalReturn);
  }


  // Useless Code above :)


  // TODO: CRIO_TASK_MODULE_REFACTOR
  //  Once you are done with the implementation inside PortfolioManagerImpl and
  //  PortfolioManagerFactory, create PortfolioManager using PortfolioManagerFactory.
  //  Refer to the code from previous modules to get the List<PortfolioTrades> and endDate, and
  //  call the newly implemented method in PortfolioManager to calculate the annualized returns.

  // Note:
  // Remember to confirm that you are getting same results for annualized returns as in Module 3.

  private static String readFileAsString(String file) throws URISyntaxException {
    return resolveFileFromResources(file).toString();
  }

  private static PortfolioTrade[] getPortfolioTrades(String content, ObjectMapper objectMapper)
      throws IOException {
    try (FileInputStream fileInputStream = new FileInputStream(content)) {
      return objectMapper.readValue(fileInputStream, PortfolioTrade[].class);
    } catch (IOException e) {
      e.printStackTrace();
      throw e;
    }
  }

  public static List<AnnualizedReturn> mainCalculateReturnsAfterRefactor(String[] args)
      throws Exception {
    String file = args[0];
    LocalDate endDate = LocalDate.parse(args[1]);
    String contents = readFileAsString(file);
    ObjectMapper objectMapper = getObjectMapper();
    RestTemplate restTemplate = new RestTemplate();
    String apiService = "tiingo";
    PortfolioManager portfolioManager = PortfolioManagerFactory.getPortfolioManager(
        apiService, restTemplate);
    List<PortfolioTrade> portfolioTrades = Arrays.asList(
        getPortfolioTrades(contents, objectMapper)
    );
    List<AnnualizedReturn> annualizedReturns = portfolioManager.calculateAnnualizedReturn(
        portfolioTrades, endDate);
    if (annualizedReturns.size() < portfolioTrades.size()) {
      throw new RuntimeException("One of the Stock has no prices between purchase and sell dates");
    } else {
      return annualizedReturns;
    }
  }

  public static void main(String[] args) throws Exception {
    Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
    ThreadContext.put("runId", UUID.randomUUID().toString());

    printJsonObject(mainCalculateReturnsAfterRefactor(args));
  }
}

