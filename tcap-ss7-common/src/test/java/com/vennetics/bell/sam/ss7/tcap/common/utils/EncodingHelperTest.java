package com.vennetics.bell.sam.ss7.tcap.common.utils;

import java.nio.ByteBuffer;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import ericsson.ein.ss7.commonparts.util.Tools;

@RunWith(MockitoJUnitRunner.class)
public class EncodingHelperTest {
    
    private static final byte[] TEST_ONE_SEQUENCE = { 0x30, 0x05, 0x01, 0x02, 0x03, 0x04, 0x05 };
    private static final byte TEST_ONE_SEQUENCE_TAG = 0x30;
    private static final byte[] TEST_ONE_SEQUENCE_LEN = { 0x05 };
    private static final byte[] TEST_ONE_SEQUENCE_VAL = { 0x01, 0x02, 0x03, 0x04, 0x05 };
    
    
    private static final byte[] TEST_TWO_SEQUENCE = { 0x30, 0x0C, Tools.getLoByteOf2(0x81), 0x03, 0x03, 0x04, 0x05,
                                                      Tools.getLoByteOf2(0x82), 0x05, 0x03, 0x04, 0x05, 0x06, 0x07};
    private static final byte[] TEST_TWO_SEQUENCE_LEN = { 0x0C };
    private static final byte[] TEST_TWO_SEQUENCE_VAL = { Tools.getLoByteOf2(0x81), 0x03, 0x03, 0x04, 0x05,
                                                          Tools.getLoByteOf2(0x82), 0x05, 0x03, 0x04, 0x05, 0x06, 0x07 };
    private static final byte TEST_TWO_SEQUENCE_EL1_TAG = Tools.getLoByteOf2(0x81);
    private static final byte[] TEST_TWO_SEQUENCE_EL1_LEN = { 0x03 };
    private static final byte[] TEST_TWO_SEQUENCE_EL1_VAL = { 0x03, 0x04, 0x05 };
    private static final byte TEST_TWO_SEQUENCE_EL2_TAG = Tools.getLoByteOf2(0x82);
    private static final byte[] TEST_TWO_SEQUENCE_EL2_LEN = { 0x05 };
    private static final byte[] TEST_TWO_SEQUENCE_EL2_VAL = { 0x03, 0x04, 0x05, 0x06, 0x07 };

    private static final byte[] LENGTH_127 = { 0x7F };
    private static final byte[] LENGTH_255 = { Tools.getLoByteOf2(0x81), Tools.getLoByteOf2(0xFF) };
    private static final byte[] LENGTH_256 = { Tools.getLoByteOf2(0x82), 0x01, Tools.getLoByteOf2(0x0) };
    
    private static final byte[] NULL_ELEMENT = { Tools.getLoByteOf2(0x80), 0x00 };

    private static final String ADDRESS_EVEN = "12345678";
    private static final byte[] ADDRESS_EVEN_BYTES = {Tools.getLoByteOf2(0x80), 0x04, 0x21, 0x43, 0x65, Tools.getLoByteOf2(0x87)};
    private static final String ADDRESS_ODD = "1234567";
    private static final byte[] ADDRESS_ODD_BYTES = {Tools.getLoByteOf2(0x80), 0x04, 0x21, 0x43, 0x65, Tools.getLoByteOf2(0xF7)};
    @Test
    public void testDecodeOfSingleSequence() {
        List<TagLengthValue> tlvs = EncodingHelper.getTlvs(TEST_ONE_SEQUENCE);
        assertTrue(tlvs.size() == 1);
        assertEquals(tlvs.get(0).getTag(), TEST_ONE_SEQUENCE_TAG);
        assertArrayEquals(tlvs.get(0).getLength(), TEST_ONE_SEQUENCE_LEN);
        assertArrayEquals(tlvs.get(0).getValue(), TEST_ONE_SEQUENCE_VAL);
    }

    @Test
    public void testDecodeOfSingleSequenceWithMultipleElements() {
        List<TagLengthValue> tlvs = EncodingHelper.getTlvs(TEST_TWO_SEQUENCE);
        assertTrue(tlvs.size() == 1);
        assertEquals(tlvs.get(0).getTag(), TEST_ONE_SEQUENCE_TAG);
        assertArrayEquals(tlvs.get(0).getLength(), TEST_TWO_SEQUENCE_LEN);
        assertArrayEquals(tlvs.get(0).getValue(), TEST_TWO_SEQUENCE_VAL);
        List<TagLengthValue> tlvs2 = EncodingHelper.getTlvs(tlvs.get(0).getValue());
        assertTrue(tlvs2.size() == 2);
        assertEquals(Tools.getLoByteOf2(tlvs2.get(0).getTag()), TEST_TWO_SEQUENCE_EL1_TAG);
        assertEquals(Tools.getLoByteOf2(tlvs2.get(1).getTag()), TEST_TWO_SEQUENCE_EL2_TAG);
        assertArrayEquals(tlvs2.get(0).getLength(), TEST_TWO_SEQUENCE_EL1_LEN);
        assertArrayEquals(tlvs2.get(1).getLength(), TEST_TWO_SEQUENCE_EL2_LEN);
        assertArrayEquals(tlvs2.get(0).getValue(), TEST_TWO_SEQUENCE_EL1_VAL);
        assertArrayEquals(tlvs2.get(1).getValue(), TEST_TWO_SEQUENCE_EL2_VAL);
    }
    
    @Test
    public void testDecodeWithSequenceLengthLessThan128() {
        final byte[] bytes = generateSequenceWithLongLengthTag(127);
        List<TagLengthValue> tlvs = EncodingHelper.getTlvs(bytes);
        assertTrue(tlvs.size() == 1);
        assertEquals(tlvs.get(0).getTag(), TEST_ONE_SEQUENCE_TAG);
        assertArrayEquals(tlvs.get(0).getLength(), LENGTH_127);
        assertEquals(tlvs.get(0).getValue().length, 127);
    }
    
    @Test
    public void testDecodeWithSequenceLengthLessThan256() {
        final byte[] bytes = generateSequenceWithLongLengthTag(255);
        List<TagLengthValue> tlvs = EncodingHelper.getTlvs(bytes);
        assertTrue(tlvs.size() == 1);
        assertEquals(tlvs.get(0).getTag(), TEST_ONE_SEQUENCE_TAG);
        assertArrayEquals(tlvs.get(0).getLength(), LENGTH_255);
        assertEquals(tlvs.get(0).getValue().length, 255);
    }
    
    @Test
    public void testDecodeWithSequenceLength256() {
        final byte[] bytes = generateSequenceWithLongLengthTag(256);
        List<TagLengthValue> tlvs = EncodingHelper.getTlvs(bytes);
        assertTrue(tlvs.size() == 1);
        assertEquals(tlvs.get(0).getTag(), TEST_ONE_SEQUENCE_TAG);
        assertArrayEquals(tlvs.get(0).getLength(), LENGTH_256);
        assertEquals(tlvs.get(0).getValue().length, 256);
    }
    
    @Test
    public void shouldBuildNullElement() {
        ByteBuffer bb = EncodingHelper.buildNullElement(NULL_ELEMENT[0]);
        assertArrayEquals(bb.array(), NULL_ELEMENT);
    }
    
    @Test
    public void shouldBuildIsdnAddressStringEven() {
        ByteBuffer bb = EncodingHelper.buildIsdnAddressStringElement(ADDRESS_EVEN, ADDRESS_EVEN_BYTES[0]);
        assertArrayEquals(bb.array(), ADDRESS_EVEN_BYTES);
    }
    
    @Test
    public void shouldBuildIsdnAddressStringOdd() {
        ByteBuffer bb = EncodingHelper.buildIsdnAddressStringElement(ADDRESS_ODD, ADDRESS_ODD_BYTES[0]);
        assertArrayEquals(bb.array(), ADDRESS_ODD_BYTES);
    }
    
    byte[] generateSequenceWithLongLengthTag(final int length) {
        byte tag = 0x30;
        byte[] lengthBytes = EncodingHelper.getAsn1Length(length);
        final byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = (byte) (i & 0x7F);
        }
        ByteBuffer bb = ByteBuffer.allocate(length + lengthBytes.length + 1);
        bb.put(tag).put(lengthBytes).put(bytes);
        return bb.array();
    }
}
