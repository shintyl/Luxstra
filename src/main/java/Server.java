import static spark.Spark.*;

import com.google.gson.Gson;
import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.android.PolyUtil;
import com.google.maps.model.LatLng;
import com.google.maps.model.TravelMode;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.cdimascio.dotenv.Dotenv;
import model.LightJSON;
import model.PathLatLngs;
import model.RouteDirections;
import model.Routes;
import org.sql2o.Sql2o;
import org.sql2o.Sql2oException;
import org.sql2o.quirks.PostgresQuirks;
import persistence.Sql2oLightDao;
import spark.Spark;
import spark.utils.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import static model.SqlSchema.*;

public class Server {

  private static Sql2o sql2o;

  private static GeoApiContext context;

  private static Sql2o getSql2o() {
    if(sql2o == null) {
      // create data source - update to use postgresql
      try {
        Properties props = getDbUrl(System.getenv("DATABASE_URL"));
        sql2o = new Sql2o(new HikariDataSource(new HikariConfig(props)), new PostgresQuirks());
        //sql2o = new Sql2o(props.getProperty("jdbcUrl"), props.getProperty("username"), props.getProperty("password"));
      } catch(URISyntaxException | Sql2oException e) {
        e.printStackTrace();
      }

      try (org.sql2o.Connection con = sql2o.beginTransaction()) {
        con.createQuery(LightsSchema).executeUpdate();
        con.commit();
      } catch (Sql2oException e) {
        e.printStackTrace();
      }
    }

    return sql2o;
  }

  private static Properties getDbUrl(String databaseUrl) throws URISyntaxException {
    Properties props = new Properties();
    if (databaseUrl == null) {
      Dotenv dotenv = Dotenv.load();
      props.setProperty("username", dotenv.get("DEV_DB_USER"));
      props.setProperty("password", dotenv.get("DEV_DB_PWORD"));
      props.setProperty("jdbcUrl",  dotenv.get("DEV_DB_URL"));
    } else {
      URI dbUri = new URI(databaseUrl);

      props.setProperty("username", dbUri.getUserInfo().split(":")[0]);
      props.setProperty("password", dbUri.getUserInfo().split(":")[1]);
      props.setProperty("jdbcUrl",  "jdbc:postgresql://" + dbUri.getHost() + ':'
              + dbUri.getPort() + dbUri.getPath() + "?sslmode=require");
    }

    return props;
  }

  final static int PORT_NUM = 7000;
  private static int getHerokuAssignedPort() {
    String herokuPort = System.getenv("PORT");
    if (herokuPort != null) {
      return Integer.parseInt(herokuPort);
    }
    return PORT_NUM;
  }

  public static GeoApiContext getGeoAPIContext() {
    if(context == null) {
      String apiKey = System.getenv("API_KEY");
      if(apiKey == null) {
        Dotenv dotenv = Dotenv.load();
        apiKey = dotenv.get("DEV_API_KEY");
      }
      context = new GeoApiContext.Builder()
              .apiKey(apiKey)
              .build();
    }
    return context;
  }

  public static void main(String[] args) {
    // set port number
    port(getHerokuAssignedPort());

    getSql2o();

    getGeoAPIContext();

    staticFiles.location("/");

    get("", (req, res) -> {
      res.status(200);
      res.type("text/html");

      return IOUtils.toString(Spark.class.getResourceAsStream("/index.html"));
    });

    get("/", (req, res) -> {
      res.status(200);
      res.type("text/html");

      return IOUtils.toString(Spark.class.getResourceAsStream("/index.html"));
    });

    notFound((req, res) -> {
      res.status(200);
      res.type("text/html");

      return IOUtils.toString(Spark.class.getResourceAsStream("/index.html"));
    });

    get("/api/lights_from_street", (req, res) -> {
      res.status(200);
      res.type("application/json");

      String name = req.queryParams("name");

      String results = new Gson().toJson(new Sql2oLightDao(getSql2o()).listFromStreetName(name));

      return results;
    });

    get("/api/fetch_route_coords", (req, res) -> {
      res.status(200);
      res.type("application/json");

      String start = req.queryParams("start");
      String end = req.queryParams("end");

      DirectionsResult directionsResult = DirectionsApi.getDirections(getGeoAPIContext(), start, end)
              .alternatives(true)
              .mode(TravelMode.WALKING)
              .await();
      DirectionsRoute[] routes = directionsResult.routes;

      System.out.println(routes.length);


      String latLngs = new Gson().toJson(new PathLatLngs(Arrays.stream(routes)
              .map(route -> Arrays.stream(route.legs)
                    .map(leg -> Arrays.stream(leg.steps)
                          .map(step -> step.polyline.decodePath())
                          .collect(Collectors.toList())
                    ).collect(Collectors.toList())
              ).collect(Collectors.toList())));

      return latLngs;
    });

    get("/api/fetch_route_directions", (req, res) -> {
      res.status(200);
      res.type("application/json");

      String start = req.queryParams("start");
      String end = req.queryParams("end");

      DirectionsResult directionsResult = DirectionsApi.getDirections(getGeoAPIContext(), start, end)
              .alternatives(true)
              .mode(TravelMode.WALKING)
              .await();
      DirectionsRoute[] routes = directionsResult.routes;

      System.out.println(routes.length);


      String directions = new Gson().toJson(new Routes(Arrays.stream(routes)
              .map(route -> new RouteDirections(
                      route.summary,
                      route.overviewPolyline.decodePath(),
                      Arrays.stream(route.legs)
                            .flatMap(leg -> Arrays.stream(leg.steps)
                                    .map(step -> step.htmlInstructions)
                            ).collect(Collectors.toList())
                )
              ).collect(Collectors.toList())
      ));

      return directions;
    });

//    try {
//      Reader reader = new BufferedReader(new InputStreamReader(Spark.class.getResourceAsStream("/out.json")));
//
//      LightJSON lightJSON = new Gson().fromJson(reader, LightJSON.class);
//
//      new Sql2oLightDao(getSql2o()).addBatch(lightJSON.getLightStream());
//    } catch(Exception e) {
//      e.printStackTrace();
//    }
  }

}
