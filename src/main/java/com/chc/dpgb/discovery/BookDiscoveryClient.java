package com.chc.dpgb.discovery;

import java.util.List;
import java.util.Optional;

public interface BookDiscoveryClient {

    Optional<ExternalBook> lookup(String isbn);

    List<ExternalBook> searchByTitle(String title);

    List<ExternalBook> searchByAuthor(String author);
}
