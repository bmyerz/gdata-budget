package com.okbrandon.budget;

import com.google.gdata.client.authn.oauth.GoogleOAuthParameters;
import com.google.gdata.client.spreadsheet.FeedURLFactory;
import com.google.gdata.client.spreadsheet.SpreadsheetQuery;
import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.spreadsheet.*;
import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.NotImplementedException;
import com.google.gdata.util.ServiceException;
import org.apache.commons.cli.*;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;

public class BudgetApp {
  /**
   * Prints out the specified cell.
   *
   * @param cell the cell to print
   */
  public static void printCell(CellEntry cell) {
    String shortId = cell.getId().substring(cell.getId().lastIndexOf('/') + 1);
    System.out.println(" -- Cell(" + shortId + "/" + cell.getTitle().getPlainText()
            + ") formula(" + cell.getCell().getInputValue() + ") numeric("
            + cell.getCell().getNumericValue() + ") value("
            + cell.getCell().getValue() + ")");
  }

  public static void main(String[] args)
          throws AuthenticationException, MalformedURLException, IOException, ServiceException, ParseException {

    Options options = new Options();
    options.addOption(OptionBuilder.withArgName("file")
            .hasArg()
            .withDescription("path to oath json file")
            .create("oauth"));
    options.addOption(OptionBuilder.withArgName("file")
            .hasArg()
            .withDescription("path to password file")
            .create("passwd"));

    CommandLineParser parser = new BasicParser();
    CommandLine cmd = parser.parse( options, args);

    String[] args_parsed = cmd.getArgs();
    if (args_parsed.length < 1) {
      throw new IllegalArgumentException("missing name of spreadsheet");
    }
    String spreadsheetName = args_parsed[0];

    SpreadsheetService service = new SpreadsheetService("Budget");

    if (cmd.hasOption("oath")) {
      String oauthFileName = cmd.getOptionValue("oauth");
      JSONObject clientSecretJson = new JSONObject(new BufferedReader(new FileReader(oauthFileName)).readLine()).getJSONObject("installed");

      String CLIENT_ID = clientSecretJson.getString("client_id");
      String CLIENT_SECRET = clientSecretJson.getString("client_secret");
      String SCOPE = "https://spreadsheets.google.com/feeds https://docs.google.com/feeds";
      String REDIRECT_URI = clientSecretJson.getJSONArray("redirect_uris").getString(0);

      GoogleOAuthParameters parameters = new GoogleOAuthParameters();
      parameters.setOAuthToken(CLIENT_ID);
      parameters.setOAuthTokenSecret(CLIENT_SECRET);
      parameters.setScope(SCOPE);

      //parameters.RedirectUri = REDIRECT_URI;

      //service.setOAuthCredentials(parameters, null);
      throw new NotImplementedException("oauth");
    } else {
      String oauthFileName = cmd.getOptionValue("passwd");
      JSONObject clientSecretJson = new JSONObject(new BufferedReader(new FileReader(oauthFileName)).readLine());

      String PASSWORD = clientSecretJson.getString("password");
      String USERNAME = clientSecretJson.getString("username");

      service.setUserCredentials(USERNAME, PASSWORD);
    }



    FeedURLFactory factory = FeedURLFactory.getDefault();

    boolean getFirstGlobal = false;
    boolean testHHE = true;

    if (testHHE) {
      SpreadsheetFeed sfeed;

      if (getFirstGlobal) {
        sfeed = service.getFeed(factory.getSpreadsheetsFeedUrl(), SpreadsheetFeed.class);
      } else {
        SpreadsheetQuery q = new SpreadsheetQuery(factory.getSpreadsheetsFeedUrl());
        q.setTitleExact(true);
        q.setTitleQuery(spreadsheetName);
        sfeed = service.query(q, SpreadsheetFeed.class);
      }
      SpreadsheetEntry sentry = sfeed.getEntries().get(0);

//      System.out.println("Spreadsheet plaintext: " + sentry.getPlainTextContent());
      System.out.println("Spreadsheet data: " + sentry.getContent().getLang() + " " + sentry.getTitle() + " " + sentry.getKey() + " " + sentry.getId() + " " + sentry.getAuthors().get(0).getName());
      WorksheetFeed wfeed = service.getFeed(sentry.getWorksheetFeedUrl(), WorksheetFeed.class);
      WorksheetEntry wentry = wfeed.getEntries().get(0);
      CellFeed cfeed = service.getFeed(wentry.getCellFeedUrl(), CellFeed.class);
      for (int i=0; i<20; i++) {
        CellEntry centry = cfeed.getEntries().get(i);
        System.out.print("cell contents: "); printCell(centry);
      }

      if (false) {
        System.out.println("rows: " + cfeed.getRowCount());
        CellEntry modded = new CellEntry(441, 4, "=5+6");
        cfeed.insert(modded);
        System.out.println("wrote!!");
        System.out.println("rows: " + cfeed.getRowCount());
      }

      ListFeed lfeed = service.getFeed(wentry.getListFeedUrl(), ListFeed.class);
      ListEntry row = new ListEntry();
      row.getCustomElements().setValueLocal("Date", "11/21/2014");
      row.getCustomElements().setValueLocal("Item", "autogenerated!");
      row.getCustomElements().setValueLocal("Brandon", "0.01");

      System.out.println("lfeed results: " + lfeed.getTotalResults());
      lfeed.insert(row);
      System.out.println("inserted row");
      System.out.println("rows: " + cfeed.getRowCount());
      System.out.println("lfeed results: " + lfeed.getTotalResults());



    } else {
      SpreadsheetFeed sfeed = service.getFeed(factory.getSpreadsheetsFeedUrl(), SpreadsheetFeed.class);
      for (SpreadsheetEntry e : sfeed.getEntries()) {
        //System.out.println(e.getSpreadsheetLink().get);
        System.out.println(e.getWorksheetFeedUrl());
        WorksheetFeed wfeed = service.getFeed(e.getWorksheetFeedUrl(), WorksheetFeed.class);
        for (WorksheetEntry we : wfeed.getEntries()) {
          System.out.println(we.getRowCount());
        }
      }
    }

    //WorksheetFeed wfeed = service.getFeed(wsfeedurl, WorksheetFeed.class);
    //System.out.println(wfeed.getEntries().get(0).getTitle().getPlainText());

  }
}
