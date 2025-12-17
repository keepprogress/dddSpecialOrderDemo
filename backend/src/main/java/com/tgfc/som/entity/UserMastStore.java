package com.tgfc.som.entity;

import java.util.Date;

public class UserMastStore {
    private String empId;

    private String storeId;

    private String channelId;

    private Date updateDate;

    private String updateEmpId;

    private String updateEmpName;

    public String getEmpId() {
        return empId;
    }

    public void setEmpId(String empId) {
        this.empId = empId == null ? null : empId.trim();
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId == null ? null : storeId.trim();
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId == null ? null : channelId.trim();
    }

    public Date getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
    }

    public String getUpdateEmpId() {
        return updateEmpId;
    }

    public void setUpdateEmpId(String updateEmpId) {
        this.updateEmpId = updateEmpId == null ? null : updateEmpId.trim();
    }

    public String getUpdateEmpName() {
        return updateEmpName;
    }

    public void setUpdateEmpName(String updateEmpName) {
        this.updateEmpName = updateEmpName == null ? null : updateEmpName.trim();
    }
}