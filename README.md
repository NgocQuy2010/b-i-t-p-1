# BIT240200_FoodManager — Mobile Midterm (Food Management)

> Sinh viên: Nguyễn Ngọc Quý — MSSV **BIT240200**. Android Native Kotlin, Android Studio.

## 1. Phù hợp đề kiểm tra

- 02 model `DishCategory` và `Dish`; Room tables **`BIT240200_DishCategory`**, **`BIT240200_Dish`**. Tất cả **tên cột Room** đều bắt đầu `BIT240200_`.
- 06 món và 03 loại mẫu (tự tạo một lần khi chạy lần đầu).
- Danh sách, chi tiết, thêm, sửa, xóa có xác nhận; chọn loại từ dữ liệu thật.
- Tìm kiếm theo một phần tên; bộ lọc **loại, đang bán, khoảng giá** có thể kết hợp. Sắp theo tên, giá tăng, giá giảm, thời gian tăng. Bộ lọc được lưu qua SharedPreferences và khôi phục khi quay lại.
- Mỗi món hỗ trợ nhiều URL ảnh; chọn đúng **một ảnh thumbnail**, hoặc không có ảnh; ảnh xem qua Glide. Loại thời gian <=15 phút: Quick / Nhanh; >15 phút: Regular / Thông thường.
- Validation: tên bắt buộc, giá >0, phút >0, loại tồn tại, không trùng tên (không phân biệt hoa/thường) trong cùng loại; URL ảnh phải là HTTP(S); chặn xóa loại đang có món, kể cả khi offline.
- Giao diện mặc định **English**, nút chuyển **English / Tiếng Việt**; chuỗi giao diện và lỗi lấy từ `values/strings.xml` và `values-vi/strings.xml`. Dùng AppCompatDelegate để duy trì lựa chọn.
- **Offline-first**: Room lưu mọi chỉnh sửa tức thì, kể cả khi không có mạng. Khi có mạng, WorkManager (network constraint + NetworkCallback + periodic) tự đẩy pending CREATE/UPDATE/DELETE rồi lấy từ MockAPI; người dùng không cần bấm Sync.

## 2. Mở project Android Studio (Windows)

1. Giải nén `BIT240200_FoodManager.zip` vào `Downloads` hoặc một thư mục bất kỳ. KHÔNG mở ZIP trực tiếp.
2. **Lưu ý Gradle Wrapper:** Do bản đóng gói không chứa tệp JAR nhị phân, mở PowerShell trong thư mục project và chạy **một lần**:
   ```powershell
   powershell -ExecutionPolicy Bypass -File .\setup-wrapper.ps1
   ```
   Script tải `gradle-wrapper.jar` từ repository chính thức của Gradle. Máy cần Internet. Trường hợp không tải được, sao chép `gradle-wrapper.jar` vào `FoodManager/gradle/wrapper/`.
3. Mở **Android Studio > File > Open**, chọn thư mục `BIT240200_FoodManager` (thư mục chứa `settings.gradle.kts`), chọn Trust Project.
4. Sync Gradle; cần **Android SDK Platform 35**, **JDK 17**. Chọn emulator Pixel, rồi **Run > app**.
5. Chưa cấu hình MockAPI? Ứng dụng **vẫn chạy local đầy đủ với 3 loại và 6 món mẫu**. Không cần MockAPI để làm chức năng cơ bản.

## 3. Cấu hình MockAPI để đạt phần đồng bộ offline (1,5 điểm)

**Cần sử dụng project MockAPI mới, không dùng lại 2 resource `courses` và `categories` của bài trước.**

1. Đăng nhập https://mockapi.io và tạo project, API Prefix: `/api/v1`.
2. Tạo **2 resources**, tên chính xác (có phân biệt chữ hoa/chữ thường):
   - `BIT240200_DishCategory`
   - `BIT240200_Dish`
3. Trường dữ liệu (schema) của `BIT240200_DishCategory`:
   - `BIT240200_Id` (String): ID nghiệp vụ cố định do app phát sinh.
   - `BIT240200_Name` (String)
   - `BIT240200_Description` (String)
4. Trường dữ liệu (schema) của `BIT240200_Dish`:
   - `BIT240200_Id` (String)
   - `BIT240200_Name` (String)
   - `BIT240200_Price` (Number)
   - `BIT240200_PreparationTime` (Number)
   - `BIT240200_IsAvailable` (Boolean)
   - `BIT240200_Description` (String)
   - `BIT240200_DishCategoryId` (String)
   - `BIT240200_DishImage` (Array of Objects), các object chứa `BIT240200_ImageUrl` (String) và `BIT240200_IsThumbnail` (Boolean).
5. `id` (chữ thường không tiền tố) là **thuộc tính hệ thống do MockAPI sinh**, app chỉ giữ nó ở **cột Room `BIT240200_RemoteId`**. `BIT240200_Id` là mã nghiệp vụ và dùng để ghép quan hệ; mọi trường **do sinh viên đặt** đều có tiền tố.
6. Dùng `gradle.properties` sửa duy nhất dòng:
   ```properties
   mockApiBaseUrl=https://YOUR_REAL_PROJECT_ID.mockapi.io/api/v1/
   ```
   Phải có dấu `/` cuối URL. Android Studio > File > Sync Project with Gradle Files > Run lại app.
7. Tại lúc kết nối lần đầu, 3 loại và 6 món mẫu được đẩy lên server tự động. Kiểm tra bằng Chrome ở:
   - `https://YOUR_REAL_PROJECT_ID.mockapi.io/api/v1/BIT240200_DishCategory`
   - `https://YOUR_REAL_PROJECT_ID.mockapi.io/api/v1/BIT240200_Dish`
   Nếu API trả về lỗi 404, kiểm tra tên resource và prefix chính xác.

**Ví dụ JSON món được gửi lên API** (phía MockAPI bổ sung `id` riêng):

```json
{
  "BIT240200_Id": "a-predefined-client-uuid",
  "BIT240200_Name": "Fried rice",
  "BIT240200_Price": 45000,
  "BIT240200_PreparationTime": 10,
  "BIT240200_IsAvailable": true,
  "BIT240200_Description": "Fresh fried rice",
  "BIT240200_DishCategoryId": "BIT240200_category_1",
  "BIT240200_DishImage": [
    {"BIT240200_ImageUrl": "https://picsum.photos/seed/food/800/450", "BIT240200_IsThumbnail": true}
  ]
}
```

### Quan trọng về độ tin cậy đồng bộ

- Dữ liệu trong Room là **nguồn hiển thị duy nhất** và mọi thay đổi ở UI được ghi vào Room trước, ngay cả khi mất mạng.
- Chỉ một worker đồng bộ chạy tại một thời điểm (`Mutex`). App đẩy thay đổi local lên server trước khi pull dữ liệu. Các thay đổi chưa gửi không bị dữ liệu server ghi đè.
- Mỗi bản ghi có ID ổn định `BIT240200_Id`: nếu POST đã thành công nhưng phản hồi bị mất, worker kiểm tra danh sách server để tìm ID trước khi POST lại. Điều này **giảm nguy cơ bản ghi trùng**, không phải bảo đảm tuyệt đối exactly-once khi có nhiều thiết bị/thao tác đồng thời; MockAPI không hỗ trợ transaction hoặc unique constraint.
- Không xử lý đồng bộ hai chiều với chính sách giải quyết xung đột phức tạp; mục tiêu là demo **một máy + một MockAPI** theo đề. Để dùng production cần backend idempotency key, transaction, xử lý version/concurrency và auth.

## 4. Các file code cần thuyết minh (Video 2)

| File | Ý nghĩa |
|---|---|
| `data/Models.kt` | 2 Room entities, DAO, tên bảng và cột có tiền tố MSSV, danh sách ảnh JSON |
| `data/DishValidator.kt` | Quy tắc kiểm tra tên, giá, phút, danh mục, trùng tên |
| `data/Remote.kt` | Retrofit GET/POST/PUT/DELETE và `@SerializedName` có tiền tố MSSV |
| `data/FoodRepository.kt` | Dữ liệu Room, seed, CRUD, kiểm tra xóa danh mục, xử lý đồng bộ |
| `FoodApplication.kt` | WorkManager, NetworkCallback, tiếng Anh mặc định |
| `ui/DishListActivity.kt` | RecyclerView, filter kết hợp, sắp xếp và lưu bộ lọc |
| `ui/DishFormActivity.kt` | Form thêm/sửa, nhiều ảnh, chọn thumbnail, validation |
| `ui/DishDetailActivity.kt` | Nhiều ảnh, trạng thái Quick/Regular, xóa có xác nhận |
| `ui/CategoryActivity.kt` | Thêm, sửa, xóa loại, không xóa khi đang sử dụng |
| `res/values/strings.xml` | Chuỗi English |
| `res/values-vi/strings.xml` | Chuỗi Tiếng Việt |

Luồng màn hình → Repository → Room; Worker (khi mạng có lại) → Repository.sync → Retrofit → MockAPI.

## 5. Checklist test / quay Video 1 (2 điểm)

1. Mở app, hiện `BIT240200` trên tiêu đề; 3 loại + 6 món.
2. Tìm chữ `rice`, lọc loại `Main dishes`, lọc chỉ đang bán, nhập giá 30000–80000; sắp giá giảm và kiểm tra điều kiện đồng thời.
3. Chạm món, xem nhiều ảnh, Quick/Regular theo thời gian.
4. Thêm món `TEST Soup` (tên không trùng), thêm 2 URL ảnh, chọn ảnh đại diện, lưu; thử nhập giá -1000 và thời gian 0 để thấy validation.
5. Sửa giá, đổi ảnh đại diện, tắt trạng thái đang bán; kiểm tra món vẫn trong danh sách nhưng biến mất khi bật `Available only`.
6. Thử xóa một loại đang có món → bị chặn. Xóa món TEST có xác nhận; tạo/xóa loại thử nghiệm.
7. Bấm English/Tiếng Việt, mở chi tiết/form và trở lại; ngôn ngữ giữ nguyên.
8. **Offline demo nâng cao:** có MockAPI cấu hình trước, tắt Internet trong emulator → thêm món `OFFLINE TEST` → bấm Lưu, thấy món trên app → bật Internet → **không nhấn đồng bộ** → đợi Worker vài giây và tải lại URL `.../BIT240200_Dish` trên Chrome để thấy bản ghi mới. Kết quả phụ thuộc kết nối MockAPI thực tế; cần kiểm tra trước khi quay.
9. Quay màn hình và nộp Video 1 cùng source Git trong ca học.

## 6. Video 2 (2 điểm)

- Hiện tên project và `BIT240200` trong app/build.gradle.kts + model.
- Giải thích cách lưu Room, tên bảng **`BIT240200_Dish`**, **`BIT240200_DishCategory`**, ví dụ cột **`BIT240200_Name`** và **`BIT240200_DishImage`**.
- Giải thích liên kết loại ↔ món qua `BIT240200_DishCategoryId`; thumbnail của món trong `BIT240200_DishImage`.
- Giải thích filter, validation, đa ngôn ngữ, UI → Room → WorkManager → Retrofit → MockAPI.
- Chỉ ra network callback + work constraint, cách xử lý pending và client UUID khi offline.
- **Hạn Video 2: 23/10/2026 theo đề**; nộp LMS. Video 1 và source nộp ngay trong ca học kiểm tra.

## 7. Lệnh kiểm tra

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

APK nếu build thành công: `app/build/outputs/apk/debug/app-debug.apk`.

**Tình trạng:** Đã kiểm tra cú pháp XML, số lượng resource strings, và compile bộ kiểm tra dữ liệu thuần Kotlin. Chưa có Android SDK/Gradle ở môi trường đóng gói nên **chưa build APK hoặc kiểm thử trên emulator thực tế**. Cần Sync và Run trong Android Studio máy bạn trước khi quay/nộp.
