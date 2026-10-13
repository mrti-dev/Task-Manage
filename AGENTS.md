# Hướng dẫn cho AI Agent

## Phạm vi

Áp dụng cho toàn bộ repository `Task-Manage`:

- `task-management-api`: Spring Boot, Maven, MySQL, JWT, STOMP/WebSocket.
- `TaskManagement-ui/task-management-ui`: React, Vite, MUI, Axios, STOMP.

`AGENTS.md` trong thư mục con được ưu tiên khi có quy tắc cụ thể hơn.

## Quy trình bắt buộc

1. Đọc `README.md` và các tài liệu liên quan trước khi sửa mã.
2. Kiểm tra `git status`; giữ nguyên thay đổi có sẵn của người dùng.
3. Xác định tiêu chí chấp nhận và phạm vi ảnh hưởng backend/frontend.
4. Chỉ sửa phần nhỏ nhất cần thiết, theo cấu trúc hiện tại.
5. Thêm hoặc cập nhật kiểm thử cho hành vi thay đổi.
6. Chạy kiểm thử liên quan, sau đó chạy kiểm tra toàn module khi khả thi.
7. Báo cáo rõ phần đã sửa, đã kiểm thử, chưa kiểm thử và lý do.

## Backend

Trước khi sửa `task-management-api`, đọc:

- `task-management-api/docs/product-spec.md`
- `task-management-api/docs/architecture.md`
- `task-management-api/docs/business.flow`

Quy tắc:

- Giữ controller mỏng; đặt nghiệp vụ và kiểm tra quyền trong service.
- Validate request bằng DTO; không trả entity trực tiếp nếu làm lộ dữ liệu.
- Mọi truy vấn theo ID phải kiểm tra quyền truy cập workspace/project/task.
- Không hard-code hoặc log token, mật khẩu, cookie hay dữ liệu nhạy cảm.
- Không thay đổi schema, dependency hoặc hợp đồng API nếu không cần thiết.
- Không triển khai nội dung `TBD` khi chưa được xác nhận.

Kiểm tra:

```powershell
cd task-management-api
.\mvnw.cmd test
```

## Frontend

Quy tắc:

- Dùng API module trong `src/api`; không gọi HTTP trực tiếp từ component.
- Tái sử dụng theme, component chung và pattern MUI hiện có.
- Chỉ phát event đồng bộ sau khi API thành công; cập nhật mọi view liên quan mà không cần refresh.
- Luôn xử lý loading, empty, error và responsive state khi có liên quan.
- Không thêm dependency hoặc abstraction nếu giải pháp hiện tại đã đủ.

Kiểm tra:

```powershell
cd TaskManagement-ui/task-management-ui
npm run lint
npm run build
```

## Quy tắc chung

- Code ngắn, rõ ràng; không thêm comment, logging, helper hoặc file không cần thiết.
- Không sửa ngoài phạm vi yêu cầu và không ghi đè thay đổi chưa commit của người dùng.
- Nếu tài liệu, mã và yêu cầu mâu thuẫn, nêu bằng chứng và dừng phần không thể xác minh.
- Thay đổi hành vi hoặc hợp đồng phải cập nhật tài liệu tương ứng.
- Không tuyên bố hoàn tất nếu chỉ build thành công nhưng chưa kiểm thử hành vi liên quan.

## Definition of Done

- Đủ tiêu chí chấp nhận và không phát sinh thay đổi ngoài phạm vi.
- Backend/frontend liên quan thống nhất về payload, quyền và trạng thái.
- Kiểm thử, lint và build liên quan thành công hoặc có giới hạn được nêu rõ.
- Không có secret, debug log, file sinh ra hoặc dependency thừa.
