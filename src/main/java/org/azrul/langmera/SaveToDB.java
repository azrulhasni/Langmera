/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.azrul.langmera;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.SerializationUtils;
import org.cfg4j.provider.ConfigurationProvider;
import org.postgresql.Driver;

/**
 *
 * @author Azrul
 */
public class SaveToDB extends AbstractVerticle {

    private List<Date> allDates = new ArrayList<>();
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    private Logger logger;
    private QLearningAnalytics analytics;
    private ConfigurationProvider config;

    public SaveToDB(Logger logger, ConfigurationProvider config) throws IOException {
        this.logger = logger;
        this.config = config;
    }

    @Override
    public void start(Future<Void> fut) {

        HttpServerOptions options = new HttpServerOptions();
        options.setReuseAddress(true);

        JDBCClient client = JDBCClient.createShared(vertx, new JsonObject()
                .put("url", "jdbc:postgresql://localhost:5432/langmeradb")
                .put("driver_class", "org.postgresql.Driver")
                .put("max_pool_size", 20)
                .put("user", "Langmera")
                .put("password", "1qazZAQ!"));

        vertx.eventBus().<byte[]>consumer("SAVE_TRACE_TO_TRACE", message -> {

            Trace trace = (Trace) SerializationUtils.deserialize(message.body());
            client.getConnection(res -> {
                if (!res.succeeded()) {
                     logger.log(Level.SEVERE, "Problem encountered getting DB connection. Please check DB credentials", res.cause());
                } else {
                    SQLConnection connection = res.result();
//                    String sql = "insert into Trace(context,qvalue,decisionid, decisiontime,decision,score) values(?,?,?,?,?,?)";
//                    JsonArray input = new JsonArray().
//                            add(trace.getContext()).
//                            add(trace.getQvalue()).
//                            add(trace.getDecisionId()).
//                            add(trace.getTimeStamp()).
//                            add(trace.getOption()).
//                            add(trace.getScore());
                    
                     String sql = "insert into Trace(context,qvalue,decisionid, decisiontime,decision,score,maxQ) values('"+trace.getContext()+"',"+trace.getQvalue()+",'"+trace.getDecisionId()+"','"+trace.getTimeStamp()+"','"+trace.getOption()+"',"+trace.getScore()+","+trace.getMaxQ()+")";
                   
                     //System.out.println("SQL:"+sql);
                   
                    connection.execute(sql,  res2 -> {
                       if (res2.failed()){
                          logger.log(Level.SEVERE, "Problem encountered when saving to DB", res2.cause());
                       }
                       connection.close();
                    });
                      
                      
                }
            });
        });

    }

}

//    Trace trace = (Trace) SerializationUtils.deserialize(e.body());
//                JDBCClient client = JDBCClient.createShared(vertx, new JsonObject()
//                        .put("url", "jdbc:postgresql://localhost:5432/")
//                        .put("driver_class", "org.postgresql.Driver")
//                        .put("max_pool_size", 10)
//                        .put("user", "Langmera")
//                        .put("password", "1qazZAQ!"));
//
//                client.getConnection(res -> {
//                    if (!res.succeeded()) {
//
//                    }
//                    SQLConnection connection = res.result();
//                    String sql = "insert into Trace(context,qvalue,decisionid, decisiontime,decision,score) values(?,?,?,?,?)";
//                    JsonArray input = new JsonArray().
//                            add(trace.getContext()).
//                            add(trace.getQvalue()).
//                            add(trace.getTimeStamp()).
//                            add(trace.getDecisionId()).
//                            add(trace.getScore());
//
//                    connection.updateWithParams(sql, input, res2 -> {
//                        if (!res2.succeeded()) {
//
//                            return;
//                        }
//
//                    }).close();
//                });
//
//    public void saveTrace(RoutingContext routingContext) {
//        TxResponse txresp = new TxResponse();
//        txresp.setStart(new Date());
//        try {
//            Class.forName("org.postgresql.Driver");
//        } catch (ClassNotFoundException ex) {
//            Logger.getLogger(SaveToDB.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        final TxRequest txReq = Json.decodeValue(routingContext.getBodyAsString(),
//                TxRequest.class);
//        JDBCClient client = JDBCClient.createShared(vertx, new JsonObject()
//                .put("url", "jdbc:postgresql://localhost:5432/")
//                .put("driver_class", "org.postgresql.Driver")
//                .put("max_pool_size", 10)
//                .put("user", "Langmera")
//                .put("password", "1qazZAQ!")
//        );
//
//        client.getConnection(res -> {
//            if (!res.succeeded()) {
//                System.err.println(res.cause());
//                JsonObject obj = new JsonObject();
//                obj.put("error", res.cause().getMessage());
//                routingContext.response().setStatusCode(500)
//                        .putHeader("content-type", "application/json; charset=utf-8").end(Json.encodePrettily(obj));
//                return;
//            }
//            SQLConnection connection = res.result();
//            String query = "SELECT * FROM transaction WHERE jdate='" + sdf.format(txReq.getFromDate()) + "' LIMIT 1000";
//            connection.query(query, res2 -> {
//                if (!res2.succeeded()) {
//                    System.err.println(res2.cause());
//                    JsonObject obj = new JsonObject();
//                    obj.put("error", res2.cause());
//                    routingContext.response().setStatusCode(500)
//                            .putHeader("content-type", "application/json; charset=utf-8")
//                            .end(Json.encodePrettily(obj));
//                    return;
//                }
//                ResultSet rs = res2.result();
//                List<Transaction> txs = new ArrayList<>();
//
//                for (JsonArray row : rs.getResults()) {
//                    try {
//                        Transaction tx = new Transaction();
//                        tx.setId(row.getInteger(0));
//                        tx.setJident(row.getString(1));
//                        tx.setJacct(row.getLong(2));
//                        tx.setJoldbal(row.getDouble(3));
//                        tx.setJfloat(row.getString(4));
//                        tx.setJabtelor(row.getInteger(5));
//                        tx.setjDateor(ISO8601DateParser.parse(row.getString(6)));
//                        tx.setJtamt(row.getDouble(7));
//                        tx.setJcode(row.getString(8));
//                        tx.setJterm(row.getString(9));
//                        tx.setJabtel(row.getInteger(10));
//                        tx.setJnum(row.getLong(11));
//                        tx.setjDate(ISO8601DateParser.parse(row.getString(12)));
//                        //tx.setJtime(ISO8601DateParser.parse(row.getString(13)));
//                        tx.setJabtel1(row.getInteger(14));
//                        tx.setJlastj(row.getString(15));
//                        tx.setJlastt(row.getString(16));
//                        tx.setJlate(row.getString(17));
//                        tx.setJcid(row.getString(18));
//                        tx.setJmne(row.getString(19));
//                        tx.setJledbal(row.getDouble(20));
//                        tx.setJtrnbr(row.getInteger(21));
//                        tx.setJclrcode(row.getString(22));
//                        tx.setJscentre(row.getString(23));
//                        tx.setJfree1(row.getString(24));
//                        tx.setJtermch(row.getString(25));
//                        tx.setJcodech(row.getString(26));
//                        tx.setJdvtyp(row.getString(27));
//                        tx.setJcardno(row.getString(28));
//                        tx.setJaccsta(row.getString(29));
//                        tx.setJtxacct(row.getLong(30));
//                        tx.setJglinfo(row.getString(31));
//                        tx.setJatmloc(row.getString(32));
//                        tx.setJatmfc(row.getString(33));
//                        tx.setJtamtb(row.getDouble(34));
//                        tx.setJtamtc(row.getDouble(35));
//                        tx.setJtamtd(row.getDouble(36));
//                        tx.setJxrate(row.getDouble(37));
//                        tx.setJfrcode(row.getString(38));
//                        tx.setJframt(row.getDouble(39));
//                        tx.setJdesc(row.getString(40));
//                        tx.setJbop(row.getString(41));
//                        tx.setJtermloc(row.getString(42));
//                        tx.setJabtelc(row.getInteger(43));
//                        tx.setJabtel1c(row.getInteger(44));
//                        tx.setJttinadr(row.getString(45));
//                        tx.setJttouadr(row.getString(46));
//                        tx.setJtaskno(row.getString(47));
//                        tx.setJbrcode(row.getString(48));
//                        tx.setJseqno(row.getString(49));
//                        tx.setJtcdesc(row.getString(50));
//                        tx.setJdcdevic(row.getString(51));
//                        tx.setJesaind(row.getString(52));
//                        tx.setJfxDate(row.getString(53));
//                        tx.setJbatind(row.getString(54));
//                        tx.setJpurcod(row.getString(55));
//                        tx.setJreason(row.getString(56));
//                        tx.setJframt1(row.getInteger(57));
//                        tx.setJtrnfind(row.getString(58));
//                        tx.setJpymtdtl(row.getString(59));
//                        tx.setJsdrname(row.getString(60));
//                        tx.setJcustref(row.getString(61));
//                        tx.setJgstype(row.getString(62));
//                        tx.setJgstamt(row.getDouble(63));
//                        tx.setJtamte(row.getDouble(64));
//                        tx.setJtamtf(row.getDouble(65));
//                        tx.setJtamtg(row.getDouble(66));
//                        tx.setJtamth(row.getDouble(67));
//                        tx.setJ24post(row.getString(68));
//                        txs.add(tx);
//                    } catch (ParseException ex) {
//                        Logger.getLogger(SaveToDB.class.getName()).log(Level.SEVERE, null, ex);
//                    }
//                }
//                txresp.setTransactions(txs);
//                txresp.setEnd(new Date());
//                routingContext
//                        .response()
//                        .putHeader("content-type", "application/json; charset=utf-8")
//                        .end(Json.encodePrettily(txresp));
//            }).close();
//        });
//
//    }

