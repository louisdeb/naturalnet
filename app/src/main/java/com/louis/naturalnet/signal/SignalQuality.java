package com.louis.naturalnet.signal;

public enum SignalQuality {
    NONE_OR_NOT_KNOWN(0),
    POOR(1),
    MODERATE(2),
    GOOD(3),
    GREAT(4);

    private int val;

    SignalQuality(int _val) {
        val = _val;
    }

    public int getVal() {
        return val;
    }
}
