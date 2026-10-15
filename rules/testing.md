# Testing Rules

## Nguyên tắc

- Mỗi thay đổi hành vi cần ít nhất một trường hợp thành công và một trường hợp thất bại phù hợp.
- Ưu tiên test gần lớp thay đổi; thêm integration test khi cần chứng minh security, transaction, repository hoặc API contract.
- Test phải độc lập, xác định và không phụ thuộc dịch vụ ngoài, Gmail thật hoặc database dùng chung.
- Không coi report test cũ là kết quả chạy mới.

## Backend

```powershell
cd task-management-api
.\mvnw.cmd test
```

- Dùng H2 cho test thông thường; chỉ gọi là MySQL integration test khi thực sự chạy trên MySQL cô lập.
- Không làm yếu cấu hình production chỉ để test qua.

## Frontend

```powershell
cd TaskManagement-ui/task-management-ui
npm test
npm run lint
npm run build
```

- Test utility/state transition bằng test hiện có; kiểm tra trực tiếp UI khi thay đổi hành vi hiển thị hoặc responsive.

## Báo cáo

- Ghi rõ lệnh, kết quả, số test nếu công cụ cung cấp và giới hạn chưa kiểm tra.
- Phân biệt rõ source review, test tự động, build và runtime/browser verification.
