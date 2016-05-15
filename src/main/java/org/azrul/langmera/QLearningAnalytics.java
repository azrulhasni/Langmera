/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.azrul.langmera;

import io.vertx.core.Vertx;
import io.vertx.core.shareddata.LocalMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import org.cfg4j.provider.ConfigurationProvider;

/**
 *
 * @author Azrul
 */
public class QLearningAnalytics implements Analytics {

    //private static final Integer epsilon = 4;
    private static Random random;
    //private static final Double configuredAlpha = null;
    private static Long startTime;
    //private static Integer maxHistoryRetained = 3000;
    private String chartDesc = "Langemera: Adaptive Real-time Analytical Framework";
    private static Map<String, List<Double>> traces = new HashMap<>();
    private ConfigurationProvider config = null;

    //private Logger logger =null;
    public QLearningAnalytics(Random random, ConfigurationProvider config) {
        QLearningAnalytics.random = random;
        startTime = (new Date()).getTime();
        this.config = config;
    }

    @Override
    public void getDecision(DecisionRequest s, Vertx vertx, Consumer<DecisionResponse> responseAction) {
        int decisionCount = vertx.sharedData().getLocalMap("DECISION_REQUEST").size();
        if (decisionCount % 10 <= config.getProperty("epsilon", Integer.class)) {
            getRandomDecision(s, vertx, responseAction);
        } else {
            getCalculatedDecision(s, vertx, responseAction);
        }
    }

    public void getCalculatedDecision(DecisionRequest req, Vertx vertx, Consumer<DecisionResponse> responseAction) {
        getDecisionPreCondition(req);
        LocalMap<String, Double> q = vertx.sharedData().getLocalMap("Q");
        String keyWithMaxVal = null;
        Double maxVal = Double.NEGATIVE_INFINITY;
        for (String k : q.keySet()) {
            if (q.get(k) > maxVal) {
                maxVal = q.get(k);
                keyWithMaxVal = k;
            }
        }
        DecisionResponse resp = null;
        if (keyWithMaxVal != null) {
            String decision = keyWithMaxVal.split(":")[1];
            resp = new DecisionResponse();
            resp.setDecisionId(req.getDecisionId());
            resp.setDecision(decision);
            //save cache to be matched to feedback
            if (req != null) {
                vertx.sharedData().getLocalMap("DECISION_REQUEST").put(req.getDecisionId(), req);
            }
            if (resp != null) {
                vertx.sharedData().getLocalMap("DECISION_RESPONSE").put(req.getDecisionId(), resp);
            }
            responseAction.accept(resp);
        } else {
            getRandomDecision(req, vertx, responseAction);
        }

    }

    public void getRandomDecision(DecisionRequest req, Vertx vertx, Consumer<DecisionResponse> responseAction) {
        getDecisionPreCondition(req);
        Integer r = random.nextInt(req.getOptions().length);
        DecisionResponse resp = new DecisionResponse();
        resp.setDecision(req.getOptions()[r]);
        resp.setDecisionId(req.getDecisionId());
        //save cache to be matched to feedback
        if (req != null) {
            vertx.sharedData().getLocalMap("DECISION_REQUEST").put(req.getDecisionId(), req);
        }
        if (resp != null) {
            vertx.sharedData().getLocalMap("DECISION_RESPONSE").put(req.getDecisionId(), resp);
        }
        responseAction.accept(resp);
    }

    private void getDecisionPreCondition(DecisionRequest s) throws RuntimeException {
        if (s == null) {
            throw new RuntimeException("Decision Request is null");
        }
        if (s.getOptions() == null) {
            throw new RuntimeException("Options array is null");
        }
        if (s.getOptions().length == 0) {
            throw new RuntimeException("Options array is empty");
        }
    }

    @Override
    public void learn(DecisionFeedback currentFeedback, Vertx vertx, Runnable responseAction) {

        LocalMap<String, DetailDecisionFeedback> decisionFeedbackMap = vertx.sharedData().getLocalMap("DECISION_FEEDBACK");
        LocalMap<String, DecisionRequest> decisionRequestMap = vertx.sharedData().getLocalMap("DECISION_REQUEST");
        LocalMap<String, DecisionResponse> decisionResponseMap = vertx.sharedData().getLocalMap("DECISION_RESPONSE");
        LocalMap<String, Double> q = vertx.sharedData().getLocalMap("Q");
        LocalMap<Long, String> trackers = vertx.sharedData().getLocalMap("FEEDBACK_TRACKER");

        int feedbackCount = decisionFeedbackMap.size();

        String context = decisionRequestMap.get(currentFeedback.getDecisionId()).getContext();
        String decision = decisionResponseMap.get(currentFeedback.getDecisionId()).getDecision();

        DetailDecisionFeedback detailFB = new DetailDecisionFeedback();
        detailFB.setFeedback(currentFeedback);
        detailFB.setContext(context);
        detailFB.setDecision(decision);
        decisionFeedbackMap.put(currentFeedback.getDecisionId(), detailFB);

        Long trackerKey = (new Date()).getTime();
        trackers.put(trackerKey, currentFeedback.getDecisionId());

        int feedbackCountByDecision = 0;
        List<Double> rewards = new ArrayList<>();
        for (DetailDecisionFeedback fb : decisionFeedbackMap.values()) {
            if (context.equals(decisionFeedbackMap.get(fb.getFeedback().getDecisionId()).getContext())
                    && decision.equals(decisionFeedbackMap.get(fb.getFeedback().getDecisionId()).getDecision())) {
                feedbackCountByDecision++;
                rewards.add(fb.getFeedback().getScore());
            }
        }

        Double w = 0.0;
        Double alpha = config.getProperty("alpha", Double.class);

        //if no step parameter is configured, calculate it
        if (alpha == null) {
            alpha = 1.0 / (feedbackCountByDecision);
        }

        //non-stationary q-learning
        int i = 0;
        for (Double ri : rewards) {
            i++;
            w = w + alpha * (Math.pow(1 - alpha, feedbackCountByDecision - i)) * ri;
        }
        Double newQ = w;

        //System.out.println(feedbackCount+" Q:["+context + ":" + decision+"]"+newQ);
        //save what we learn
        if (newQ.isInfinite() || newQ.isNaN()) {
            //skip
        } else {
            String key = context + ":" + decision;
            q.put(key, newQ);
        }

        //Limit the number of history taken into account - prevents memory leak
        if (feedbackCount > config.getProperty("maxHistoryRetained", Integer.class)) {
            Long tk = Collections.min(trackers.keySet());
            String decisionIDWithMinTracker = trackers.get(tk);
            decisionFeedbackMap.remove(decisionIDWithMinTracker);
            trackers.remove(tk);
        }

        //clear cached req/resp once the feedback has come back
        decisionRequestMap.remove(currentFeedback.getDecisionId());
        decisionResponseMap.remove(currentFeedback.getDecisionId());

        //keep traces
        if (Boolean.TRUE.equals(config.getProperty("collect.traces", Boolean.class))) {
            for (String contextDecision : q.keySet()) {
                List<Double> qtrace = traces.get(contextDecision);
                if (qtrace == null) {
                    qtrace = new ArrayList<Double>();
                    qtrace.add(q.get(contextDecision));
                    traces.put(contextDecision, qtrace);
                } else {
                    qtrace.add(q.get(contextDecision));
                }
            }
        }

        responseAction.run();
        if (Boolean.TRUE.equals(currentFeedback.getTerminal())) {
            long delta = (new Date()).getTime() - startTime;
            System.out.println("Time taken to process " + feedbackCount + " msgs:" + delta + " ms");
            System.out.println("Time taken per msg: " + (delta / feedbackCount) + " ms");
            System.out.println("Msgs per s: " + ((1000.0 * (double) feedbackCount) / ((double) delta)) + " msgs");
            if (Boolean.TRUE.equals(config.getProperty("collect.traces", Boolean.class))) {
                final LineChart demo = new LineChart(chartDesc, traces);
                demo.pack();
                demo.setVisible(true);
            }
        }

    }
}