# WikiRead — Wikipedia Client

Ứng dụng Android phục vụ đồ án nhóm 5 thành viên, hướng tới tìm kiếm, đọc và quản lý bài viết Wikipedia tiếng Việt. Mã nguồn hiện tại là bản **prototype 0.1.0**: khung ứng dụng và UI lịch sử đã sẵn sàng; API tìm kiếm và reader thật chưa được tích hợp.

## Trạng thái chức năng

| Chức năng | Hiện trạng |
|---|---|
| Điều hướng | Bốn tab Khám phá, Yêu thích, Lịch sử, Cài đặt; mở màn hình bài mẫu và quay lại |
| Ô tìm kiếm | Nhập/xóa từ khóa, chọn chủ đề và giữ query khi tạo lại Activity; chưa gọi API tìm kiếm |
| Đọc bài | Hiển thị tên bài được chọn và lời hướng dẫn cục bộ; chưa tải nội dung Wikipedia |
| Liên kết bài | Mở trang tương ứng bằng trình duyệt; chia sẻ liên kết bằng Android Intent |
| Yêu thích | Danh sách minh họa, lọc thể loại và xóa từng thẻ; chưa lưu yêu thích thật |
| Lịch sử | Danh sách động từ kho lưu cục bộ, mới nhất trước; mở lại, xóa từng bài hoặc xóa tất cả có xác nhận; reader thật chưa gọi ghi nhận bài đã đọc |
| Cỡ chữ | Bốn mức, lưu bằng SharedPreferences và áp dụng cho preview lẫn reader mẫu |
| Loading, rỗng, lỗi | Có StatusView dùng chung; trạng thái lỗi mạng/thử lại chưa được tích hợp với API |

Thao tác xóa/lọc danh sách yêu thích mẫu được giữ qua việc tạo lại màn hình và Activity bằng SavedStateHandle, không cam kết còn sau force-stop. Dữ liệu lịch sử và cỡ chữ được lưu bền vững. Lịch sử bắt đầu trống vì bản mẫu chưa đọc bài thật; màn hình bài mẫu không tự tạo bản ghi giả. Nghe tóm tắt, lưu bài và xóa cache đang bị vô hiệu hóa vì chưa triển khai. Bản hiện tại chưa hỗ trợ đọc ngoại tuyến.

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
| Volley | 1.2.1; đã chuẩn bị RequestQueue, chưa nối API |
| Lưu thiết lập/lịch sử | SharedPreferences; lịch sử được xử lý trên Executor riêng |
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

Thay `emulator-5554` bằng serial thực tế. Script cần Python 3 và APK debug đã cài; kiểm tra UI bằng cây accessibility, lưu screenshot/JSON/log tại `build/tv1-device`. Script tạm đưa dữ liệu kiểm thử vào lịch sử rồi khôi phục dữ liệu ban đầu và thiết lập xoay trong `finally`; bản sao dự phòng nằm trong `history-backup.json` nếu tiến trình bị ngắt đột ngột. Để thiết bị mở khóa và không thao tác đồng thời trong lúc chạy test.

## Kiểm thử và CI

Ngày 21/10/2026, `assembleDebug`, `lintDebug` và `testDebugUnitTest` chạy thành công; **15/15 test đạt**, Lint không có lỗi chặn. APK đã cài và chạy trên emulator `medium_phone`, Android 16/API 36; **8/8 nhóm kiểm tra ADB đạt**. Bản sao mã nguồn sạch, không kèm `local.properties`, `.tools` hoặc output cũ, cũng build được trên cùng máy. Chưa xác nhận build trên máy khác.

Bộ [MainActivityTest](app/src/test/java/com/example/wikipediaclient/MainActivityTest.java) kiểm tra điều hướng/Back, giữ query và saved state, tính nhất quán của bài mẫu, nội dung Intent chia sẻ, lọc danh sách yêu thích mẫu, UI lịch sử, lưu cỡ chữ, icon thanh hệ thống và việc không áp dụng khoảng đệm bàn phím hai lần. [HistoryRepositoryTest](app/src/test/java/com/example/wikipediaclient/HistoryRepositoryTest.java) kiểm tra chống trùng, thứ tự, mở lại dữ liệu và xóa lịch sử không đụng đến dữ liệu khác.

Robolectric chạy API 28. Lần kiểm tra ADB hiện tại bao phủ lịch sử với fixture, mở bài/Back, xóa và khởi động lại, hủy/xác nhận xóa qua xoay máy, phục hồi dữ liệu lỗi, query qua xoay máy/đổi tab và khôi phục màn hình sau `am kill` khi app ở nền. Đã kiểm tra process dừng trước khi trở lại task. Các kết quả này áp dụng cho prototype; chưa bao phủ thiết bị thật, TalkBack, tablet hoặc API/WebView Wikipedia thật.

[Workflow Android build](.github/workflows/android.yml) chạy khi push lên `main` hoặc mở pull request, sử dụng JDK 17 và ba task `assembleDebug`, `lintDebug`, `testDebugUnitTest`. Xem kết quả từng lần chạy tại [GitHub Actions](https://github.com/xthien666/Wikipedia-Client/actions).

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
│   │   ├── remote/               # Package chuẩn bị cho API
│   │   └── repository/           # Hợp đồng HistoryRepository; chờ API và dữ liệu yêu thích
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

`AppContainer` cung cấp Volley RequestQueue dùng application context, Executor cho tác vụ blocking, ReaderSettings và HistoryRepository. SearchViewModel giữ query; SampleListViewModel chỉ còn phục vụ danh sách yêu thích mẫu. API và database chung của nhóm chưa được triển khai.

### Hợp đồng lịch sử để tích hợp reader

`HistoryRepository` nằm trong `data/repository`, hiện dùng `LocalHistoryRepository` để TV1 hoàn thiện UI độc lập. Sau khi reader hiển thị **thành công nội dung thật**, TV3 gọi `getHistoryRepository().record(new HistoryEntry("vi", resolvedPageId, resolvedTitle, canonicalUrl, System.currentTimeMillis()))`. Dùng metadata của response cuối sau redirect; không ghi khi chỉ mở màn hình mẫu hoặc khi tải bài lỗi. Màn hình lịch sử quan sát trạng thái loading/ready/error và có nút thử lại khi đọc lỗi. Dữ liệu chỉ là metadata; mở bài vẫn cần mạng. TV4 có thể thay implementation trong `AppContainer` bằng DB nhưng giữ interface này hoặc cập nhật bên dùng cùng một thay đổi.

## Phân công và tích hợp

| Thành viên | Phần phụ trách |
|---|---|
| TV1 | Nền tảng, Gradle/manifest, navigation, common UI, UI lịch sử và tích hợp |
| TV2 | API, model/mapping mạng và tìm kiếm |
| TV3 | Reader, HTML/WebView, liên kết và trạng thái bài |
| TV4 | Database, repository yêu thích/lịch sử và UI yêu thích |
| TV5 | Settings, nguồn/chia sẻ và kiểm thử tích hợp |

Mỗi phần hiện có giao diện mẫu hoặc điểm tích hợp để chủ feature phát triển tiếp. Khi nối API, dùng metadata của bài đã resolve cho nội dung, yêu thích, lịch sử, nguồn và chia sẻ. Không truyền HTML dài qua navigation; không giữ Activity hoặc View trong ViewModel. HTTP/database cần nằm trong lớp dữ liệu, công việc blocking chạy ngoài main thread.

Tạo nhánh ngắn theo phần việc, chẳng hạn `feature/search` hoặc `feature/article`. Trước pull request, chạy ba task build/lint/test ở trên và ghi rõ kiểm tra thiết bị đã thực hiện. Thống nhất thay đổi Gradle, manifest, navigation và dependency dùng chung với TV1; thay đổi schema dữ liệu với TV4.

Không commit SDK/JDK cục bộ, `local.properties`, output build, khóa ký, token hoặc khóa API. README này là tài liệu chung trên GitHub; kế hoạch, ghi chép và ảnh kiểm thử cá nhân chưa được đưa vào repository.

## Phần triển khai tiếp theo

- Nối tìm kiếm và tải nội dung Wikipedia tiếng Việt thật; xử lý phân trang, lỗi mạng và response hết hiệu lực.
- Thay reader mẫu bằng HTML/WebView; giữ nội dung và metadata cùng bài khi chuyển link, redirect và Back.
- Nối reader thật với `HistoryRepository.record()` sau khi hiển thị thành công; tích hợp repository/database yêu thích của TV4.
- Tích hợp nguồn/giấy phép theo metadata, cỡ chữ hiện có và các ca kiểm thử lifecycle trên reader thật.

Ứng dụng là đồ án học tập của nhóm, không phải ứng dụng chính thức của Wikimedia. Bản mẫu chưa tải nội dung Wikipedia; giấy phép nội dung cần được xác định và hiển thị đúng khi tích hợp dữ liệu thật.
