# Product Specification

## 1. Mục tiêu

Cung cấp API quản lý công việc với hành vi rõ ràng, nhất quán, có thể kiểm thử và dễ mở rộng.

## 2. Đối tượng sử dụng

- Người dùng quản lý công việc cá nhân hoặc nhóm.
- Ứng dụng khách tích hợp qua API.
- Quản trị viên hệ thống nếu sản phẩm yêu cầu phân quyền quản trị.

## 3. Phạm vi chức năng

Trạng thái dưới đây phản ánh hành vi đã có trong mã nguồn và kiểm thử; các quy tắc chuyển trạng thái hoặc chính sách chưa được xác định vẫn để `TBD`.

| Mã | Chức năng | Trạng thái |
|---|---|---|
| F-01 | Tạo công việc | Đã triển khai; kiểm tra dữ liệu ở DTO/service |
| F-02 | Xem danh sách và chi tiết công việc | Đã triển khai; danh sách có phân trang và lọc |
| F-03 | Cập nhật công việc | Đã triển khai; quyền được kiểm tra ở service |
| F-04 | Thay đổi trạng thái công việc | Đã triển khai; tập trạng thái có trong enum, quy tắc chuyển tiếp: TBD |
| F-05 | Xóa hoặc lưu trữ công việc | Xóa mềm đã triển khai; chính sách lưu trữ: TBD |
| F-06 | Xác thực và phân quyền | JWT và kiểm tra quyền theo workspace/project/task đã triển khai |
| F-07 | Thêm thành viên và chuyển Owner | Đã triển khai; Owner/Admin thêm tài khoản hiện có trực tiếp, người nhận được thông báo qua app và email |
| F-08 | Calendar và nhắc deadline | Đã triển khai; lịch nhắc mặc định 08:00 theo múi giờ cấu hình |
| F-09 | Checklist, tag và phụ thuộc task | Đã triển khai |
| F-10 | My Tasks, tìm kiếm và bộ lọc lưu | Đã triển khai; bộ lọc lưu theo người dùng |
| F-11 | Dashboard workspace | Đã triển khai; tiến độ, workload và overdue; có lọc project/ngày |
| F-12 | Xác minh đăng ký OTP qua Gmail | Đã triển khai; OTP 6 số hạn 5 phút, lưu hash, tối đa 5 lần thử, cooldown 60s, từ chối login khi chưa xác minh |
| F-13 | Thông báo thêm thành viên Workspace | Đã triển khai; membership, notification và email outbox được ghi cùng giao dịch; realtime và gửi email sau commit |

## 4. Quy tắc nghiệp vụ

- Mỗi công việc phải có định danh duy nhất.
- Dữ liệu đầu vào phải được kiểm tra trước khi xử lý.
- Đăng ký tài khoản tạo trạng thái `email_verified=false`, không cấp JWT cho đến khi xác minh OTP thành công.
- Đăng nhập chỉ cho phép với tài khoản đã `email_verified=true`; tài khoản chưa xác minh trả mã lỗi `EMAIL_NOT_VERIFIED`.
- Bảng xác minh chỉ lưu hash OTP; payload gửi mail chứa OTP được mã hóa trong outbox đến khi gửi thành công. OTP hạn 5 phút, tối đa 5 lần thử sai (số lần sai được lưu bền vững), cooldown gửi lại 60 giây và vô hiệu hóa OTP cũ. Endpoint gửi lại trả response chung cho email hợp lệ, không tồn tại, đã xác minh hoặc đang cooldown.
- Refresh token chỉ được dùng để cấp access token cho tài khoản đã xác minh email.
- Workspace mới có đúng một Owner; chuyển Owner phải nhắm tới thành viên hiện có và chạy trong giao dịch có khóa workspace.
- Chỉ Owner/Admin được thêm thành viên; email phải thuộc tài khoản hiện có, OWNER chỉ được chỉ định qua chuyển quyền.
- Khi thêm thành viên, membership, notification và email outbox được lưu trong cùng giao dịch; sau commit hệ thống phát notification realtime và worker gửi email.
- Email OTP và email thông báo thành viên được mã hóa trong outbox. Sau commit, worker nền được đánh thức để gửi ngay; polling mỗi giây là đường dự phòng. Worker thử lại tối đa 5 lần khi lỗi; API thành công nghĩa là đã xếp hàng, chưa xác nhận đã gửi.
- Migration xác minh email đánh dấu user tồn tại trước khi bật tính năng là đã xác minh; đăng ký mới luôn bắt đầu ở trạng thái chưa xác minh.
- Chuyển trạng thái task hợp lệ: TBD; không suy diễn quy tắc ngoài enum và validation hiện có.
- Không trả thông tin nội bộ hoặc dữ liệu nhạy cảm trong lỗi API.
- Quy tắc chưa được xác nhận phải ghi `TBD`, không tự suy diễn.

## 5. Yêu cầu API

- Sử dụng định dạng JSON thống nhất.
- Dùng đúng mã trạng thái HTTP theo kết quả xử lý.
- Phản hồi lỗi có cấu trúc ổn định: mã lỗi, thông báo và chi tiết hợp lệ.
- Các thao tác ghi phải xác định rõ tính lặp an toàn và xử lý xung đột.
- Sắp xếp checklist phải chứa đầy đủ mỗi item của task đúng một lần.
- Endpoint, DTO và response được định nghĩa trong controller/DTO; README liệt kê các route chính. Quy tắc HTTP cho xung đột nghiệp vụ: cần chuẩn hóa thêm.

## 6. Yêu cầu phi chức năng

- Bảo mật: kiểm tra quyền ở phía máy chủ; không ghi log bí mật.
- Toàn vẹn dữ liệu: thao tác nhiều bước phải có ranh giới giao dịch rõ ràng.
- Hiệu năng: phân trang danh sách; lọc trực tiếp tại database theo user/workspace/active thay vì nạp toàn bảng vào bộ nhớ; danh sách task dùng batch query cho tags, checklist, comment count và subtask count để giữ nguyên response mà không phát sinh N+1 hoặc tích Descartes.
- Quan sát: lỗi phải có khả năng truy vết nhưng không làm lộ dữ liệu nhạy cảm.
- Kiểm thử: mỗi quy tắc nghiệp vụ có ít nhất một trường hợp thành công và thất bại.

## 7. Tiêu chí hoàn thành

- Yêu cầu và tiêu chí chấp nhận đã được cập nhật.
- Luồng nghiệp vụ và hợp đồng API khớp với triển khai.
- Kiểm thử tự động liên quan đều đạt.
- Không có thay đổi ngoài phạm vi hoặc mục `TBD` bị triển khai như yêu cầu chính thức.

## 8. Câu hỏi cần xác nhận

- Ma trận quyền chi tiết cho từng vai trò (OWNER, ADMIN, MEMBER) và từng thao tác.
- Thuộc tính bắt buộc của công việc.
- Tập trạng thái và quy tắc chuyển trạng thái.
- Chính sách xóa mềm, lưu trữ hoặc xóa vĩnh viễn.
- Quy tắc deadline, múi giờ, thông báo và phân công.
