package com.lytvest.audiotts.model.enums;

public enum SentenceStatus {
    WAITING_FOR_CHARACTER,
    DETERMINING_CHARACTER,
    WAITING_FOR_STRESS,
    SETTING_STRESS,
    WAITING_FOR_TTS,
    GENERATING_TTS,
    READY
}
