package com.tgfc.som.entity;

public class Channel {
    private String channelId;

    private String channelName;

    private String posChannel;

    private String companyCode;

    private Integer sequenceNum;

    private String sapChannel;

    private String dcStoreId;

    private String status;

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId == null ? null : channelId.trim();
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName == null ? null : channelName.trim();
    }

    public String getPosChannel() {
        return posChannel;
    }

    public void setPosChannel(String posChannel) {
        this.posChannel = posChannel == null ? null : posChannel.trim();
    }

    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode == null ? null : companyCode.trim();
    }

    public Integer getSequenceNum() {
        return sequenceNum;
    }

    public void setSequenceNum(Integer sequenceNum) {
        this.sequenceNum = sequenceNum;
    }

    public String getSapChannel() {
        return sapChannel;
    }

    public void setSapChannel(String sapChannel) {
        this.sapChannel = sapChannel == null ? null : sapChannel.trim();
    }

    public String getDcStoreId() {
        return dcStoreId;
    }

    public void setDcStoreId(String dcStoreId) {
        this.dcStoreId = dcStoreId == null ? null : dcStoreId.trim();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status == null ? null : status.trim();
    }
}