package com.chc.dpgb.discovery;

import java.util.List;

public interface BookDiscoveryClient {

	List<ExternalBook> search(String title, String author);
}
