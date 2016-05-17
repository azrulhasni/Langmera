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
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Random;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
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

        VertxOptions options = new VertxOptions();
        options.setMaxEventLoopExecuteTime(Long.MAX_VALUE);
        options.setWorkerPoolSize(config.getProperty("workerPoolSize", Integer.class));
        options.setEventLoopPoolSize(40);

        Vertx vertx = Vertx.vertx(options);
        vertx.deployVerticle(new DecisionService(config));
    }

    public DecisionService(ConfigurationProvider config) throws IOException {
       

        FileHandler logHandler = new FileHandler(config.getProperty("log.file", String.class),
                config.getProperty("log.sizePerFile", Integer.class) * 1024 * 1024,
                config.getProperty("log.maxFileCount", Integer.class), true);
        logHandler.setFormatter(new SimpleFormatter());
        logHandler.setLevel(Level.INFO);
        
        Logger rootLogger = Logger.getLogger("");
        rootLogger.removeHandler(rootLogger.getHandlers()[0]);
        logHandler.setLevel(Level.parse(config.getProperty("log.level",String.class)));
        rootLogger.setLevel(Level.parse(config.getProperty("log.level",String.class)));
        rootLogger.addHandler(logHandler);
        logger = rootLogger;
        this.analytics = new QLearningAnalytics(new Random(), logger,config);
        this.config = config;
    }

    @Override
    public void start(Future<Void> fut) {

        // Create a router object.
        Router router = Router.router(vertx);

        router.route().handler(BodyHandler.create());

        //router.route("/langmera/assets/*").handler(StaticHandler.create("assets"));
        router.post("/langmera/api/makeDecision").handler(this::makeDecision);
        router.post("/langmera/api/acceptFeedback").handler(this::acceptFeedback);
        router.post("/langmera/api/getRequestTemplate").handler(this::getRequestTemplate);
        router.post("/langmera/api/getResponseTemplate").handler(this::getFeedbackTemplate);

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
        });

    }
}
