package com.vennetics.bell.sam.ss7.tcap.ati.enabler.dialogue.states;

import java.nio.ByteBuffer;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.ericsson.einss7.japi.OutOfServiceException;
import com.ericsson.einss7.japi.VendorException;
import com.ericsson.einss7.japi.WouldBlockException;
import com.ericsson.jain.protocol.ss7.tcap.Tools;
import com.vennetics.bell.sam.ss7.tcap.ati.enabler.map.SubscriberState;
import com.vennetics.bell.sam.ss7.tcap.ati.enabler.rest.OutboundATIMessage;
import com.vennetics.bell.sam.ss7.tcap.common.dialogue.IDialogue;
import com.vennetics.bell.sam.ss7.tcap.common.dialogue.IDialogueContext;
import com.vennetics.bell.sam.ss7.tcap.common.dialogue.states.AbstractDialogueState;
import com.vennetics.bell.sam.ss7.tcap.common.dialogue.states.IInitialDialogueState;
import com.vennetics.bell.sam.ss7.tcap.common.exceptions.BadProtocolException;
import com.vennetics.bell.sam.ss7.tcap.common.exceptions.UnexpectedResultException;
import com.vennetics.bell.sam.ss7.tcap.common.utils.EncodingHelper;
import com.vennetics.bell.sam.ss7.tcap.common.utils.TagLengthValue;

import jain.protocol.ss7.SS7Exception;
import jain.protocol.ss7.tcap.ComponentIndEvent;
import jain.protocol.ss7.tcap.DialogueIndEvent;
import jain.protocol.ss7.tcap.JainTcapProvider;
import jain.protocol.ss7.tcap.JainTcapStack;
import jain.protocol.ss7.tcap.component.InvokeReqEvent;
import jain.protocol.ss7.tcap.component.Parameters;
import jain.protocol.ss7.tcap.component.ResultIndEvent;
import jain.protocol.ss7.tcap.dialogue.BeginReqEvent;
import jain.protocol.ss7.tcap.dialogue.DialogueConstants;
import jain.protocol.ss7.tcap.dialogue.EndIndEvent;

@Component
public class AtiDialogueStart extends AbstractDialogueState implements IInitialDialogueState {

    private static final Logger logger = LoggerFactory.getLogger(AtiDialogueStart.class);

    private static final byte SUBSCRIBER_STATE_TAG = Tools.getLoByteOf2(0xA1);
    private static final byte LOCATION_INFO_TAG = Tools.getLoByteOf2(0xA0);

    private static final byte IDLE_TAG = Tools.getLoByteOf2(0x80);
    private static final byte BUSY_TAG = Tools.getLoByteOf2(0x81);
    private static final byte NOT_PROVIDED_TAG = Tools.getLoByteOf2(0x82);

    private static final byte GEO_INFO_TAG = Tools.getLoByteOf2(0x80);
    private static final byte CELL_LAI_TAG = Tools.getLoByteOf2(0x81);
    private static final byte VLR_NUMBER_TAG = Tools.getLoByteOf2(0x82);
    private static final byte LOCATION_NUMBER_TAG = Tools.getLoByteOf2(0x83);
    
    private static String stateName = "AtiDialogueStart";

    public AtiDialogueStart(final IDialogueContext context, final IDialogue dialogue) {
        super(context, dialogue);
        logger.debug("Changing state to {}", getStateName());
    }
    
    public AtiDialogueStart() {
        super();
        logger.debug("Changing state to {}", getStateName());
    }

    public void activate() {
        startDialogue();
    }

    @Override
    public void handleEvent(final ComponentIndEvent event) {
        logger.debug("ComponentIndEvent event received in state {}", getStateName());
        processComponentIndEvent(event);
    }

    @Override
    public void handleEvent(final DialogueIndEvent event) {
        logger.debug("DialogueIndEvent event received in state {}", getStateName());
        processDialogueIndEvent(event);
    }

    public void startDialogue() {
        InvokeReqEvent invokeReq = null;
        BeginReqEvent beginReq = null;
        int dialogueId = -1;
        try {
            JainTcapProvider provider = getContext().getProvider();
            dialogueId = provider.getNewDialogueId(getContext().getSsn());
            getDialogue().setDialogueId(dialogueId);
            int invokeId = provider.getNewInvokeId(dialogueId);
            logger.debug("Starting dialogue with dialogueId:{} and invokeId:{}",
                         dialogueId,
                         invokeId);
            invokeReq = getDialogue().getComponentRequestBuilder().createInvokeReq(getContext().getTcapEventListener(),
                                                                                   invokeId,
                                                                                   getDialogue().getRequest(),
                                                                                   true,
                                                                                   getContext().getConfigProperties(), dialogueId);
            provider.sendComponentReqEventNB(invokeReq);
            beginReq = getDialogue().getDialogueRequestBuilder().createBeginReq(getContext(), dialogueId);
            provider.sendDialogueReqEventNB(beginReq);
        } catch (SS7Exception ex) {
            ex.printStackTrace();
        } catch (WouldBlockException vbEx) {
            handleWouldBlock(invokeReq, vbEx);
        } catch (OutOfServiceException oosEx) {
            handleOutOfServiceException(invokeReq, oosEx);
        } catch (VendorException vEx) {
            vEx.printStackTrace();
        }
    }

    /**
     *
     * @param event
     *
     * @exception SS7Exception
     */
    @Override
    public void processResultIndEvent(final ResultIndEvent event) throws SS7Exception {
        logger.debug("ResultIndEvent event received in state {}", getStateName());
        OutboundATIMessage obm = (OutboundATIMessage) getDialogue().getRequest();
        if (event.isParametersPresent()) {
            Parameters params = event.getParameters();
            final byte[] returnedBytes = params.getParameter();
            logger.debug("Parameters = {}", EncodingHelper.bytesToHex(returnedBytes));
            processReturnedBytes(returnedBytes, obm);
            getDialogue().setResult(obm);
        } else {
            obm.setStatus(SubscriberState.UNKOWN);
            getDialogue().setResult(obm);
        }
        final JainTcapProvider provider = getContext().getProvider();
        final JainTcapStack stack = getContext().getStack();
        switch (stack.getProtocolVersion()) {
            case DialogueConstants.PROTOCOL_VERSION_ANSI_96:
                provider.releaseInvokeId(event.getLinkId(), event.getDialogueId());
                break;
            case DialogueConstants.PROTOCOL_VERSION_ITU_97:
                provider.releaseInvokeId(event.getInvokeId(), event.getDialogueId());
                break;
            default:
                throw new BadProtocolException("Wrong protocol version" + stack.getProtocolVersion());
        }
        logger.debug("Changing state from {}", getStateName());
        getDialogue().setState(new AtiDialogueEnd(getContext(), getDialogue()));
        getDialogue().activate();
    }

    private void processReturnedBytes(final byte[] returnedBytes, final OutboundATIMessage obm) {
        byte[] bytes;
        bytes = getValueFromSequence(returnedBytes);
        logger.debug("Values from sequence = {}", EncodingHelper.bytesToHex(bytes));
        bytes = getValueFromSequence(bytes);
        logger.debug("Values from next sequence = {}", EncodingHelper.bytesToHex(bytes));
        final List<TagLengthValue> tlvs = EncodingHelper.getTlvs(bytes);
        processSubscriberInfo(tlvs, obm);
    }
    
    private void processSubscriberInfo(final List<TagLengthValue> tlvs, final OutboundATIMessage obm) {
        for (TagLengthValue tlv: tlvs) {
            logger.debug("tag {}, length {}, Value {}", EncodingHelper.bytesToHex(tlv.getTag()),
                         EncodingHelper.bytesToHex(tlv.getLength()),
                         EncodingHelper.bytesToHex(tlv.getValue()));
            if (tlv.getTag() == SUBSCRIBER_STATE_TAG) {
                final List<TagLengthValue> ssTlv = EncodingHelper.getTlvs(tlv.getValue());
                if (ssTlv.size() == 1) {
                    processSubscriberState(ssTlv.get(0).getTag(), obm);
                } else {
                    obm.setStatus(SubscriberState.UNKOWN);
                }
            } else if (tlv.getTag() == LOCATION_INFO_TAG) {
                final List<TagLengthValue> locTlvs = EncodingHelper.getTlvs(tlv.getValue());
                for (final TagLengthValue locTlv: locTlvs) {
                    processLocationInfo(locTlv.getTag(), locTlv.getValue(), obm);
                }
            }
        }
    }

    private void processLocationInfo(final byte tag, final byte[] bs, final OutboundATIMessage obm) {
        if (tag == GEO_INFO_TAG) {
            ByteBuffer bb = ByteBuffer.wrap(bs);
            bb.get(); // Skip shape
            final byte[] latitude = new byte[3];
            bb.get(latitude, 0, 3);
            obm.setLatitude(latitude);
            logger.debug("latitude = {}", EncodingHelper.bytesToHex(latitude));
            final byte[] longitude = new byte[3];
            bb.get(longitude, 0, 3);
            obm.setLongitude(longitude);
            logger.debug("longitude = {}", EncodingHelper.bytesToHex(longitude));
            final byte uncertainty = bb.get();
            obm.setUncertainty(uncertainty);
            logger.debug("Uncertainty = {}", EncodingHelper.bytesToHex(uncertainty));
        } else if (tag == VLR_NUMBER_TAG) {
            logger.debug(EncodingHelper.bytesToHex(bs));
        } else if (tag == LOCATION_NUMBER_TAG) {
            logger.debug(EncodingHelper.bytesToHex(bs));
        } else if (tag == CELL_LAI_TAG) {
            logger.debug(EncodingHelper.bytesToHex(bs));
        } else if (tag == EncodingHelper.INTEGER_TAG) {
            Integer val = new Integer(0);
            for (int i = 0; i < bs.length; i++)  {
                val = val + ((bs[i] & 0xFF) << (bs.length - i - 1) * 8);
            }
            logger.debug("Age of geographical info  = {}", val);
            obm.setAge(val);
        } else {
            logger.error("Unknown tag");
        }
    }

    private void processSubscriberState(final byte octet, final OutboundATIMessage obm) {
        logger.debug("SubscriberState tag = {}", EncodingHelper.bytesToHex(octet));
        if (octet == IDLE_TAG) {
            obm.setStatus(SubscriberState.ASSUMED_IDLE);
        } else if (octet == BUSY_TAG) {
            obm.setStatus(SubscriberState.CAMEL_BUSY);
        } else if (octet == EncodingHelper.ENUMERATED_TAG) {
            obm.setStatus(SubscriberState.NET_DET_NOT_REACHEABLE);
        } else if (octet == NOT_PROVIDED_TAG) {
            obm.setStatus(SubscriberState.NOT_PROVIDED_VLR);
        } else {
            obm.setStatus(SubscriberState.UNKOWN);
        }
    }

    byte[] getValueFromSequence(final byte[] bytes) {
        final List<TagLengthValue> tlvs = EncodingHelper.getTlvs(bytes);
        if (!(tlvs.size() == 1 && tlvs.get(0).getTag() == EncodingHelper.SEQUENCE_TAG)) {
            logger.error("Expecting sequence");
            throw new UnexpectedResultException(EncodingHelper.bytesToHex(bytes));
        }
        return tlvs.get(0).getValue();
    }
    
    /**
     * Dialogue event.
     */
    public void processEndIndEvent(final EndIndEvent event) throws SS7Exception {
        logger.debug("Expected EndIndEvent received.");
    }

    public String getStateName() {
        return stateName;
    }
    
    public void terminate() {
        getDialogue().setState(new AtiDialogueEnd(getContext(), getDialogue()));
    }
}