# Project Rules

Các file trong thư mục này bổ sung cho `AGENTS.md`; khi có xung đột, `AGENTS.md` gần file đang sửa nhất được ưu tiên.

| File | Phạm vi |
|---|---|
| `general.md` | Mọi thay đổi trong repository |
| `backend.md` | `task-management-api/**` |
| `frontend.md` | `TaskManagement-ui/task-management-ui/**` |
| `testing.md` | Kiểm thử, lint, build và báo cáo kết quả |
| `security.md` | Xác thực, phân quyền, dữ liệu nhạy cảm và WebSocket |

## Thứ tự áp dụng

1. Đọc `AGENTS.md` ở root và `AGENTS.md` gần phạm vi đang sửa nhất.
2. Đọc `general.md` và các rule theo module liên quan.
3. Đọc tài liệu nghiệp vụ/kiến trúc được rule yêu cầu.
4. Giữ nguyên thay đổi chưa commit không thuộc phạm vi.
