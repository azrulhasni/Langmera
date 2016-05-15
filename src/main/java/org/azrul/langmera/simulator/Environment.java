/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.azrul.langmera.simulator;

import com.fasterxml.uuid.Generators;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.Json;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import org.apache.commons.lang.SerializationUtils;
import org.azrul.langmera.DecisionFeedback;
import org.azrul.langmera.DecisionRequest;
import org.azrul.langmera.DecisionResponse;

/**
 *
 * @author Azrul
 */
public class Environment extends AbstractVerticle {

    private GaussRewardFunction<String> simulator = null;
    private static final Integer maxMsg = 3001;

    public static void main(String[] args) throws ClassNotFoundException, InterruptedException {
        VertxOptions options = new VertxOptions();
        options.setMaxEventLoopExecuteTime(Long.MAX_VALUE);
        options.setWorkerPoolSize(10);
        Vertx vertx = Vertx.vertx(options);
        vertx.deployVerticle(new Environment());

        //for (Integer i = 0; i < maxMsg; i++) {
        Integer i = 0;
        while(true){
            i++;
            Thread.sleep(1000);
            vertx.eventBus().send("COUNTER", i);
        }
    }

    @Override
    public void start(Future<Void> fut) throws InterruptedException {
        vertx.eventBus().consumer("COUNTER", msg -> {
            simulator = new GaussRewardFunction<String>(new GaussParam<String>("CONCERTS", 1.0, 3.5),
                    new GaussParam<String>("MOVIES", 0.0, 3.5),
                    new GaussParam<String>("SPORTS", -1.0, 3.5));
            HttpClientOptions options = new HttpClientOptions();
            options.setReuseAddress(true);
            HttpClient httpClient = vertx.createHttpClient(options);
            
            final int it = (Integer) msg.body();

            UUID uuid = Generators.timeBasedGenerator().generate();
            DecisionRequest req = new DecisionRequest();
            req.setContext("age<18 AND 5000<salary<10000");
            req.setDecisionId(uuid.toString());
            req.setOptions(new String[]{"MOVIES", "SPORTS", "CONCERTS"});
            String reqBuffer = Json.encode(req);

            HttpClientRequest httpClientReq = httpClient.postAbs("http://localhost:8080/langmera/api/makeDecision", res -> {
                res.bodyHandler(buffer -> {

//                    if (it == Math.floorDiv(maxMsg, 3)) {
//
//                        simulator = new GaussRewardFunction<String>(new GaussParam<String>("CONCERTS", -1.0, 3.5),
//                                new GaussParam<String>("MOVIES", 0.0, 3.5),
//                                new GaussParam<String>("SPORTS", 1.0, 3.5));
//
//                    }
//
//                    if (it == 2 * Math.floorDiv(maxMsg, 3)) {
//
//                        simulator = new GaussRewardFunction<String>(new GaussParam<String>("CONCERTS", 1.0, 3.5),
//                                new GaussParam<String>("MOVIES", 1.0, 3.5),
//                                new GaussParam<String>("SPORTS", -1.0, 3.5));
//                    }
                    DecisionResponse response = Json.decodeValue(buffer.getString(0, buffer.length()), DecisionResponse.class);
                    System.out.println(it+" ---Response:" + response.getDecisionId() + "::::" + response.getDecision());

                    Double reward = simulator.reward(response.getDecision());
                    //System.out.println(reward);
                    DecisionFeedback feedback = new DecisionFeedback();
                    feedback.setDecisionId(response.getDecisionId());
                    feedback.setScore(reward);
                    //System.out.println("Counter:"+it);
//                    if (it == (maxMsg - 1)) {
//                        feedback.setTerminal(Boolean.TRUE);
//                        System.out.println("Send termination");
//
//                    }
                    String reqBuffer2 = Json.encode(feedback);
                    HttpClientRequest httpClientReq2 = httpClient.postAbs("http://localhost:8080/langmera/api/acceptFeedback", res2 -> {
                        //no response needed
                        res2.exceptionHandler(ex -> {
                            ex.printStackTrace();
                            
                        });

                    });
                    httpClientReq2.putHeader("content-length", Integer.toString(reqBuffer2.length()))
                            .putHeader("content-type", "application/json; charset=utf-8")
                            //.setTimeout(Long.MAX_VALUE)
                            .write(reqBuffer2)
                            .exceptionHandler(ex -> {
                                ex.printStackTrace();
                                httpClient.close();
                            })
                            .endHandler(res3 -> {
                                if (it == (maxMsg - 1)) {
                                    httpClient.close();
                                }
                            })
                            .end();

                });
                res.exceptionHandler(ex -> {
                    ex.printStackTrace();
                });
            });
            httpClientReq.putHeader("content-length", Integer.toString(reqBuffer.length()))
                    .putHeader("content-type", "application/json; charset=utf-8")
                    //.setTimeout(Long.MAX_VALUE)
                    .write(reqBuffer)
                    .exceptionHandler(ex -> {
                        ex.printStackTrace();
                    })
                    .end();

        });

    }

}
