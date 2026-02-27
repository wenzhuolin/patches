package com.patches.plm.domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "kpi_evaluation_detail")
public class KpiEvaluationDetailEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "evaluation_id", nullable = false)
    private Long evaluationId;

    @Column(name = "rule_id", nullable = false)
    private Long ruleId;

    @Column(name = "metric_value")
    private Double metricValue;

    @Column(name = "threshold_snapshot", length = 128)
    private String thresholdSnapshot;

    @Column(name = "pass", nullable = false)
    private boolean pass;

    @Column(name = "reason", columnDefinition = "text")
    private String reason;

    @Column(name = "evidence", columnDefinition = "text")
    private String evidence;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEvaluationId() {
        return evaluationId;
    }

    public void setEvaluationId(Long evaluationId) {
        this.evaluationId = evaluationId;
    }

    public Long getRuleId() {
        return ruleId;
    }

    public void setRuleId(Long ruleId) {
        this.ruleId = ruleId;
    }

    public Double getMetricValue() {
        return metricValue;
    }

    public void setMetricValue(Double metricValue) {
        this.metricValue = metricValue;
    }

    public String getThresholdSnapshot() {
        return thresholdSnapshot;
    }

    public void setThresholdSnapshot(String thresholdSnapshot) {
        this.thresholdSnapshot = thresholdSnapshot;
    }

    public boolean isPass() {
        return pass;
    }

    public void setPass(boolean pass) {
        this.pass = pass;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }
}
