# WikiRead — Wikipedia Client

Ứng dụng Android phục vụ đồ án nhóm 5 thành viên, hướng tới tìm kiếm, đọc và quản lý bài viết Wikipedia tiếng Việt. Mã nguồn hiện tại là bản **prototype 0.1.0**: đã tích hợp tìm kiếm Wikipedia thật, kho SQLite cho yêu thích/lịch sử và UI danh sách; reader đầy đủ vẫn đang chờ tích hợp.

## Trạng thái chức năng

| Chức năng | Hiện trạng |
|---|---|
| Điều hướng | Bốn tab Khám phá, Yêu thích, Lịch sử, Cài đặt; Back giữa các bài; bỏ mở lặp cùng `pageId` dù tiêu đề khác |
| Ô tìm kiếm | API Wikipedia tiếng Việt, snippet dạng chữ, debounce và phím tìm kiếm, tải thêm có chống trùng; hủy/bỏ qua response cũ và giữ query qua lifecycle |
| Đọc bài | Hiển thị tên bài được chọn và lời hướng dẫn cục bộ; chưa tải nội dung Wikipedia |
| Liên kết bài | Mở trang tương ứng bằng trình duyệt; chia sẻ liên kết bằng Android Intent |
| Yêu thích | Kho SQLite bền vững, danh sách thật, mở/xóa có xác nhận; chờ nối nút lưu trong reader, bộ lọc thể loại chưa có metadata nên bị vô hiệu hóa |
| Lịch sử | Danh sách động từ kho lưu cục bộ, mới nhất trước; mở lại, xóa từng bài hoặc xóa tất cả có xác nhận; reader thật chưa gọi ghi nhận bài đã đọc |
| Cỡ chữ | Bốn mức, lưu bằng SharedPreferences và áp dụng cho preview lẫn reader mẫu |
| Loading, rỗng, lỗi | StatusView cho tìm kiếm, yêu thích và lịch sử; tìm kiếm có retry khi lỗi và retry tải thêm mà giữ kết quả đã tải |

Dữ liệu yêu thích/lịch sử được lưu trong hai bảng SQLite riêng, khóa duy nhất `language + pageId`; cỡ chữ được lưu bằng SharedPreferences. Lịch sử cũ của TV1 được chuyển sang SQLite khi đọc kho lần đầu, giữ bản cũ nếu chưa chuyển thành công. Yêu thích và lịch sử bắt đầu trống vì reader chưa lưu/đọc bài thật; màn hình bài mẫu không tự tạo bản ghi giả. Nghe tóm tắt, nút lưu bài trong reader và xóa cache đang bị vô hiệu hóa vì chưa triển khai. Bản hiện tại chưa hỗ trợ đọc ngoại tuyến.

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
| Volley | 1.2.1; Action API tìm kiếm và REST summary |
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

Script yêu cầu working tree sạch đối với các file Git theo dõi và file mới chưa bị ignore; chạy build/lint/test rồi tạo `build/handoff-<commit>-<thời gian>.zip`. Gói gồm APK, `source.zip` lấy từ đúng commit, README, báo cáo, manifest và `SHA256SUMS.txt`. Kết quả thiết bị chỉ được nhận khi PASS, đã khôi phục dữ liệu và SHA-256 khớp APK. Gói không phụ thuộc tài liệu kế hoạch, SDK/JDK hoặc cấu hình cá nhân trên máy. Đây là bản debug phục vụ tích hợp; manifest ghi rõ reader đầy đủ còn đang chờ.

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

Ngày 22/10/2026, bản tích hợp TV1/TV2/TV4 chạy thành công `assembleDebug`, `lintDebug` và `testDebugUnitTest`; **46/46 test đạt**, Lint không có lỗi chặn. Bộ kiểm tra bao phủ JSON/API, response cũ, phân trang chống trùng, UI/lifecycle, tính bền vững của SQLite, thử lại thao tác ghi/xóa lỗi và nhận diện bài trong back stack theo pageId. GitHub Actions đã build/test bản tích hợp trước đó thành công trên Ubuntu; chưa xác nhận cài/chạy trên điện thoại thật hoặc máy của thành viên khác.

Bộ [MainActivityTest](app/src/test/java/com/example/wikipediaclient/MainActivityTest.java) kiểm tra điều hướng/Back, giữ query và saved state, tính nhất quán của bài mẫu, Intent chia sẻ, thư viện rỗng, UI lịch sử, lưu cỡ chữ, icon thanh hệ thống và khoảng đệm bàn phím. [HistoryRepositoryTest](app/src/test/java/com/example/wikipediaclient/HistoryRepositoryTest.java) kiểm tra chống trùng, thứ tự và dữ liệu bền vững. [PersonalStorageTest](app/src/test/java/com/example/wikipediaclient/PersonalStorageTest.java) kiểm tra chuyển dữ liệu cũ, bảo vệ metadata mới, giữ bookmark khi xóa history và chặn truy cập DB trên main thread. Các test UI dùng repository tìm kiếm giả; test ADB mới đi qua Volley/API thật.

Robolectric chạy API 28. Kiểm thử ADB dùng emulator `medium_phone`, Android 16/API 36 và fixture được gắn nhãn rõ ràng cho dữ liệu cá nhân. Reader vẫn là prototype; kiểm thử mở bài xác nhận đúng metadata và điều hướng, chưa xác nhận HTML/WebView toàn bài. Chưa bao phủ thiết bị thật, TalkBack hoặc tablet.

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
│   │   ├── remote/               # Package chuẩn bị cho API
│   │   └── repository/           # API tìm kiếm/summary, SQLite bookmark và history
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

`AppContainer` cung cấp Volley RequestQueue dùng application context, Executor cho tác vụ blocking, ReaderSettings, SearchRepository, ArticleRepository, BookmarkRepository và HistoryRepository. SearchViewModel giữ query bằng SavedStateHandle; LibraryViewModel tải dữ liệu SQLite ngoài main thread. ArticleRepository hiện trả summary và metadata; chưa trả HTML toàn bài cho reader.

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

Mỗi phần hiện có giao diện mẫu hoặc điểm tích hợp để chủ feature phát triển tiếp. Khi nối API, dùng metadata của bài đã resolve cho nội dung, yêu thích, lịch sử, nguồn và chia sẻ. Không truyền HTML dài qua navigation; không giữ Activity hoặc View trong ViewModel. HTTP/database cần nằm trong lớp dữ liệu, công việc blocking chạy ngoài main thread.

Tạo nhánh ngắn theo phần việc, chẳng hạn `feature/search` hoặc `feature/article`. Trước pull request, chạy ba task build/lint/test ở trên và ghi rõ kiểm tra thiết bị đã thực hiện. Thống nhất thay đổi Gradle, manifest, navigation và dependency dùng chung với TV1; thay đổi schema dữ liệu với TV4.

Không commit SDK/JDK cục bộ, `local.properties`, output build, khóa ký, token hoặc khóa API. README này là tài liệu chung trên GitHub; kế hoạch, ghi chép và ảnh kiểm thử cá nhân chưa được đưa vào repository.

## Phần triển khai tiếp theo

- Nối API nội dung toàn bài với reader; bổ sung kiểm thử mất mạng/redirect trên nội dung thật.
- Thay reader mẫu bằng HTML/WebView; giữ nội dung và metadata cùng bài khi chuyển link, redirect và Back.
- Nối reader thật với `HistoryRepository.recordRead()` sau khi hiển thị thành công và với BookmarkRepository để thêm/bỏ yêu thích.
- Tích hợp nguồn/giấy phép theo metadata, cỡ chữ hiện có và các ca kiểm thử lifecycle trên reader thật.

Ứng dụng là đồ án học tập của nhóm, không phải ứng dụng chính thức của Wikimedia. Reader mẫu chưa tải toàn bài Wikipedia; nguồn và giấy phép nội dung cần được tích hợp đầy đủ khi nối reader thật.
