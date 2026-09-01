package com.chc.dpgb.common.observation;

import java.util.ArrayList;
import java.util.List;

import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;

/**
 * 직접 추가한 custom span의 계약(이름·속성·오류)을 단위 테스트에서 검증하기 위한 기록용 핸들러.
 * <p>
 * 별도 테스트 라이브러리(micrometer-observation-test)를 들이지 않고, 실제로 끝난 관측 결과만 모아 검증한다.
 */
public final class RecordingObservationHandler implements ObservationHandler<Observation.Context> {

    private final List<Observation.Context> stopped = new ArrayList<>();

    /**
     * 이 핸들러 하나만 붙은 {@link ObservationRegistry}를 만든다.
     */
    public static ObservationRegistry registryWith(RecordingObservationHandler handler) {
        ObservationRegistry registry = ObservationRegistry.create();
        registry.observationConfig().observationHandler(handler);
        return registry;
    }

    @Override
    public void onStop(Observation.Context context) {
        stopped.add(context);
    }

    @Override
    public boolean supportsContext(Observation.Context context) {
        return true;
    }

    public List<String> names() {
        return stopped.stream().map(Observation.Context::getName).toList();
    }

    public List<String> lowCardinalityValues(String key) {
        return values(key, true);
    }

    public List<String> highCardinalityValues(String key) {
        return values(key, false);
    }

    public List<Throwable> errors() {
        return stopped.stream().map(Observation.Context::getError).filter(error -> error != null).toList();
    }

    private List<String> values(String key, boolean lowCardinality) {
        return stopped.stream()
                .map(context -> lowCardinality
                        ? context.getLowCardinalityKeyValue(key)
                        : context.getHighCardinalityKeyValue(key))
                .filter(keyValue -> keyValue != null)
                .map(KeyValue::getValue)
                .toList();
    }
}
