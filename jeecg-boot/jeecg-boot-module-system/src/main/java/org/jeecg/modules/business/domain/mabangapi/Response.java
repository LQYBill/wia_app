package org.jeecg.modules.business.domain.mabangapi;

public abstract class Response {

    public enum Code {
        ERROR("999"),
        SUCCESS("200");

        public final String value;

        Code(String value) {
            this.value = value;
        }
    }

    private final Code status;

    protected Response(Code status) {
        this.status = status;
    }

    public boolean success() {
        return status == Code.SUCCESS;
    }

    public boolean error() {
        return status == Code.ERROR;
    }

}
