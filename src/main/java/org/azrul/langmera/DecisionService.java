/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.azrul.langmera;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Collectors;
import org.apache.commons.lang.SerializationUtils;
import org.cfg4j.provider.ConfigurationProvider;
import org.cfg4j.provider.ConfigurationProviderBuilder;
import org.cfg4j.source.ConfigurationSource;
import org.cfg4j.source.classpath.ClasspathConfigurationSource;
import org.cfg4j.source.context.environment.Environment;
import org.cfg4j.source.context.environment.ImmutableEnvironment;
import org.cfg4j.source.files.FilesConfigurationSource;
import org.cfg4j.source.context.filesprovider.ConfigFilesProvider;

/**
 *
 * @author Azrul
 */
public class DecisionService extends AbstractVerticle {

    private Analytics analytics = null;
    private ConfigurationProvider config = null;
    private Logger logger = null;

    public static void main(String[] args) throws IOException {
        ConfigurationProvider config = null;
        ConfigFilesProvider configFilesProvider = () -> Arrays.asList(Paths.get("config.properties"));
        if (args.length <= 0) {
            ConfigurationSource source = new ClasspathConfigurationSource(configFilesProvider);
            config = new ConfigurationProviderBuilder()
                    .withConfigurationSource(source)
                    .build();
        } else {
            ConfigurationSource source = new FilesConfigurationSource(configFilesProvider);
            Environment environment = new ImmutableEnvironment(args[0]);
            config = new ConfigurationProviderBuilder()
                    .withConfigurationSource(source)
                    .withEnvironment(environment)
                    .build();

        }
        Logger logger = null;
        if (config.getProperty("log.file", String.class).isEmpty() == false) {
            FileHandler logHandler = new FileHandler(config.getProperty("log.file", String.class),
                    config.getProperty("log.sizePerFile", Integer.class) * 1024 * 1024,
                    config.getProperty("log.maxFileCount", Integer.class), true);
            logHandler.setFormatter(new SimpleFormatter());
            logHandler.setLevel(Level.INFO);

            Logger rootLogger = Logger.getLogger("");
            rootLogger.removeHandler(rootLogger.getHandlers()[0]);
            logHandler.setLevel(Level.parse(config.getProperty("log.level", String.class)));
            rootLogger.setLevel(Level.parse(config.getProperty("log.level", String.class)));
            rootLogger.addHandler(logHandler);

            logger = rootLogger;
        } else {
            logger = Logger.getGlobal();
        }

        VertxOptions options = new VertxOptions();
        options.setMaxEventLoopExecuteTime(Long.MAX_VALUE);
        options.setWorkerPoolSize(config.getProperty("workerPoolSize", Integer.class));
        options.setEventLoopPoolSize(40);

        Vertx vertx = Vertx.vertx(options);
        vertx.deployVerticle(new DecisionService(logger, config));
        vertx.deployVerticle(new SaveToDB(logger, config));

    }

    public DecisionService(Logger logger, ConfigurationProvider config) throws IOException {
        this.logger = logger;
        this.analytics = new QLearningAnalytics(new Random(), logger, config);
        this.config = config;
    }

    @Override
    public void start(Future<Void> fut) {

        // Create a router object.
        Router router = Router.router(vertx);

        //enable CORS
        if (config.getProperty("enableCors", Boolean.class)) {
            String allowedAddress = config.getProperty("cors.allowedAddress", String.class);

            CorsHandler c = CorsHandler.create(allowedAddress);

            String allowedHeaders = config.getProperty("cors.allowedHeaders", String.class);
            if (allowedHeaders != null) {
                String[] allowedHeadersArray = allowedHeaders.split(",");
                c.allowedHeaders(new HashSet<String>(Arrays.asList(allowedHeadersArray)));
            }
            String allowedMethods = config.getProperty("cors.allowedMethods", String.class);
            if (allowedMethods != null) {
                String[] allowedMethodsArray = allowedMethods.split(",");
                for (String m : allowedMethodsArray) {
                    if ("POST".equals(m)) {
                        c.allowedMethod(HttpMethod.POST);
                    } else if ("PUT".equals(m)) {
                        c.allowedMethod(HttpMethod.PUT);
                    } else if ("GET".equals(m)) {
                        c.allowedMethod(HttpMethod.GET);
                    } else if ("DELETE".equals(m)) {
                        c.allowedMethod(HttpMethod.DELETE);
                    }
                }
            }

            router.route().handler(c);
        }
        //Handle body
        router.route().handler(BodyHandler.create());

        //router.route("/langmera/assets/*").handler(StaticHandler.create("assets"));
        router.post("/langmera/api/makeDecision").handler(this::makeDecision);
        router.post("/langmera/api/acceptFeedback").handler(this::acceptFeedback);
        //router.post("/langmera/api/getHistory").handler(this::getHistory);
        //router.post("/langmera/api/getRequestTemplate").handler(this::getRequestTemplate);
        //router.post("/langmera/api/getResponseTemplate").handler(this::getFeedbackTemplate);

        HttpServerOptions options = new HttpServerOptions();
        options.setReuseAddress(true);

        // Create the HTTP server and pass the "accept" method to the request handler.
        vertx
                .createHttpServer(options)
                .requestHandler(router::accept)
                .listen(
                        // Retrieve the port from the configuration,
                        // default to 8080.
                        config().getInteger("http.port", config.getProperty("http.port", Integer.class)),
                        result -> {
                            if (result.succeeded()) {
                                fut.complete();
                            } else {
                                fut.fail(result.cause());
                            }
                        }
                );
    }

    private void getHistory(RoutingContext routingContext) {
        final HistoryRequest req = Json.decodeValue(routingContext.getBodyAsString(),
                HistoryRequest.class);
        List<HistoryResponseElement> histResponses = new ArrayList<>();
        String driver = config.getProperty("jdbc.driver", String.class);
        String url = config.getProperty("jdbc.url", String.class);
        String username = config.getProperty("jdbc.username", String.class);
        String password = config.getProperty("jdbc.password", String.class);

        JDBCClient client = JDBCClient.createShared(vertx, new JsonObject()
                .put("url", url)
                .put("driver_class", driver)
                .put("user", username)
                .put("password", password));
        JsonArray param = new JsonArray();
        param.add(req.getContext()).
                add(req.getFrom()).
                add(req.getTo());
//        for (int i = 0; i < InMemoryDB.store.get(0).size(); i++) {
//            HistoryResponseElement resp = new HistoryResponseElement();
//            String context = (String) InMemoryDB.store.get(0).get(i);
//            if (context.equals(req.getContext())) {
//                Date time = (Date) InMemoryDB.store.get(3).get(i);
//                if (time.after(req.getFrom()) && time.before(req.getTo())) {
//                    resp.setContext(context);
//                    resp.setDecision((String) InMemoryDB.store.get(1).get(i));
//                    resp.setInterest((Double) InMemoryDB.store.get(2).get(i));
//                    resp.setTime(time);
//                    histResponses.add(resp);
//                }
//            }
//        }

        client.getConnection(ar -> {
            SQLConnection connection = ar.result();
            connection.queryWithParams("SELECT * FROM Trace ORDER BY decisiontime desc where context=? and decisiontime between ? and ?",
                    param,
                    r -> {
                        if (r.succeeded()) {
                            for (JsonArray row : r.result().getResults()) {
                                HistoryResponseElement respElement = new HistoryResponseElement();
                                respElement.setContext(row.getString(1));
                                respElement.setDecision(row.getString(6));
                                respElement.setInterest(row.getDouble(8));
                                respElement.setTime(new Date(row.getLong(5)));
                                histResponses.add(respElement);
                            }
                            HistoryResponse resp = new HistoryResponse();
                            resp.setElements(histResponses);

                            routingContext.response()
                            .setStatusCode(201)
                            .putHeader("content-type", "application/json; charset=utf-8")
                            .exceptionHandler(ex -> {
                                logger.log(Level.SEVERE, "Problem encountered when making decision", ex);
                            })
                            .end(Json.encodePrettily(resp));
                        }

                        connection.close();
                    });

        });

    }

    private void getRequestTemplate(RoutingContext routingContext) {
        DecisionRequest req = new DecisionRequest();
        req.setDecisionId("ID1234");
        req.setContext("age<18 AND 5000<salary<10000");
        req.setOptions(new String[]{"MOVIE", "CONCENRT", "SPORTS"});

        routingContext.response()
                .setStatusCode(201)
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(req));
    }

    private void getFeedbackTemplate(RoutingContext routingContext) {
        DecisionFeedback fb = new DecisionFeedback();
        fb.setDecisionId("ID1234");
        fb.setScore(-1.0);

        routingContext.response()
                .setStatusCode(201)
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(fb));
    }

    private void makeDecision(RoutingContext routingContext) {

        final DecisionRequest req = Json.decodeValue(routingContext.getBodyAsString(),
                DecisionRequest.class);
        analytics.getDecision(req, vertx, resp -> {

            routingContext.response()
                    .setStatusCode(201)
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .exceptionHandler(ex -> {
                        logger.log(Level.SEVERE, "Problem encountered when making decision", ex);
                    })
                    .end(Json.encodePrettily(resp));
//            RequestResponseWrapper wrapper = new RequestResponseWrapper(req, resp);
//            EventBus eb = vertx.eventBus();
//            eb.send("SAVE_DECISION_TO_DB", SerializationUtils.serialize(wrapper));
        });

    }

    private void acceptFeedback(RoutingContext routingContext) {

        final DecisionFeedback feedback = Json.decodeValue(routingContext.getBodyAsString(),
                DecisionFeedback.class);

        analytics.learn(feedback, vertx, () -> {

            routingContext.response()
                    .setStatusCode(201)
                    .exceptionHandler(ex -> {
                        logger.log(Level.SEVERE, "Problem encountered when accepting feedback", ex);
                    })
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end();
//            EventBus eb = vertx.eventBus();
//            eb.send("SAVE_FEEDBACK_TO_DB", SerializationUtils.serialize(feedback));
        });

    }
}
