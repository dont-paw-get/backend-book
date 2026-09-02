# ADR-0013: 제목·저자 교집합 검색 API 추가

- 상태: Accepted
- 일자: 2026-09-02

## 배경

ADR-0012에서 `GET /api/v1/books/search`를 isbn 단건 조회로 전환했다. 하지만 isbn을 아직 모르고 제목·저자만 아는 상황(바코드가 없거나 스캔이 안 되는 경우)에서도 알라딘 후보를 찾을 방법이 필요하다. 알라딘 `ItemSearch`는 제목과 저자를 동시에 AND 조건으로 거는 검색을 지원하지 않는다.

## 결정

1. 신규 endpoint `GET /api/v1/books/search/by-title-author?title={t}&author={a}`(operationId `searchBookByTitleAndAuthor`)를 추가한다. ADR-0012의 isbn 검색은 그대로 둔다.
2. 서버가 알라딘 `ItemSearch`를 **제목으로 한 번(`QueryType=Title`), 저자로 한 번(`QueryType=Author`)** 호출한다. 각 호출은 `MaxResults=50`으로 요청한다.
3. 두 결과 리스트를 `ExternalBook`으로 매핑한 뒤 **isbn(isbn13 우선) 동일성**으로 교집합을 구한다. isbn이 없는 후보는 교집합 대상에서 제외한다.
4. "최상단"은 **제목 검색 결과의 알라딘 반환 순서**를 유지해, 그중 저자 검색 결과에도 있는 첫 번째 책이다. 알라딘의 기본 정렬(정확도/판매량)을 그대로 신뢰한다.
5. `title` 또는 `author`가 없거나 공백이면 400 `INVALID_SEARCH_PARAMETER`(메시지 "유효한 제목과 저자가 필요합니다.").
6. 교집합이 비거나 한쪽 검색 결과가 0건이면 HTTP 200 + `{ "book": null }`로 응답한다(404 아님). 클라이언트가 직접 도서 정보를 입력하는 폴백 흐름으로 이어진다.
7. 이 endpoint는 **서재 등록 여부를 확인하지 않는다**(`alreadyRegistered` 없음) — 순수 외부 검색이다. 응답 스키마는 `{ book: ExternalBook | null }` 단일 필드(`TitleAuthorBookSearchResponse`).
8. 알라딘이 실제 오류(`errorCode`)를 반환하면 502 `ALADIN_API_ERROR`. 결과 없음은 빈 `item` 배열로 오므로 정상 처리한다(`ItemLookUp`의 errorCode 8과 다른 지점).

## 결과

- `docs/api/openapi.yaml`(v0.10.0): `searchBookByTitleAndAuthor` operation, `TitleAuthorBookSearchResponse` 스키마 신설. `InvalidSearchParameter` 응답을 isbn 전용 설명에서 두 검색을 모두 아우르는 설명 + `examples` 2종으로 일반화.
- `BookDiscoveryClient` 포트에 `searchByTitle(title)` / `searchByAuthor(author)`(각각 `List<ExternalBook>`) 추가. `AladinBookDiscoveryClient`가 `ItemSearch.aspx` 호출을 재도입한다(ADR-0003/CLIAR-34에서 한 번 제거했던 것). 기존 `AladinSearchResponse`/`AladinItem`/`toExternalBook`/`AuthorNameNormalizer`를 재사용한다.
- `BookDiscoveryService.searchByTitleAndAuthor(title, author)`가 교집합·최상단 로직을 담당한다(순수 로직, memberId·서재 조회 불필요). 자동 계측되는 RestClient outbound span 외에 custom span은 추가하지 않는다.
