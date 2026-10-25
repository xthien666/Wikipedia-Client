# Kịch bản phần TV5 và phối hợp demo nhóm

## Phần nói của TV5, khoảng 2 phút

0:00–0:25: “Em phụ trách Cài đặt, About, nguồn/chia sẻ và tổng hợp kiểm thử. Bản hiện tại giữ bốn mức chữ 14, 16, 18, 22 sp. Lựa chọn lưu bằng SharedPreferences.” Mở Cài đặt, đổi Nhỏ rồi Rất lớn, xem preview tiếng Việt.

0:25–0:45: Đóng/mở app và vào Cài đặt: mức Rất lớn vẫn chọn. Mở bài thật: HTML dùng cỡ chữ tương ứng. Nếu cần, trình chiếu bằng chứng 23 và 09 thay cho chờ mạng.

0:45–1:10: Mở bài A Văn Miếu – Quốc Tử Giám, theo liên kết bài phường B. Nhấn Chia sẻ và đọc tiêu đề/URL B, hủy bảng chia sẻ; Back về A. Không gửi tới người nhận trong demo.

1:10–1:30: Chỉ dòng giấy phép/revision của bài và nút Bài gốc. Mở About: nguồn nội dung, điều khoản Wikimedia và GitHub tách riêng. Thông tin nhóm chỉ bổ sung khi đã nhận dữ liệu thật.

1:30–2:00: “Bản này có 79 unit test đạt, trong đó 12 ca TV5 mới. Mất mạng/thử lại đã chạy trên emulator. Báo cáo T01–T16 ghi rõ phần có bằng chứng và phần chờ thiết bị/owner. Unit test không thay toàn bộ nghiệm thu.” Chiếu ảnh 19/21 nếu minh họa lỗi mạng.

## Khung demo nhóm 5–7 phút, chờ nội dung từng người

- TV1, 45–60 giây: phạm vi, kiến trúc, build/APK và điều hướng.
- TV2, 60–75 giây: tìm kiếm thật, tải thêm, lỗi/retry và xử lý request cũ.
- TV3, 60–75 giây: HTML, link, anchor, Back và lifecycle.
- TV4, 60–75 giây: yêu thích/lịch sử, định danh và dữ liệu sau mở lại.
- TV5, 90–120 giây: cỡ chữ, About, nguồn/chia sẻ và kết quả kiểm thử.

Nhóm phải tự diễn tập đủ, kiểm tra thời gian và chọn một APK cuối cùng. Slide nhóm sẽ thực hiện sau khi nhận nội dung của các thành viên; gói hiện tại chưa có slide. Video dự phòng 90 giây không có lời, quay thao tác Settings/About/mở bài; mở trước khi bảo vệ để xác nhận âm thanh/hiển thị theo máy trình chiếu.

## Checklist Android thật/TalkBack còn chờ

1. Cài đúng APK và ghi model/Android/WebView/SHA.
2. Chọn 22 sp, đọc tiếng Việt, kiểm tra xuống dòng và bảng; đóng/mở app xác nhận lưu.
3. Tăng cỡ chữ hệ thống, kiểm tra scroll đến About, không cắt nội dung/vùng chạm.
4. Bật TalkBack bằng người kiểm thử: đọc tên/mức đã chọn, nút chia sẻ/nguồn có tên bài, giấy phép có vai trò nút; đi tuần tự không lạc focus.
5. A→B→Back, kiểm tra HTML, title, canonical, license/revision đúng bài; chọn app nhận để xem preview, hủy và không gửi.
6. Mở bài gốc và giấy phép bằng trình duyệt đã thiết lập; xác nhận URL thật.
7. Ghi kết quả, ảnh/video, lỗi và owner vào CSV. Không ghi “đạt” trước khi chạy.
