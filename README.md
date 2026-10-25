# WikiRead — Wikipedia Client

Ứng dụng Android phục vụ đồ án nhóm 5 thành viên, hướng tới tìm kiếm, đọc và quản lý bài viết Wikipedia tiếng Việt. Mã nguồn hiện tại là bản **prototype 0.1.0**: đã tích hợp tìm kiếm Wikipedia thật, reader toàn bài HTML/WebView, kho SQLite và UI yêu thích/lịch sử.

## Trạng thái chức năng

| Chức năng | Hiện trạng |
|---|---|
| Điều hướng | Bốn tab Khám phá, Yêu thích, Lịch sử, Cài đặt; Back giữa các bài; bỏ mở lặp cùng `pageId` dù tiêu đề khác |
| Ô tìm kiếm | API Wikipedia tiếng Việt, snippet dạng chữ, debounce và phím tìm kiếm, tải thêm có chống trùng; hủy/bỏ qua response cũ và giữ query qua lifecycle |
| Đọc bài | HTML toàn bài qua REST; bảng cuộn ngang, ảnh và chú thích; lỗi/mất mạng/thử lại; JavaScript và truy cập file tắt |
| Liên kết bài | Bài tiếng Việt qua reader, anchor ở lại trong bài, link ngoài/ngôn ngữ khác/trang chức năng mở trình duyệt; chia sẻ URL canonical |
| Yêu thích | Kho SQLite bền vững, danh sách thật, mở/xóa có xác nhận; nút thêm/bỏ trong reader dùng ID đã resolve; bộ lọc thể loại chưa có metadata nên bị vô hiệu hóa |
| Lịch sử | Danh sách động từ kho lưu cục bộ, mới nhất trước; mở lại, xóa từng bài hoặc xóa tất cả có xác nhận; chỉ ghi sau khi WebView hiển thị nội dung thành công; Back/xoay máy giữ bài và vị trí đọc |
| Cỡ chữ | Bốn mức, lưu bằng SharedPreferences và áp dụng cho preview và HTML reader |
| Loading, rỗng, lỗi | StatusView cho tìm kiếm, reader, yêu thích và lịch sử; tìm kiếm có retry khi lỗi và retry tải thêm mà giữ kết quả đã tải |

Dữ liệu yêu thích/lịch sử được lưu trong hai bảng SQLite riêng, khóa duy nhất `language + pageId`; cỡ chữ được lưu bằng SharedPreferences. Lịch sử cũ của TV1 được chuyển sang SQLite khi đọc kho lần đầu, giữ bản cũ nếu chưa chuyển thành công. Yêu thích/lịch sử bắt đầu trống trên cài mới, được cập nhật bằng metadata bài thật từ reader. Lưu yêu thích chỉ lưu liên kết, không tải nội dung offline. Xóa cache vẫn chưa triển khai; reader không hiển thị nút nghe tóm tắt. Bản hiện tại chưa hỗ trợ đọc ngoại tuyến.

## Công nghệ và cấu hình

Các phiên bản dưới đây lấy từ cấu hình build trong repository.

| Thành phần | Phiên bản/cấu hình |
|---|---|
| Ngôn ngữ, UI | Java 17, XML, ViewBinding, Material Components |
| JDK chạy Gradle | 17 |
| Gradle Wrapper | 9.6.0 |
| Android Gradle Plugin | 9.4.1 |
| compileSdk / targetSdk | 36 / 36 |
| minSdk | 23 |
| AppCompat | 1.7.1 |
| Material Components | 1.13.0 |
| Navigation Component | 2.9.7 |
| Lifecycle | 2.9.4 |
| Volley | 1.2.1; Action API tìm kiếm, REST summary và REST `with_html` toàn bài |
| Lưu dữ liệu cá nhân | SQLiteOpenHelper cho yêu thích/lịch sử; SharedPreferences cho cỡ chữ; truy cập DB trên Executor riêng |
| Kiểm thử | JUnit 4.13.2, Robolectric 4.17; suite chạy API 28 |

Module ứng dụng: `app`. Namespace và application ID: `com.example.wikipediaclient`.

## Clone và chạy bằng Android Studio

Chuẩn bị Git, JDK 17 và Android Studio có thể đồng bộ project với cấu hình Gradle/AGP nêu trên. Trong SDK Manager, cài Android SDK Platform 36, Build Tools 36.0.0 và Platform Tools; chấp nhận license trên máy của bạn.

```bash
git clone https://github.com/xthien666/Wikipedia-Client.git
cd Wikipedia-Client
```

1. Mở thư mục `Wikipedia-Client` bằng Android Studio.
2. Trong Settings → Build, Execution, Deployment → Build Tools → Gradle, chọn Gradle JDK 17 và dùng Gradle Wrapper của project.
3. Chọn Android SDK đã cài trên máy, để Android Studio tạo `local.properties`, rồi chờ Gradle sync hoàn tất.
4. Chọn điện thoại bật USB debugging hoặc emulator API 23 trở lên, rồi chạy module `app`.

Cần kết nối mạng khi tải Gradle, dependency và SDK. `local.properties`, thư mục `.tools` và các thư mục build không nằm trong repository; mỗi thành viên cấu hình JDK/SDK của máy mình.

## Build và kiểm tra bằng dòng lệnh

Chạy từ thư mục gốc project. Đặt `JAVA_HOME` trỏ tới JDK 17; cấu hình SDK bằng `local.properties` do Android Studio tạo hoặc biến môi trường `ANDROID_HOME`.

Windows — PowerShell:

```powershell
.\gradlew.bat --no-daemon assembleDebug lintDebug testDebugUnitTest
```

Linux/macOS hoặc Git Bash:

```bash
./gradlew --no-daemon assembleDebug lintDebug testDebugUnitTest
```

Nếu shell báo thiếu quyền chạy wrapper trên Linux/macOS, chạy `chmod +x gradlew` rồi thử lại. Project đã kèm wrapper; không cần cài Gradle toàn cục.

Có thể dùng script PowerShell hỗ trợ build local:

```powershell
.\tools\build-local.ps1 -Tasks assembleDebug,lintDebug,testDebugUnitTest
```

Script dùng `JAVA_HOME` và cấu hình SDK hiện có trong `local.properties`/biến môi trường; không ghi đè SDK Android Studio đã chọn. JDK/SDK portable trong `.tools` chỉ là phương án dự phòng. Khi clone mới, mỗi người tự cấu hình JDK và SDK.

Các đầu ra sau khi chạy:

| Đầu ra | Đường dẫn |
|---|---|
| APK debug | `app/build/outputs/apk/debug/app-debug.apk` |
| Báo cáo Android Lint | `app/build/reports/lint-results-debug.html` |
| Báo cáo unit test | `app/build/reports/tests/testDebugUnitTest/index.html` |

APK và báo cáo được tạo ở máy build, không được lưu trong Git.

### Đóng gói bản debug theo commit

Sau khi commit phần mã nguồn dùng chung, chạy:

```powershell
.\tools\package-debug.ps1
# Kèm kết quả thiết bị đã chạy trên đúng APK:
.\tools\package-debug.ps1 -DeviceReport build/tv1-device/report.json
```

Script yêu cầu working tree sạch đối với các file Git theo dõi và file mới chưa bị ignore; chạy build/lint/test rồi tạo `build/handoff-<commit>-<thời gian>.zip`. Gói gồm APK, `source.zip` lấy từ đúng commit, README, báo cáo, manifest và `SHA256SUMS.txt`. Kết quả thiết bị chỉ được nhận khi PASS, đã khôi phục dữ liệu và SHA-256 khớp APK. Gói không phụ thuộc tài liệu kế hoạch, SDK/JDK hoặc cấu hình cá nhân trên máy. Đây là bản debug phục vụ tích hợp; manifest ghi phạm vi tìm kiếm, reader và kho dữ liệu cá nhân.

### Cài APK bằng ADB

Thêm thư mục `platform-tools` của SDK vào PATH hoặc chạy ADB trực tiếp từ thư mục đó. Kết nối thiết bị và chấp nhận yêu cầu USB debugging trên điện thoại.

```bash
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Chạy test trực tiếp trên emulator hoặc điện thoại

Trong Android Studio, chọn thiết bị ở thanh công cụ rồi bấm Run. Có thể mở Device Manager để khởi động một AVD có sẵn. Emulator được tạo bằng Android Studio cũng xuất hiện trong `adb devices` và có thể chạy kiểm thử qua ADB. [Hướng dẫn emulator](https://developer.android.com/studio/run/emulator).

Với điện thoại: bật Developer options → USB debugging, cắm cáp USB có truyền dữ liệu và chấp nhận hộp thoại cấp quyền trên điện thoại. `adb devices` cần hiện trạng thái `device`; nếu là `unauthorized`, mở khóa điện thoại và xác nhận quyền. [Hướng dẫn thiết bị thật](https://developer.android.com/studio/run/device).

Trong Terminal PowerShell của Android Studio, tại thư mục project:

```powershell
$adb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
& $adb devices -l
& $adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
python tools/test-tv1-device.py --adb $adb --serial emulator-5554
```

Thay `emulator-5554` bằng serial thực tế. Script cần Python 3, APK debug đã cài và mạng tới Wikipedia; kiểm tra UI bằng cây accessibility, lưu screenshot/JSON/log tại `build/tv1-device`. Script tạm đưa fixture vào lịch sử và SQLite rồi khôi phục database, preferences và thiết lập xoay trong `finally`; bản sao dự phòng nhị phân được mã hóa base64 trong `history-backup.json` nếu tiến trình bị ngắt đột ngột. Để thiết bị mở khóa và không thao tác đồng thời trong lúc chạy test.

## Kiểm thử và CI

Ngày 25/10/2026, bản tích hợp TV1/TV2/TV3/TV4/TV5 chạy thành công `assembleDebug`, `lintDebug` và `testDebugUnitTest`; **82/82 test đạt**, Lint không có lỗi chặn. Bộ kiểm tra bao phủ JSON/API, response cũ, phân trang chống trùng, UI/lifecycle, tính bền vững của SQLite, thử lại thao tác ghi/xóa lỗi và nhận diện bài trong back stack theo pageId. GitHub Actions [run 6](https://github.com/xthien666/Wikipedia-Client/actions/runs/39803121581) đã build/test bản tích hợp TV1/TV3 trên Ubuntu; chưa xác nhận cài/chạy trên điện thoại thật hoặc máy của thành viên khác.

Hai test host `python -m unittest discover -s tools -p test_device_backup.py` kiểm tra script không xóa/ghi đè dữ liệu khi sao lưu thất bại giữa chừng; workflow CI cũng chạy hai ca này.

Bộ [MainActivityTest](app/src/test/java/com/example/wikipediaclient/MainActivityTest.java) kiểm tra điều hướng/Back, giữ query và saved state, HTML/metadata cùng bài khi A → B → Back, Intent chia sẻ canonical, lỗi WebView, thư viện rỗng, UI lịch sử, lưu cỡ chữ, icon thanh hệ thống và khoảng đệm bàn phím. [HistoryRepositoryTest](app/src/test/java/com/example/wikipediaclient/HistoryRepositoryTest.java) kiểm tra chống trùng, thứ tự và dữ liệu bền vững. [PersonalStorageTest](app/src/test/java/com/example/wikipediaclient/PersonalStorageTest.java) kiểm tra chuyển dữ liệu cũ, bảo vệ metadata mới, giữ bookmark khi xóa history và chặn truy cập DB trên main thread. Các test UI dùng repository tìm kiếm/bài viết giả; test thiết bị đi qua Volley/API thật. Suite reader còn kiểm tra response cũ, retry bookmark lỗi và saved state nhỏ sau process death.

Robolectric chạy API 28. Kiểm thử ADB dùng emulator `medium_phone`, Android 16/API 36 và fixture được gắn nhãn rõ ràng cho dữ liệu cá nhân. Reader đã được kiểm tra bằng WebView thực với bài Hà Nội, Hệ Mặt Trời và alias Hà Thành; fixture riêng kiểm tra bảng rộng và ảnh hỏng. Chưa bao phủ thiết bị thật, TalkBack hoặc tablet.

Ngày 22/10/2026, **11/11 nhóm kiểm tra ADB đạt** trên APK tích hợp: lịch sử đúng thứ tự/mở bài/Back; xóa bền vững; xác nhận xóa qua xoay máy; phục hồi JSON cũ bị lỗi; bookmark mở/xóa/hủy và giữ sau khởi động lại; xóa history giữ bookmark; tìm kiếm Wikipedia thật và tải thêm; giữ query qua xoay/Back/đổi tab; khôi phục đúng màn hình sau khi process thật sự dừng; không phát sinh crash. Script xác minh SHA-256 APK và khôi phục chính xác database/preferences trước khi mở lại app.

[Workflow Android build](.github/workflows/android.yml) chạy khi push lên `main`, mở pull request hoặc chọn Run workflow; dùng JDK 17 và ba task `assembleDebug`, `lintDebug`, `testDebugUnitTest`. Mỗi lần chạy tải lên báo cáo test/lint, kể cả khi một bước thất bại. Lần chạy thành công còn có artifact `wikiread-debug-<số lần chạy>` chứa APK. Mở [GitHub Actions](https://github.com/xthien666/Wikipedia-Client/actions), chọn lần chạy và tải tại Artifacts; file được giữ 7 ngày. APK debug giữa các máy có thể dùng khóa ký khác nhau nên không cam kết cài cập nhật đè lên bản hiện có.

## Cấu trúc mã nguồn

```text
app/src/main/
├── java/com/example/wikipediaclient/
│   ├── MainActivity.java          # Khung ứng dụng, navigation và insets
│   ├── WikiApplication.java       # Khởi tạo AppContainer
│   ├── data/
│   │   ├── AppContainer.java      # Dependency dùng chung
│   │   ├── model/                # ArticleLocator, HistoryEntry
│   │   ├── local/                # ReaderSettings, LocalHistoryRepository
│   │   ├── remote/               # Volley, JSON mapper và phân loại lỗi
│   │   └── repository/           # API tìm kiếm/summary/HTML, SQLite bookmark và history
│   └── ui/
│       ├── navigation/
│       ├── common/               # StatusView, SampleListViewModel
│       ├── search/
│       ├── article/
│       ├── library/
│       ├── history/
│       └── settings/
└── res/                          # Layout, navigation graph, màu và chuỗi
app/src/test/                     # Unit tests Robolectric
.github/workflows/                # Kiểm tra tự động trên GitHub
```

`AppContainer` cung cấp Volley RequestQueue dùng application context, Executor cho tác vụ blocking, ReaderSettings, SearchRepository, ArticleRepository, BookmarkRepository và HistoryRepository. SearchViewModel giữ query bằng SavedStateHandle; LibraryViewModel tải dữ liệu SQLite ngoài main thread. ArticleRepository trả summary và nội dung HTML toàn bài cùng metadata đã resolve.

### Hợp đồng lịch sử để tích hợp reader

`HistoryRepository` nằm trong `data/repository`, được nối với SQLite của TV4 qua lớp `data/local/LocalHistoryRepository`. Sau khi reader hiển thị **thành công nội dung thật**, TV3 gọi `getHistoryRepository().recordRead(new PersonalArticle("vi", resolvedPageId, resolvedTitle, canonicalUrl), System.currentTimeMillis())`. Lời gọi lịch sử này tự chạy trên disk executor; UI quan sát `observe()` để nhận loading/ready/error. Dùng metadata của response cuối sau redirect; không ghi khi chỉ mở màn hình mẫu, nhận summary hoặc tải bài lỗi. Các lời gọi BookmarkRepository vẫn là blocking và phải chạy trên `getDiskExecutor()`. Chi tiết hợp đồng chung: [TV4 integration](docs/tv4-integration.md).

`retry()` của lịch sử thực hiện lại thao tác ghi/xóa gần nhất bị lỗi, hoặc tải lại khi chỉ lỗi đọc. `refresh()` giữ thông báo thao tác ghi/xóa thất bại cho đến khi người dùng thử lại hoặc yêu cầu một thao tác ghi khác. Retry được giữ trong phiên tiến trình hiện tại; không lưu thao tác chưa thành công qua process death. Xóa toàn bộ vẫn cần xác nhận trước lần yêu cầu đầu tiên.

## Phân công và tích hợp

| Thành viên | Phần phụ trách |
|---|---|
| TV1 | Nền tảng, Gradle/manifest, navigation, common UI, UI lịch sử và tích hợp |
| TV2 | API, model/mapping mạng và tìm kiếm |
| TV3 | Reader, HTML/WebView, liên kết và trạng thái bài |
| TV4 | Database, repository yêu thích/lịch sử và UI yêu thích |
| TV5 | Settings, nguồn/chia sẻ và kiểm thử tích hợp |

Từ ngày 22/10/2026, cùng một người phụ trách cả TV1 và TV3; ký hiệu vai trò được giữ để đối chiếu kế hoạch và kiểm thử. Tìm kiếm, reader và lưu dữ liệu cá nhân đã tích hợp. Reader dùng metadata của bài đã resolve cho nội dung, yêu thích, lịch sử, nguồn và chia sẻ. Định danh đã hiển thị được lưu trong back stack để mở lại tên đích/ID sau alias không tạo màn hình trùng; locator để tải lại sau process death dùng title/ID canonical. Không truyền HTML dài qua navigation; không giữ Activity hoặc View trong ViewModel. HTTP/database cần nằm trong lớp dữ liệu, công việc blocking chạy ngoài main thread.

Phần TV1/TV3 hiện được tích hợp trực tiếp trên `main`; trước khi push, chạy ba task build/lint/test ở trên và ghi rõ kiểm tra thiết bị đã thực hiện. Các phần khác có thể dùng nhánh ngắn và pull request theo quy trình nhóm. Thống nhất thay đổi Gradle, manifest, navigation và dependency dùng chung với TV1; thay đổi schema dữ liệu với TV4.

Không commit SDK/JDK cục bộ, `local.properties`, output build, khóa ký, token hoặc khóa API. README này là tài liệu chung trên GitHub; kế hoạch, ghi chép và ảnh kiểm thử cá nhân chưa được đưa vào repository.

## Reader TV3: luồng đọc và kiểm chứng

`RemoteArticleRepository.getContent()` tải JSON `/w/rest.php/v1/page/{title}/with_html`. `ArticleContentMapper` giữ HTML, ID/title/key đã resolve, revision và giấy phép trong cùng `ArticleContent`. ViewModel giữ đối tượng này trong phiên; `SavedStateHandle` chỉ chứa locator và tỷ lệ cuộn, không chứa HTML/WebView/Activity. Sau process death, app tải lại nội dung hoặc hiển thị lỗi kết nối.

`ArticleFragment` dùng `loadDataWithBaseURL()` và navigation native cho A → B → Back. Liên kết nội bộ dùng chung ArticleNavigator để bỏ mở trùng khi chạm nhanh. WebView chỉ điều hướng trong tài liệu với anchor; URL không hỗ trợ bị chặn. Sau callback hiển thị thành công, reader ghi lịch sử bằng ID cuối cùng và bật các thao tác cho đúng bài. Yêu thích chạy trên disk executor; lỗi ghi và thao tác thử lại được giữ khi reader tạm dừng/tiếp tục; cỡ chữ lấy từ ReaderSettings. Back/xoay máy phục hồi tỷ lệ cuộn và kiểm tra sai lệch tối đa một màn hình; người dùng chạm sẽ dừng tự phục hồi vị trí.

Bộ nghiệm thu WebView thực chạy độc lập, cần cài cả APK app và APK androidTest:

```powershell
.\tools\build-local.ps1 -Tasks assembleDebug,assembleDebugAndroidTest,lintDebug,testDebugUnitTest
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
python tools/test-tv3-device.py --adb adb --serial emulator-5554
# Thêm ca mất mạng khi process bị hủy, chỉ dành cho emulator:
python tools/test-tv3-device.py --adb adb --serial emulator-5554 --offline-process-death
```

Script lưu báo cáo JSON, ảnh, crash log, SHA-256 APK và bản sao dữ liệu tại `build/tv3-device`. Nó kiểm tra bài thật/alias, Back, xoay máy, anchor chú thích, nguồn link ngoài, cỡ chữ, lưu đúng bài và process death thật; fixture riêng mô phỏng bảng rộng/ảnh hỏng. Script sao lưu rồi khôi phục database/preferences và thiết lập xoay trong `finally`. Tùy chọn `--offline-process-death` còn sao lưu/khôi phục Wi-Fi và dữ liệu di động; kiểm tra reader khôi phục bài từ HTTP cache nếu có, hoặc báo lỗi có thử lại. Báo cáo ghi rõ nhánh thực tế đã chạy, không coi HTTP cache là cam kết đọc ngoại tuyến. Không thao tác đồng thời trên thiết bị khi chạy.

Ngày 22/10/2026: **23/23 kiểm tra emulator đạt**, Android 16/API 36, WebView 133.0.6943.137; database/preferences đã được đối chiếu khôi phục đúng từng byte. Không phát sinh crash app. Ảnh và JSON chi tiết nằm tại máy build, không lưu trong Git. Tên/phiên bản bài có thể thay đổi khi chạy lại với Wikipedia đang hoạt động. Ca chú thích lấy ID từ HTML đang hiển thị và chờ WebView cuộn; không phụ thuộc mã chú thích cố định. Mỗi lần instrumentation chỉ xuất ảnh của chính lần chạy đó.

Chưa xác nhận trên điện thoại thật, TalkBack hoặc tablet. Bookmark không bảo đảm đọc ngoại tuyến; các tiện ích tương tác phụ thuộc JavaScript của Wikipedia nằm ngoài phạm vi. Ứng dụng là đồ án học tập, không phải ứng dụng chính thức của Wikimedia. Nguồn và giấy phép bài lấy từ response API và được hiển thị trong reader.


## Bàn giao TV5 ngày 23/10/2026

Component nguồn/chia sẻ ở `ui/source/ArticleSourceController`; About phân biệt nội dung Wikipedia và mã ứng dụng. Bản bàn giao giữ bốn mức 14/16/18/22 sp. Xem [hồ sơ và T01–T16](docs/TV5.md), [đặc tả cỡ chữ](docs/TV5-FONT-SPEC.md) và [kịch bản demo](docs/TV5-DEMO.md). Thông tin nhóm, Android thật/TalkBack và một số ca nghiệm thu còn chờ. Kết quả 79 unit test không có nghĩa toàn bộ T01–T16 đã đạt.


## Tích hợp TV5 và hoàn thiện TV1/TV3 — 25/10/2026

Nhánh `feature/tv5-settings` (commit `a00449e`) đã được review và hợp nhất, giữ lịch sử tác giả. Component nguồn/chia sẻ dùng đúng metadata đã commit; About có các liên kết nguồn/điều khoản/project, thông tin nhóm còn chờ cung cấp. Bộ nguyên trạng TV5 đạt 79 unit test; sau bổ sung hồi quy canonical identity của TV1/TV3, tổng 82 unit test và hai test host đạt. Build APK/AndroidTest và lint thành công.

Reader tích hợp đạt 32 kiểm tra trên emulator Android 16/API 36, WebView 133.0.6943.137: HTML/link/anchor/bảng/ảnh, Back và xoay, alias/định danh, cỡ chữ, bookmark/history, source/share, process death có và mất mạng. Nguồn/chia sẻ được xác minh bằng Intent native chặn trước khi mở app ngoài; chưa xác nhận trình duyệt tải trang hay gửi tới người nhận. Ca mất mạng thực tế hiện lỗi kết nối và retry tải lại đúng bài sau khi mạng trở lại; dữ liệu, xoay, Wi-Fi và dữ liệu di động đã khôi phục. Đây là kết quả emulator, chưa phải nghiệm thu điện thoại thật/TalkBack.
