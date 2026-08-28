package com.chc.dpgb.discovery;

import java.util.Optional;

public interface BookDiscoveryClient {

    Optional<ExternalBook> lookup(String isbn);
}
