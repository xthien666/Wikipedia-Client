# WikiRead — Wikipedia Client

Ứng dụng Android phục vụ đồ án nhóm 5 thành viên, hướng tới tìm kiếm, đọc và quản lý bài viết Wikipedia tiếng Việt. Mã nguồn hiện tại là bản **prototype 0.1.0**: có nền tảng ứng dụng, giao diện và các tương tác mẫu để nhóm tích hợp API và lưu trữ ở những bước tiếp theo.

## Trạng thái chức năng

| Chức năng | Hiện trạng |
|---|---|
| Điều hướng | Bốn tab Khám phá, Yêu thích, Lịch sử, Cài đặt; mở màn hình bài mẫu và quay lại |
| Ô tìm kiếm | Nhập/xóa từ khóa, chọn chủ đề và giữ query khi tạo lại Activity; chưa gọi API tìm kiếm |
| Đọc bài | Hiển thị tên bài được chọn và lời hướng dẫn cục bộ; chưa tải nội dung Wikipedia |
| Liên kết bài | Mở trang tương ứng bằng trình duyệt; chia sẻ liên kết bằng Android Intent |
| Yêu thích | Danh sách minh họa, lọc thể loại và xóa từng thẻ; chưa lưu yêu thích thật |
| Lịch sử | Danh sách minh họa, xóa từng bài hoặc xóa tất cả có xác nhận; chưa ghi lịch sử đọc thật |
| Cỡ chữ | Bốn mức, lưu bằng SharedPreferences và áp dụng cho preview lẫn reader mẫu |
| Loading, rỗng, lỗi | Có StatusView dùng chung; trạng thái lỗi mạng/thử lại chưa được tích hợp với API |

Thao tác xóa/lọc danh sách mẫu được giữ qua việc tạo lại màn hình và Activity bằng SavedStateHandle, không cam kết còn sau force-stop. Cỡ chữ là thiết lập được lưu bền vững. Nghe tóm tắt, lưu bài và xóa cache đang bị vô hiệu hóa vì chưa triển khai. Bản hiện tại chưa hỗ trợ đọc ngoại tuyến.

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
| Lưu thiết lập | SharedPreferences |
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

Script ưu tiên JDK/SDK portable trong `.tools` nếu có, còn khi clone mới cần tự cấu hình `JAVA_HOME` và SDK. Script không cung cấp sẵn JDK hoặc thư mục Android SDK.

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

## Kiểm thử và CI

Mốc mã `4faa319` đã build local thành công, Android Lint không có lỗi chặn và **11/11 unit test đạt**. Lint vẫn có cảnh báo; đây chưa phải kết luận nghiệm thu toàn bộ sản phẩm.

Bộ [MainActivityTest](app/src/test/java/com/example/wikipediaclient/MainActivityTest.java) kiểm tra điều hướng/Back, giữ query và saved state, tính nhất quán của bài mẫu, nội dung Intent chia sẻ, lọc/xóa danh sách mẫu, lưu cỡ chữ, icon thanh hệ thống và việc không áp dụng khoảng đệm bàn phím hai lần.

Robolectric chạy API 28. Giao diện đã được kiểm tra riêng trên emulator API 36 ở các luồng mẫu, cỡ chữ lớn, xoay ngang, bàn phím và chế độ tối của hệ thống. Những kiểm tra này chưa bao phủ thiết bị thật, TalkBack, tablet, process death thật hoặc API Wikipedia.

[Workflow Android build](.github/workflows/android.yml) chạy khi push lên `main` hoặc mở pull request, sử dụng JDK 17 và ba task `assembleDebug`, `lintDebug`, `testDebugUnitTest`. Xem kết quả từng lần chạy tại [GitHub Actions](https://github.com/xthien666/Wikipedia-Client/actions).

## Cấu trúc mã nguồn

```text
app/src/main/
├── java/com/example/wikipediaclient/
│   ├── MainActivity.java          # Khung ứng dụng, navigation và insets
│   ├── WikiApplication.java       # Khởi tạo AppContainer
│   ├── data/
│   │   ├── AppContainer.java      # Dependency dùng chung
│   │   ├── model/                # ArticleLocator
│   │   ├── local/                # ReaderSettings; chờ dữ liệu cá nhân
│   │   ├── remote/               # Package chuẩn bị cho API
│   │   └── repository/           # Package chuẩn bị cho repository
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

`AppContainer` cung cấp Volley RequestQueue dùng application context, Executor cho tác vụ blocking và ReaderSettings. SearchViewModel giữ query; SampleListViewModel giữ bộ lọc và các ID mẫu đã xóa. Các package remote/repository hiện chưa có implementation API hoặc database.

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
- Thay danh sách mẫu bằng repository/database yêu thích và lịch sử, chống trùng và giữ dữ liệu sau khởi động lại.
- Tích hợp nguồn/giấy phép theo metadata, cỡ chữ hiện có và các ca kiểm thử lifecycle trên reader thật.

Ứng dụng là đồ án học tập của nhóm, không phải ứng dụng chính thức của Wikimedia. Bản mẫu chưa tải nội dung Wikipedia; giấy phép nội dung cần được xác định và hiển thị đúng khi tích hợp dữ liệu thật.
