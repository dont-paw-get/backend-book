package com.chc.dpgb.discovery;

import java.util.List;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.chc.dpgb.common.exception.InvalidSearchParameterException;

@Service
public class BookDiscoveryService {

	private final BookDiscoveryClient bookDiscoveryClient;

	/**
	 * bookDiscoveryClient는 실제로는 ALADIN_API_TTB_KEY를 읽는 @Lazy 빈이다.
	 * 주입 지점에도 @Lazy를 붙여야 지연 프록시가 생성되고, 그렇지 않으면 이 생성자가
	 * 즉시 실행될 때 실제 빈이 만들어지며 자격 증명 placeholder가 즉시 해석된다.
	 */
	BookDiscoveryService(@Lazy BookDiscoveryClient bookDiscoveryClient) {
		this.bookDiscoveryClient = bookDiscoveryClient;
	}

	public List<ExternalBook> search(String title, String author) {
		if (isBlank(title) && isBlank(author)) {
			throw new InvalidSearchParameterException();
		}
		return bookDiscoveryClient.search(title, author);
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
