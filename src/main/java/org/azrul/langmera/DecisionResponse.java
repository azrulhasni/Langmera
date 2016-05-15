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
public class DecisionResponse implements Shareable {
    private String decisionId;
    private String decision;
    
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
        final DecisionResponse other = (DecisionResponse) obj;
        if (!Objects.equals(this.decisionId, other.decisionId)) {
            return false;
        }
        return true;
    }

    public String getDecisionId() {
        return decisionId;
    }

    public void setDecisionId(String decisionId) {
        this.decisionId = decisionId;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }
}
