# General Rules

## Trước khi sửa

- Đọc `README.md`, kiểm tra `git status` và xác định rõ tiêu chí chấp nhận.
- Không ghi đè, hoàn tác hoặc định dạng lại thay đổi có sẵn ngoài phạm vi.
- Đối chiếu cả backend và frontend nếu thay đổi liên quan payload, quyền, trạng thái hoặc realtime event.
- Nếu yêu cầu, tài liệu và mã nguồn mâu thuẫn, nêu bằng chứng; không tự triển khai phần đang là `TBD`.

## Khi sửa

- Chọn thay đổi nhỏ nhất đáp ứng yêu cầu; giữ nguyên cấu trúc và convention hiện tại.
- Không thêm dependency, abstraction, helper, comment, debug log hoặc file nếu không cần thiết.
- Không thay đổi API, schema hoặc hành vi tương thích ngược ngoài phạm vi đã xác nhận.
- Cập nhật tài liệu khi hợp đồng hoặc hành vi quan sát được thay đổi.

## Hoàn thành

- Thêm hoặc cập nhật kiểm thử cho hành vi đã đổi.
- Chạy kiểm tra liên quan trước, sau đó kiểm tra toàn module khi khả thi.
- Báo cáo riêng: file đã sửa, kiểm tra đã chạy, kết quả và phần chưa xác minh.
- Build thành công không thay thế kiểm thử hành vi hoặc xác minh runtime.
