package com.chc.dpgb.library;

// 키 공간 소진 시 호출자가 서재 전체 rebalance를 트리거하도록 하는 내부 신호 — API로 노출되지 않는다.
public class ShelfRankExhaustedException extends RuntimeException {
}
