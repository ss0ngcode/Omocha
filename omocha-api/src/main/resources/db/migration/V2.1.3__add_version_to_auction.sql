-- auction 테이블에 version 컬럼 추가
ALTER TABLE auction
    ADD COLUMN version BIGINT;

-- 기존 데이터에 대해 version 초기값 설정
UPDATE auction
SET version = 0;

-- version 컬럼에 NOT NULL 제약조건 추가
ALTER TABLE auction
    ALTER COLUMN version SET NOT NULL;
