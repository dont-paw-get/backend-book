package com.chc.dpgb.discovery.aladin;

import java.util.List;

record AladinSearchResponse(Integer errorCode, String errorMessage, List<AladinItem> item) {
}
