/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.azrul.langmera;

import java.util.List;

/**
 *
 * @author Azrul
 */
public class HistoryResponse {
    private List<HistoryResponseElement> elements;

    public List<HistoryResponseElement> getElements() {
        return elements;
    }

    public void setElements(List<HistoryResponseElement> elements) {
        this.elements = elements;
    }
}
