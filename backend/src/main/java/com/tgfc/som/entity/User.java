package com.tgfc.som.entity;

import java.util.Date;

public class User {
    private String empId;

    private String channelId;

    private String storeId;

    private String empName;

    private String systemFlag;

    private String disabledFlag;

    private Short authorityGrade;

    private Date startDate;

    private Date endDate;

    private Date updateDate;

    private String updateEmpId;

    private String updateEmpName;

    public String getEmpId() {
        return empId;
    }

    public void setEmpId(String empId) {
        this.empId = empId == null ? null : empId.trim();
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId == null ? null : channelId.trim();
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId == null ? null : storeId.trim();
    }

    public String getEmpName() {
        return empName;
    }

    public void setEmpName(String empName) {
        this.empName = empName == null ? null : empName.trim();
    }

    public String getSystemFlag() {
        return systemFlag;
    }

    public void setSystemFlag(String systemFlag) {
        this.systemFlag = systemFlag == null ? null : systemFlag.trim();
    }

    public String getDisabledFlag() {
        return disabledFlag;
    }

    public void setDisabledFlag(String disabledFlag) {
        this.disabledFlag = disabledFlag == null ? null : disabledFlag.trim();
    }

    public Short getAuthorityGrade() {
        return authorityGrade;
    }

    public void setAuthorityGrade(Short authorityGrade) {
        this.authorityGrade = authorityGrade;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
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