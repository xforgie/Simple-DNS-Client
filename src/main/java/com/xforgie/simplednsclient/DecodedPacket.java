package com.xforgie.simplednsclient;

/**
 *
 * Stores information from a received packet
 *
 */

public class DecodedPacket {

    private enum Flag {
        QR ((byte)0x10),
        AA ((byte)0x8),
        TC ((byte)0x4),
        RD ((byte)0x2),
        RA ((byte)0x1);

        private final byte flagBit;

        Flag(byte flagbit) {
            this.flagBit = flagbit;
        }

        public byte getFlagBit() {
            return this.flagBit;
        }

        public static byte getValue(Flag flag) {
            return flag.getFlagBit();
        }
    }

    private int responseID;

    // 0, 0, 0, qr, aa, tc, rd, ra
    private byte flags;

    @SuppressWarnings("unused")
	private int opCode;
    private int rCode;

    @SuppressWarnings("unused")
	private int qdCount;
    private int anCount;
    private int nsCount;
    private int arCount;

    @SuppressWarnings("unused")
	private String domainName;

    @SuppressWarnings("unused")
	private int qType;
    @SuppressWarnings("unused")
	private int qClass;

    private ResourceRecord[] answers;
    private ResourceRecord[] authorities;
    private ResourceRecord[] additionals;

    public DecodedPacket() {

    }

    public void setFlags(byte flags) {
        this.flags = flags;
    }

    public boolean isAuthoritative() {
        return (flags & Flag.getValue(Flag.AA)) != 0x0;
    }

    public boolean isResponse() {
        return (flags & Flag.getValue(Flag.QR)) != 0;
    }

    public void setResponseID(int responseID) {
        this.responseID = responseID;
    }

    public void setOpCode(int opCode) {
        this.opCode = opCode & 0xF;
    }

    public void setRCode(int rCode) {
        this.rCode = rCode & 0xF;
    }

    public void setQdCount(int qdCount) {
        this.qdCount = qdCount & 0xFFFF;
    }

    public void setAnCount(int anCount) {
        this.anCount = anCount & 0xFFFF;
    }

    public void setNsCount(int nsCount) {
        this.nsCount = nsCount & 0xFFFF;
    }

    public void setArCount(int arCount) {
        this.arCount = arCount & 0xFFFF;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public void setQType(int qType) {
        this.qType = qType & 0xFFFF;
    }

    public void setQClass(int qClass) {
        this.qClass = qClass & 0xFFFF;
    }

    public void setANRecords(ResourceRecord[] rr) {
        this.answers = rr;
    }

    public void setNSRecords(ResourceRecord[] rr) {
        this.authorities = rr;
    }

    public void setARRecords(ResourceRecord[] rr) {
        this.additionals = rr;
    }

    public int getResponseID() {
        return responseID;
    }

    public int getANCount() {
        return anCount;
    }

    public int getNSCount() {
        return nsCount;
    }

    public int getARCount() {
        return arCount;
    }

    public int getRCode() {
        return rCode;
    }

    public ResourceRecord[] getAnswers() {
        return answers;
    }

    public ResourceRecord[] getAuthorities() {
    	return authorities; 
    }

    public ResourceRecord[] getAdditionals() {
        return additionals;
    }

}
