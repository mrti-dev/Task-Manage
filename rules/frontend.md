# Frontend Rules

Áp dụng cho `TaskManagement-ui/task-management-ui/**`.

## Kiến trúc

- Gọi HTTP qua module trong `src/api`; component không gọi Axios trực tiếp.
- Tái sử dụng MUI theme, component chung và pattern hiện có trước khi tạo abstraction mới.
- Giữ state cục bộ tối thiểu; derive state khi có thể và cleanup listener/subscription khi unmount.
- Dùng định tuyến và API contract hiện có; không hard-code URL backend trong component.

## Trải nghiệm

- Mọi luồng bất đồng bộ liên quan phải có loading, empty, error và disabled state phù hợp.
- Giao diện phải dùng được trên desktop và mobile; không chỉ thu nhỏ layout desktop.
- Chỉ cập nhật UI hoặc phát event đồng bộ sau khi API thành công.
- Khi dữ liệu thay đổi, cập nhật mọi view liên quan như sidebar, list, card, badge mà không yêu cầu refresh.
- Không dùng optimistic update nếu chưa có rollback rõ ràng.

## Realtime

- Chuẩn hóa ID trước khi so sánh hoặc cập nhật state.
- Event handler phải idempotent và không tạo bản ghi trùng khi REST response và STOMP cùng đến.
- Ngắt STOMP subscription và event-bus listener khi component bị hủy.
