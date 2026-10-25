# TV5 - Hồ sơ bàn giao ngày 23/10/2026

Nền Git: `bbdf0c481ee872abb1bf4c13cdb72e1db70cca32` cộng thay đổi TV5 chưa commit/push. APK: `WikiRead-TV5-debug.apk`.
SHA-256: `f16bc22ed9bb6fdae8b47135c0a191728b0b3d9f70634c245f3c79aa05cb823d`.

Đã build `assembleDebug lintDebug testDebugUnitTest`: thành công; 79/79 unit test đạt, không lỗi/bỏ qua. Lint: 0 lỗi, 82 cảnh báo. Bằng chứng XML/HTML nằm trong `verification/`.

Thiết bị: Medium_Phone_API_37.0, emulator-5554, Android 17/API 37, 1080x2400, 420 dpi. WebView com.google.android.webview 145.0.7632.218. Người chạy: Codex hỗ trợ TV5, danh tính TV5 chưa được cung cấp. Test tự động dùng Robolectric API 28; build trên JBR 25.0.3 của Android Studio, nguồn Java 17. Chưa xác nhận build trên máy thứ hai.

## Phần TV5 đã thực hiện

- Tách listener nguồn/chia sẻ/giấy phép thành `ArticleSourceController` và `ExternalActions`, dùng DTO `ArticleContent` hiện tại. `ArticleFragment` chỉ nối trạng thái hiển thị. TV3 tiếp tục sở hữu HTML, Back/lifecycle, bookmark và vị trí đọc.
- Khi HTML chưa commit hoặc có lỗi, xóa metadata cũ và khóa thao tác. Bind metadata mới cùng lúc; giải phóng listener khi hủy view. Test alias/Back không chia sẻ metadata bài cũ.
- About có link Wikipedia, điều khoản Wikimedia, GitHub; phân biệt giấy phép nội dung và mã ứng dụng. Tên nhóm, họ tên/MSSV/lớp ghi rõ chờ cung cấp. Không tự điền.
- Giữ 4 mức chữ đang có: 14/16/18/22 sp; mặc định 16. SharedPreferences lưu lựa chọn, preview và reader dùng cùng cỡ. Đây là lựa chọn của bản bàn giao; nhóm cần đối chiếu rubric với kế hoạch gốc 3 mức.
- Tăng vùng chạm giấy phép tối thiểu 48dp và nhãn trợ năng có tên bài/cỡ chữ. Đã xem trực tiếp tiếng Việt ở 22 sp và chữ hệ thống 150% trên emulator.

## Bằng chứng thao tác thật

`03/04`: đổi mức chữ. `06`: About. `09`: HTML thật, giấy phép và revision. `10`: bảng chia sẻ canonical A. `12`: Bài gốc mở Chrome nhưng dừng ở màn thiết lập. `14–18`: A là Văn Miếu - Quốc Tử Giám, B là bài phường, chia sẻ B, Back về A. `19`: tắt mạng emulator, lỗi và retry. `21`: bật mạng và retry tải Hà Nội thành công. `23`: mức 22 còn sau force-stop/mở lại. `24`: chữ hệ thống 150%. Mạng/cỡ chữ hệ thống đã khôi phục như ban đầu.

Video `TV5-demo.mp4` là màn hình thao tác thật khoảng 90 giây, chưa có thuyết minh. Không dùng video này để chứng minh các ca ngoài những thao tác nhìn thấy. Ảnh PNG và cây UI JSON trong `evidence/` ghi trạng thái enabled/selected và content-description.

## Ma trận nghiệm thu T01–T16

Đối chiếu mục 8 của `planmobile.docx`. Đây là kết quả hiện có; không tuyên bố toàn bộ T01–T16 đã đạt. Mã ảnh là tiền tố tên trong `evidence/`.

| Mã | Tình huống | Kết quả | Thực tế | Bằng chứng | Còn cần làm | Owner |
|---|---|---|---|---|---|---|
| T01 | Cài mới; tìm Hà Nội | Đạt một phần | Cài cập nhật, mở bài thật Văn Miếu và Hà Nội qua link. Chưa chạy cài mới + nhập đúng query Hà Nội. | 09, 21; WikipediaApiTest | Cài mới trên Android khác; tìm Hà Nội và mở kết quả. | TV5 |
| T02 | Tải thêm rồi đổi query | Đạt tự động | SearchViewModelTest: paging/chống trùng/reset/response tải thêm cũ. | SearchViewModelTest (11 ca) | Chạy tải nhiều trang trên API thật. | TV2 |
| T03 | A về sau query B | Đạt tự động | Fake điều khiển thứ tự response; A cũ không ghi đè B. | newQueryCancelsOldRequestAndLateResultIsIgnored | Chạy chéo trên APK cuối cùng. | TV2, TV5 |
| T04 | Rỗng/không kết quả | Đạt tự động | Query rỗng hủy request; empty/error state có test. | clearingQueryReturnsToIdleAndCancelsRequest; successEmptyAndErrorStates | Xác nhận UI với query không có kết quả trên API thật. | TV5 |
| T05 | Mất mạng/timeout/404/giới hạn | Đạt phần đã chạy | Mất mạng thật trên emulator: báo lỗi, retry hồi phục. Fake timeout/JSON/404/429/503: không crash, không tự retry HTTP bị giới hạn. | 19, 21; Tv5AcceptanceTest (3 ca t05) | Chạy lại trên điện thoại Android thật; không gây tải lỗi lên Wikimedia. | TV2, TV5 |
| T06 | Bài dài/bảng/ảnh/chú thích | Chưa đủ nghiệm thu | HTML/ảnh thật hiển thị, cỡ 22 sp đọc được. Chưa đo bảng ngang, ảnh lỗi và anchor trên thiết bị thật. | 09, 17, 21; ArticleLinksTest | TV3 chạy đủ các bài biên và kiểm tra không ANR. | TV3 |
| T07 | A, B, chia sẻ B, Back | Đạt một phần | A là di tích, B là phường: chia sẻ B đúng, Back về A đúng metadata. Unit test HTML/share/history đồng bộ. | 14–18; MainActivityTest | Kiểm tra bookmark B, dung sai vị trí cuộn và Back lần hai. | TV1, TV3; TV5 kiểm tra nguồn |
| T08 | Xoay khi tìm/tải/đọc | Đạt tự động một phần | Có test recreate giữ query/article/cỡ chữ và không reload content. Chưa xoay trực tiếp với request mạng thật ở cả 3 thời điểm. | MainActivityTest; ArticleViewModelTest | Chạy thao tác xoay và so vị trí/DB trên APK. | TV1, TV5 |
| T09 | Bấm bookmark nhanh; mở lại | Đạt tự động một phần | Test serialize toggle, retry ghi lỗi, SQLite và lưu bền. Chưa thao tác nhanh/force-stop qua UI. | ArticleViewModelTest; PersonalStorageTest | TV4 chạy nhanh trên thiết bị và kiểm tra không trùng. | TV4 |
| T10 | A, B, A; xóa history | Đạt tự động | History unique/order/persistence; xóa history không xóa bookmark. | HistoryRepositoryTest (6); MainActivityTest | TV1/TV4 chạy UI thật và xác nhận dữ liệu. | TV1, TV4 |
| T11 | Đổi cỡ chữ; đóng/mở | Đạt trên emulator và tự động | 14/16/18/22 lưu đúng. Chọn 22, force-stop/mở lại vẫn chọn 22; HTML thật và chữ hệ thống 150% hiển thị. | 03, 04, 09, 23, 24; test fontPreferenceSurvivesAColdLaunchAndAppliesToReader | Điện thoại Android thật, vùng chạm và TalkBack. | TV3, TV5 |
| T12 | Nguồn/chia sẻ sau chuyển bài | Đạt một phần | Canonical B đúng; Back metadata A đúng. Bài gốc mở Chrome. Test Intent nguồn/giấy phép đúng. Chrome đang ở màn thiết lập ban đầu. | 10, 12, 15–18; Tv5AcceptanceTest | Hoàn tất Chrome bằng người dùng, xác nhận trang nguồn/giấy phép tải được. | TV5 |
| T13 | Clone/build trên máy khác | Đạt một phần | Clone sạch bbdf0c4 trên cùng máy, áp TV5, build/cài emulator thành công. Không phải máy thứ hai. | verification/build-metadata.json; 01 | TV1 và người khác clone/build trên máy khác. | TV1 + người khác |
| T14 | Alias rồi tên đích | Đạt tự động một phần | Fixture alias trả canonical/giấy phép bài đích, Back đúng. Mapper và định danh đã có test. | aliasAndBackKeepCanonicalSourceAndLicenseAligned | TV2/TV4 kiểm tra alias thật và không trùng bookmark/history. | TV2, TV4 |
| T15 | Nền; hệ thống hủy process | Chưa chạy trên thiết bị | Có test state nhỏ, Activity mới và reload sau process death. Force-stop của T11 không thay thế ca này. | progressLivesInSmallSavedStateAndNewModelCanReloadAfterProcessDeath | Home, hủy process nền bằng adb, trở lại task; thử có/mất mạng. | TV1, TV5 |
| T16 | Link ngoài/URL không hỗ trợ | Đạt tự động một phần | Test phân loại link, chặn scheme/file/credentials, xử lý không có app ngoài. Chưa chạy toàn bộ ma trận URL trên emulator. | ArticleLinksTest (5); Tv5AcceptanceTest | TV3/TV5 mở link ngoài, khác ngôn ngữ, namespace và URL lỗi trực tiếp. | TV3, TV5 |

## Lỗi/việc còn chờ

- **TV5-01 (Chờ dữ liệu)**: Tên nhóm, họ tên/MSSV/lớp chưa có. Nhóm cung cấp; TV5 điền và chụp lại.
- **TV5-02 (Chờ thiết bị)**: Chưa nghiệm thu chạm thật/TalkBack. Mượn Android; TV5 chạy checklist.
- **TV5-03 (Môi trường)**: Chrome hiện màn thiết lập, chưa xác nhận tải trang. Người dùng hoàn tất thiết lập; TV5 kiểm tra URL/giấy phép.
- **TV5-04 (Chờ chủ sở hữu)**: About ghi rõ mã app chưa công bố giấy phép. Nhóm/TV1 quyết định giấy phép, TV5 cập nhật About.
- **QA-01 (Chờ nghiệm thu)**: 79 unit test không thay toàn bộ nghiệm thu. Từng owner chạy và gửi bằng chứng.
- **QA-02 (Cảnh báo kỹ thuật)**: 0 lỗi, 82 cảnh báo; có OldTargetApi. TV1 điều phối rà cảnh báo; không đổi SDK trong phạm vi TV5.
- **DOC-01 (Chờ tổng hợp)**: Chưa nhận phần viết, ảnh và đóng góp TV1–TV4. Mỗi thành viên gửi nội dung; TV5 ghép bản cuối.

## Giao diện tích hợp cho TV3

```java
sourceActions = new ArticleSourceController(context, share, original, license);
sourceActions.bind(content, committed); // đúng ArticleContent đã commit lên WebView
sourceActions.bind(null, false);        // đang tải/lỗi, không giữ metadata cũ
sourceActions.release();               // onDestroyView
```

TV5 không đổi API/repository/DB hoặc viết lại reader. File `MainActivityTest` chỉ sửa helper chờ executor: drain main looper trước barrier và giới hạn chờ 10 giây; các assertion giữ nguyên. Không gửi HTML qua component. License thiếu thì hiện thông báo rõ và không giữ link bài trước. Nút share mở Android Sharesheet; người dùng tự chọn nơi nhận.

## Phần chờ trước khi nộp chính thức

1. Nhóm cung cấp thông tin About thật và đối chiếu lựa chọn bốn mức chữ với rubric.
2. TV5 mượn Android thật để kiểm tra tiếng Việt, chạm, chữ lớn, TalkBack; iPhone không chạy APK Android.
3. Chủ từng feature hoàn tất phần còn lại T01–T16; TV1 có người build trên máy khác.
4. TV1–TV4 gửi phần báo cáo và bằng chứng của mình. TV5 ghép báo cáo/slide nhóm, bổ sung tên, thuyết minh và video đầy đủ.
5. Nhóm review thay đổi rồi commit/push. Bản này chưa tự gửi GitHub hoặc nộp bài.

## Tham khảo kiểm tra được

- https://developer.android.com/guide/topics/ui/accessibility/views/apps-views
- https://developer.android.com/training/basics/intents/sending
- https://foundation.wikimedia.org/wiki/Policy:Terms_of_Use
- https://creativecommons.org/licenses/by-sa/4.0/
- https://github.com/xthien666/Wikipedia-Client

Giấy phép nội dung hiển thị theo metadata bài; nguồn ảnh/tác giả và lịch sử sửa đổi xem từ bài gốc. Repository hiện chưa công bố giấy phép mã nguồn ở commit nền.


## Review và tích hợp ngày 25/10/2026

TV1/TV3 đã review commit `a00449e3c6b9514258fd4926d976b67b08bde1dc` từ `feature/tv5-settings` và hợp nhất local bằng merge commit `862e8cf`, giữ lịch sử tác giả. Bản TV5 nguyên trạng build được bằng JDK 17, assembleDebug/assembleDebugAndroidTest/lintDebug đạt và 79/79 unit test không lỗi/bỏ qua. Các ảnh/video/verification mô tả trong hồ sơ bàn giao gốc không nằm trong commit; không coi đó là kết quả đã tái chạy tại máy TV1.

TV1/TV3 nối tiếp bằng cách ghi định danh canonical vào back stack sau khi HTML hiển thị; mở lại tên đích/ID sau alias không đẩy màn hình trùng, cả sau recreate. SavedStateHandle dùng title/ID canonical để tải lại. Bổ sung ba ca hồi quy: tổng 82/82 unit test đạt; hai test host bảo vệ dữ liệu khi backup thất bại đạt. Các mục chờ thông tin nhóm, điện thoại thật/TalkBack và lựa chọn giấy phép mã nguồn vẫn cần chủ sở hữu cung cấp/xác nhận.


Bản tích hợp đã chạy reader trên emulator Android 16/API 36, WebView 133.0.6943.137: 32/32 kiểm tra đạt. Bằng chứng ở máy TV1: `build/tv5-integration-20261025/reader/report.json` và các ảnh cùng thư mục. APK SHA-256: `81D1AB97725CA8BB6B44AEA7ACBCEBDFB3BEBF88F4859B8779E705F8F8A8AF57`.

| Hạng mục đối chiếu hồ sơ gốc | Bằng chứng tái kiểm chứng |
|---|---|
| T06/T07/T14/T16 | Bài thật Hà Nội/Hệ Mặt Trời/Hà Thành, chú thích, Back/định danh, external routing; bảng rộng/ảnh hỏng bằng fixture có nhãn. |
| T08 | Xoay khi đang đọc, vị trí sai lệch không quá một viewport. Chưa coi đây là đủ tất cả thời điểm request đang tải. |
| T12 | Sáu assertion trên thiết bị cho article B và Back A: native Sharesheet dùng URL canonical, Bài gốc/giấy phép đúng metadata. Intent được chặn trước khi mở app ngoài; không tuyên bố đã tải trang Chrome hay gửi chia sẻ. |
| T15 + T05 | Process thật bị hủy ở nền; khi offline reader báo lỗi, tắt thao tác nội dung cũ; bật mạng rồi retry phục hồi đúng bài. Dữ liệu/mạng/rotation khôi phục và không crash. |

Các kết quả này bổ sung ma trận ngày 23/10, không thay nghiệm thu điện thoại, thông tin About thật hoặc hồ sơ báo cáo/slide/video nhóm cuối cùng.
