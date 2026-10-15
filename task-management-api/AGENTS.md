# Hướng dẫn cho AI Agent

## Phạm vi

Áp dụng cho toàn bộ thư mục `task-management-api`.

## Nguồn thông tin chuẩn

Trước khi sửa mã, phải đọc:

1. `docs/product-spec.md`
2. `docs/architecture.md`
3. `docs/business.flow`

Nếu tài liệu và mã nguồn mâu thuẫn, dừng suy diễn, nêu rõ khác biệt và yêu cầu xác nhận.

## Quy trình làm việc

1. Xác định yêu cầu và tiêu chí chấp nhận.
2. Đối chiếu luồng nghiệp vụ, kiến trúc và mã hiện tại.
3. Chỉ sửa phần nhỏ nhất cần thiết.
4. Thêm hoặc cập nhật kiểm thử cho hành vi thay đổi.
5. Chạy kiểm thử liên quan trước, sau đó chạy bộ kiểm thử toàn dự án khi khả thi.
6. Cập nhật tài liệu nếu hợp đồng hoặc hành vi thay đổi.

## Quy tắc code

- Ưu tiên code ngắn, rõ ràng và nhất quán với dự án.
- Không thêm dependency, abstraction hoặc cấu hình khi chưa cần thiết.
- Không để logic nghiệp vụ trong controller.
- Không log token, mật khẩu, dữ liệu cá nhân hoặc nội dung nhạy cảm.
- Không triển khai nội dung đánh dấu `TBD` nếu chưa được xác nhận.
- Không sửa ngoài phạm vi yêu cầu.

## Definition of Done

- Đáp ứng đủ tiêu chí chấp nhận.
- Kiểm thử liên quan thành công.
- Không làm thay đổi hành vi ngoài phạm vi.
- Spec, sơ đồ nghiệp vụ và mã nguồn thống nhất.
