package org.example;

public enum ContractCallOptType {
    INIT(0),
    MAIN(1),
    QUERY(2);

    private Integer value;
    private ContractCallOptType(int value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }
}
