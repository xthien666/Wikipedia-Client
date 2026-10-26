# Đánh giá và tích hợp các nhánh thành viên — 26/10/2026

Nền tích hợp: main/4c79f6b, giữ giao diện và thumbnail đã có. Review dựa trên diff, hợp đồng dữ liệu, unit test, thao tác emulator và hồ sơ bàn giao. Các số kiểm thử dưới đây thuộc bản tích hợp tại máy TV1; không dùng kết quả tự báo của nhánh để thay cho lần chạy mới.

## Nhánh và kết luận

| Vai trò | Nhánh/commit bàn giao | Đánh giá và xử lý |
|---|---|---|
| TV2 | tv2-new / 96b1cef | Đã có continuation đầy đủ, bảo vệ tham số tìm kiếm cố định, retry 429/503 có giới hạn, parse Retry-After và thông báo search. Tiếp nhận sau sửa thêm deadline dùng chung cho manual retry/mở bài mới và biên số lớn. |
| TV4 | feature/tv4-library / f0557a2 | Giữ retry xóa khi refresh/callback muộn; nếu đã xóa nhưng đọc lỗi thì chỉ retry đọc; DialogFragment khôi phục đúng bài. Tiếp nhận sau giải quyết xung đột với thumbnail/UI và đổi test kho lịch sử sang SqliteHistoryStore hiện tại. |
| TV5 | feature/tv5-about-20261026 / 3ef225a | About có bốn họ tên/MSSV do nhánh cung cấp; test About và hồ sơ/rubric được cập nhật. Tiếp nhận. Hồ sơ vẫn ghi rõ giới hạn TalkBack/điện thoại, lỗi xoay API37 và phần thuyết trình chưa được tất cả thành viên xác nhận. |
| TV2/TV5 cũ | feature/search / 7c3a342; feature/tv5-settings / a00449e | Đã nằm trong main từ các lượt trước, không có commit mới cần merge lại. |
| TV1/TV3 | main | Giữ mã và UI mới nhất của main; hoàn thiện điểm nối rate limit, saved reading position và nghiệm thu tích hợp. Không tạo nhánh riêng. |

Giữ lịch sử tác giả bằng merge commit: d4d9807 (TV4), 99e1202 (TV5), 0b3d0a5 (TV2). Sửa tích hợp ở e51e24a. Không xóa nhánh thành viên.

## Điểm được sửa trong lần tích hợp

- TV4/LibraryFragment giữ ImageView/thumbnail adapter mới, đồng thời nhận Fragment result từ dialog mới. Không hoàn nguyên giao diện về bản prototype của nhánh TV4.
- Test LocalRepositoriesTest dùng SqliteHistoryStore cho các phép CRUD blocking; facade HistoryRepository bất đồng bộ vẫn giữ nguyên trong production. Test UI TV4 dùng mạng giả và SQLite thật để không khởi tạo Volley thật trong Robolectric. Các assertion dữ liệu và lifecycle được giữ.
- WikipediaApi giữ deadline Retry-After chung cho tìm kiếm, summary và nội dung reader. Bấm retry hoặc mở bài mới trong cửa sổ chờ chỉ xếp lịch; không gửi request sớm. Hủy request/cancelAll cũng hủy lịch chờ. Các ca này dùng transport/scheduler giả, không tạo tải để ép Wikimedia trả 429/503.
- SearchPage giữ constructor offset cũ, bao gồm null cho trang cuối; object continuation được tạo qua withContinuation để tránh overload null mơ hồ. Giá trị Retry-After cực lớn không tràn số khi đổi sang giây UI; phép nhân có chặn tràn tương thích minSdk23.
- Reader nhận thời gian chờ cụ thể và có đường thử lại qua API đã kiểm soát deadline. 404 tiếp tục không retry tự động. Khi WebView đổi viewport hoặc tạm báo scroll range bằng 0 trong reflow, không ghi đè vị trí đọc cũ bằng giá trị không hợp lệ.

## Kiểm chứng đã thực hiện

- assembleDebug, assembleDebugAndroidTest, lintDebug và testDebugUnitTest thành công; 143/143 unit test, không lỗi/bỏ qua. Suite gồm kiểm thử SQLite API23/28, UI/library lifecycle, continuation, backoff, cancel và deadline dùng chung.
- Hai test host bảo vệ dữ liệu khi sao lưu thất bại đạt.
- Reader đạt 33/33 kiểm tra trên emulator Android16/API36, WebView133.0.6943.137. Bài thật Hà Nội/Hệ Mặt Trời/Hà Thành; bảng rộng/ảnh hỏng dùng fixture có nhãn. Có anchor, Back/xoay, alias, source/share, toggle nhanh và process death có/mất mạng.
- Ca offline thực tế hiện connection-error, rồi retry tải lại đúng bài sau khi bật mạng. Database/preferences, xoay và Wi-Fi/dữ liệu di động đã đối chiếu khôi phục. Không phát sinh crash.

APK SHA-256: 042903ABA638931CAB7725B48FF05E7A4675AF8006EB570AF024F598DFEA01E5.

Bằng chứng local: build/member-integration-20261026/verified-build.log, unit-tests.json, host-tests.log, reader/report.json và ảnh cùng thư mục. Các lần build/test lỗi trước khi sửa tương thích vẫn giữ trong baseline-build.log và final-build.log; không coi chúng là kết quả PASS.

- Bộ TV1/TV4 đạt 12/12 nhóm kiểm tra: history order/clear/recovery, search và continuation thật, query/lifecycle/process death, SQLite bookmark và xác nhận xóa qua xoay. Phần fixture lịch sử dùng task sạch/dọc để tránh orientation override của instrumentation trước đó; đường launch giữ task cho process death vẫn được giữ. Không nới điều kiện kiểm tra dữ liệu.
- Settings/About đạt 2/2 kiểm tra qua cây UI thật, bốn bản ghi thành viên hiển thị đúng; nhãn cỡ chữ có đầy đủ bốn mức. Các màn reader/ngang/About/thư viện đã được xem qua screenshot, giữ nguyên giao diện đã có trên main.
- Tổng 47 kiểm tra của ba bộ có phần giao nhau, đều trên cùng APK. Build từ git archive sạch của e51e24a đạt trên cùng máy Windows, không có local.properties/output cũ. Chưa thay nghiệm thu máy thành viên khác.

Báo cáo đạt: build/member-integration-20261026/{tv1-final,reader,settings,combined}/report.json. Các lượt fixture lỗi ban đầu và bước sửa tham số adb vẫn giữ ở tv1/report.json và tv1-retest/report.json; dữ liệu đã được khôi phục sau mỗi lượt.

## Kiểm tra CI sau khi push

Lượt GitHub Actions 40354501611 ở commit 137809a đạt host test nhưng lỗi 1/143 unit test trên Ubuntu: LocalRepositoriesTest.dataSurvivesReopen báo `table bookmarks already exists`. Test kho dữ liệu khởi động WikiApplication, trong khi AppContainer tự xếp tác vụ đọc lịch sử và test cũng xóa/mở lại cùng database. Bản sửa dùng Application thuần cho riêng lớp test này để nó sở hữu trọn vòng đời SQLite; giữ nguyên assertion, executor tuần tự và hai SDK23/28. Không đổi cơ sở dữ liệu production và không bỏ qua test. Build/lint/unit local sau sửa đạt; log tại build/member-integration-20261026/ci-isolation-build.log. Kết quả CI của bản sửa được kiểm tra riêng trên GitHub Actions, không dùng lượt lỗi làm bằng chứng PASS.

## Việc còn lại và giới hạn

- TV5-QA-01 vẫn cần chạy lại trên Android17/API37: máy TV1 chỉ có AVD API36. Đã sửa cửa sổ reflow/resize có thể xóa vị trí và thêm hồi quy; ca xoay trên API36 đạt. Không dùng kết quả này để tuyên bố API37 đã được nghiệm thu.
- TV5: tên nhóm riêng/lớp nếu môn yêu cầu; rubric chính thức chưa có; nghiệm thu nghe tiếng Việt/chạm/focus bằng TalkBack và điện thoại thật; nhận phê duyệt nội dung, slide và lời thuyết minh của cả nhóm. Bốn tên/MSSV trong About đã có, không còn ghi chờ danh sách thành viên.
- TV1/TV3: phối hợp máy thành viên khác/điện thoại và API37, chốt bản nộp theo rubric; nhóm/TV1 quyết định giấy phép mã app. Chưa tự gán giấy phép.
- Các tài liệu TV5 mô tả video/slide/evidence ngoài Git. Chỉ đánh giá những tài liệu/mã hiện có trong commit và những kiểm tra đã tái chạy; không khẳng định đã nhận hoặc xem các media chưa được cung cấp.
