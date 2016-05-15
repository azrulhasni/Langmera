/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.azrul.langmera;

import io.vertx.core.shareddata.Shareable;
import java.util.Objects;

/**
 *
 * @author Azrul
 */
public class DecisionFeedback implements Shareable {
    private String decisionId;
    private Double score;
    private Boolean terminal;

    public String getDecisionId() {
        return decisionId;
    }

    public void setDecisionId(String decisionId) {
        this.decisionId = decisionId;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 53 * hash + Objects.hashCode(this.decisionId);
        return hash;
    }

  

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DecisionFeedback other = (DecisionFeedback) obj;
        if (!Objects.equals(this.decisionId, other.decisionId)) {
            return false;
        }
        return true;
    }
    
      @Override
    public String toString() {
        return "DecisionFeedback{" + "decisionId=" + decisionId + ", score=" + score + '}';
    }

    public Boolean getTerminal() {
        return terminal;
    }

    public void setTerminal(Boolean terminal) {
        this.terminal = terminal;
    }
}
