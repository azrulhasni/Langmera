/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.azrul.langmera;

import java.io.Serializable;

/**
 *
 * @author Azrul
 */
public class RequestResponseWrapper implements Serializable{
    private DecisionRequest request;
    private DecisionResponse response;
    
    public RequestResponseWrapper(){}

    public RequestResponseWrapper(DecisionRequest request, DecisionResponse response) {
        this.request = request;
        this.response = response;
    }

    public DecisionRequest getRequest() {
        return request;
    }

    public void setRequest(DecisionRequest request) {
        this.request = request;
    }

    public DecisionResponse getResponse() {
        return response;
    }

    public void setResponse(DecisionResponse response) {
        this.response = response;
    }
}
