/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.azrul.langmera;

import io.vertx.core.Vertx;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 *
 * @author Azrul
 */
public interface Analytics {
    public void getDecision(DecisionRequest s,Vertx vertx, Consumer<DecisionResponse> responseAction);
    public void learn(DecisionFeedback currentFeedback,Vertx vertx, Runnable responseAction);
    
    
}
