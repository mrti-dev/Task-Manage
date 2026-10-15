# Backend Rules

Áp dụng cho `task-management-api/**`.

## Nguồn chuẩn

Đọc trước khi sửa:

- `task-management-api/docs/product-spec.md`
- `task-management-api/docs/architecture.md`
- `task-management-api/docs/business.flow`

## Thiết kế

- Controller chỉ nhận request, validate DTO và ánh xạ response; nghiệp vụ và kiểm tra quyền đặt trong service.
- Không trả entity trực tiếp nếu có thể làm lộ quan hệ hoặc dữ liệu nội bộ.
- Mọi truy vấn theo ID phải giới hạn theo workspace/project/task mà người dùng được truy cập.
- Thao tác nhiều bước cần transaction rõ ràng; khóa dữ liệu khi bất biến có thể bị race condition phá vỡ.
- Truy vấn danh sách phải lọc và phân trang tại database; tránh `findAll()` rồi lọc trong bộ nhớ và tránh N+1.

## Dữ liệu và API

- Validate request bằng Jakarta Validation và bảo vệ bất biến thêm tại service.
- Thay đổi schema phải có Flyway migration tương thích; production dùng Hibernate `validate`.
- Giữ response lỗi ổn định, không trả stack trace hoặc chi tiết nội bộ.
- Không thay đổi endpoint, DTO hoặc mã trạng thái HTTP nếu yêu cầu không cần.

## Realtime và email

- Chỉ phát STOMP notification hoặc đánh thức mail worker sau khi transaction commit.
- API xếp hàng email không được mô tả là đã gửi thành công.
- Payload outbox nhạy cảm phải được mã hóa và có giới hạn retry như đặc tả hiện tại.
