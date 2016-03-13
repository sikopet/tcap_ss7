package com.vennetics.bell.sam.ss7.tcap.enabler.dialogue;

import java.util.concurrent.CountDownLatch;

import com.vennetics.bell.sam.ss7.tcap.enabler.component.requests.IComponentRequestBuilder;
import com.vennetics.bell.sam.ss7.tcap.enabler.dialogue.requests.IDialogueRequestBuilder;
import com.vennetics.bell.sam.ss7.tcap.enabler.dialogue.states.IDialogueState;
import com.vennetics.bell.sam.ss7.tcap.enabler.rest.OutboundATIMessage;

import jain.protocol.ss7.tcap.ComponentIndEvent;
import jain.protocol.ss7.tcap.DialogueIndEvent;
import jain.protocol.ss7.tcap.JainTcapProvider;

public interface IDialogue {
    
    int getDialogueId();

    JainTcapProvider getJainTcapProvider();

    IDialogueState getState();

    void setState(IDialogueState state);
    
    void setDialogueId(int dialogueId);
        
    void handleEvent(ComponentIndEvent event);

    void handleEvent(DialogueIndEvent event);
    
    void activate();
    
    String getStateName();
    
    Object getRequest();

    void setDialogueRequestBuilder(IDialogueRequestBuilder dialogueRequestBuilder);

    void setComponentRequestBuilder(IComponentRequestBuilder componentRequestBuilder);

    IDialogueRequestBuilder getDialogueRequestBuilder();

    IComponentRequestBuilder getComponentRequestBuilder();
    
    OutboundATIMessage getResult();
    
    void setResult(OutboundATIMessage result);

    void setLatch(CountDownLatch cDl);
    
    CountDownLatch getLatch();
}
