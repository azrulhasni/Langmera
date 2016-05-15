/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.azrul.langmera;

import io.vertx.core.shareddata.Shareable;

/**
 *
 * @author Azrul
 */
public class DetailDecisionFeedback implements Shareable {
    private DecisionFeedback feedback;
    private String context;
    private String decision;

    public DecisionFeedback getFeedback() {
        return feedback;
    }

    public void setFeedback(DecisionFeedback feedback) {
        this.feedback = feedback;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }
}
