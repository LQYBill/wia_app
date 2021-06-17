package org.jeecg.modules.business.domain.mabangapi.getorderlist;

public enum DateType {
    UPDATE("updateTime"),
    PAID("paidtime"),
    CREATE("createDate"),
    EXPRESS("expressTime");
    private final String text;

    DateType(String text) {
        this.text = text;
    }

    public String text() {
        return text;
    }
}
