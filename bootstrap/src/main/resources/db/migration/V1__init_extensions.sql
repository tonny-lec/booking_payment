-- =============================================================================
-- V1__init_extensions.sql
-- PostgreSQL拡張機能の初期化
-- =============================================================================

-- pgcrypto: gen_random_uuid()を含む暗号化関数を提供
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- uuid-ossp: UUID生成関数を提供 (uuid_generate_v4()など)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 拡張機能のドキュメントコメント
COMMENT ON EXTENSION pgcrypto IS 'gen_random_uuid()を含む暗号化関数';
COMMENT ON EXTENSION "uuid-ossp" IS 'UUID生成関数';
