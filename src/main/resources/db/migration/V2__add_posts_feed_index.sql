-- V2: index for the cursor-paged feed (GET /api/posts).
-- The query is
--   WHERE created_at < ? OR (created_at = ? AND id < ?)
--   ORDER BY created_at DESC, id DESC LIMIT n
-- With this index MySQL walks the B-tree backwards from the cursor position and
-- stops after n rows, so every page costs the same. Without it, MySQL reads and
-- sorts the whole posts table on every request (EXPLAIN shows "Using filesort").
CREATE INDEX idx_posts_created_at_id ON posts (created_at, id);
