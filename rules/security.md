# Security Rules

## Xác thực và phân quyền

- Mọi quyền truy cập phải được kiểm tra phía server; route guard frontend chỉ phục vụ UX.
- Kiểm tra quyền theo resource cụ thể, không chỉ dựa vào việc token hợp lệ hoặc role toàn cục.
- Không mở rộng quyền OWNER/ADMIN/MEMBER khi ma trận nghiệp vụ còn `TBD`.
- WebSocket handshake và subscription phải xác thực user; không tin `userId`, workspace ID hoặc destination do client gửi.

## Dữ liệu nhạy cảm

- Không hard-code, commit hoặc log token, cookie, mật khẩu, OTP, SMTP credential, khóa JWT hay khóa mã hóa outbox.
- Dùng biến môi trường cho secret; test dùng giá trị giả cô lập.
- Không trả stack trace, thông tin tồn tại tài khoản hoặc chi tiết hạ tầng trong response công khai.
- Cookie refresh token phải giữ thuộc tính bảo mật phù hợp theo môi trường.

## Kiểm tra thay đổi bảo mật

- Thêm test cho request không xác thực, sai quyền và quyền hợp lệ.
- Kiểm tra HTTP CORS và WebSocket origin/auth riêng biệt; một cấu hình không chứng minh cấu hình còn lại.
- Với truy vấn theo ID, thêm test chống truy cập chéo workspace/project/task.
