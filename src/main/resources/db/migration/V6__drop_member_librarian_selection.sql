-- 대표 사서 선택 기능이 User/Member 서비스로 이관되어 Book Service에서 제거된다.
-- librarian(마스터 카탈로그) 테이블과 시드 데이터는 Book Service가 계속 소유하므로 유지한다.
DROP TABLE member_librarian_selection;
