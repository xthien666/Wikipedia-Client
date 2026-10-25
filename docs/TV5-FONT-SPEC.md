# Đặc tả cỡ chữ TV5 - bản bàn giao 23/10/2026

Kế hoạch ban đầu nêu ba mức. Bản tích hợp bbdf0c4 đã có bốn mức 14/16/18/22 sp và test lưu/áp dụng. Bản TV5 này giữ bốn mức để phù hợp mã đang tích hợp; mức thứ tư không phải lỗi chức năng. Đây là lựa chọn cho bản bàn giao theo mặc định đã thông báo, chưa phải biên bản xác nhận của toàn nhóm. Nhóm đối chiếu rubric trước khi nộp; nếu môn bắt buộc ba mức thì thống nhất lại.

Nhỏ 14; Chuẩn 16 (mặc định); Lớn 18; Rất lớn 22. Giá trị ngoài tập trở về 16. SharedPreferences reader_settings/font_size lưu bền. Preview native dùng sp, WebView defaultFontSize dùng mức chọn và textZoom theo fontScale hệ thống. TV3 tiếp tục sở hữu HTML/CSS và bố cục reader.

Đã kiểm tra tự động cả bốn mức; emulator xem tiếng Việt ở 14/22, cold restart 22, HTML thật 22 và system fontScale 1.5. Chưa có Android thật/TalkBack. Chưa coi bốn mức đã được toàn nhóm xác nhận.
