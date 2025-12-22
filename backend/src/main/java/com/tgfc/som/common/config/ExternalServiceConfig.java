package com.tgfc.som.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 外部服務設定
 *
 * 定義各外部服務的 timeout 設定
 */
@Configuration
@ConfigurationProperties(prefix = "external-service")
public class ExternalServiceConfig {

    /**
     * CRM 服務設定
     */
    private ServiceTimeout crm = new ServiceTimeout(Duration.ofSeconds(2), true);

    /**
     * 促銷引擎設定
     */
    private ServiceTimeout promotion = new ServiceTimeout(Duration.ofSeconds(2), true);

    /**
     * 商品主檔設定
     */
    private ServiceTimeout product = new ServiceTimeout(Duration.ofSeconds(1), false);

    public ServiceTimeout getCrm() {
        return crm;
    }

    public void setCrm(ServiceTimeout crm) {
        this.crm = crm;
    }

    public ServiceTimeout getPromotion() {
        return promotion;
    }

    public void setPromotion(ServiceTimeout promotion) {
        this.promotion = promotion;
    }

    public ServiceTimeout getProduct() {
        return product;
    }

    public void setProduct(ServiceTimeout product) {
        this.product = product;
    }

    /**
     * 服務超時設定
     */
    public static class ServiceTimeout {
        private Duration timeout;
        private boolean canDegrade;

        public ServiceTimeout() {
            this.timeout = Duration.ofSeconds(2);
            this.canDegrade = true;
        }

        public ServiceTimeout(Duration timeout, boolean canDegrade) {
            this.timeout = timeout;
            this.canDegrade = canDegrade;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        public boolean isCanDegrade() {
            return canDegrade;
        }

        public void setCanDegrade(boolean canDegrade) {
            this.canDegrade = canDegrade;
        }

        public long getTimeoutMillis() {
            return timeout.toMillis();
        }
    }
}
