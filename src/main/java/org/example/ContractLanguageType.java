package org.example;

import jnr.ffi.annotations.In;

public enum ContractLanguageType {
    JAVASCRIPT(0),
    SOLIDITY(1);

    private Integer value;
    private ContractLanguageType(int value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }


}
