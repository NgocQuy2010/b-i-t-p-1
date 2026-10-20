# Video 1 — demo app (3–5 phút)

1. (0:00–0:30) "Em là sinh viên Nguyễn Ngọc Quý - MSSV BIT240200. Đây là ứng dụng quản lý món ăn trên Android Studio, có 3 loại và 6 món dữ liệu mẫu."
2. (0:30–1:20) "Em tìm theo tên, lọc theo loại/đang bán/khoảng giá đồng thời và sắp theo giá hoặc thời gian. Bộ lọc vẫn được lưu khi quay lại."
3. (1:20–2:00) "Trong chi tiết món có nhiều ảnh. Em chọn ảnh đại diện từ form; món chế biến không quá 15 phút hiện Quick, còn lại Regular."
4. (2:00–3:10) "Em thêm món mới, kiểm tra giá âm bị chặn, sửa tên/giá/ảnh, xóa với xác nhận. Danh mục đang có món không thể xóa."
5. (3:10–3:40) "Ứng dụng mặc định tiếng Anh và có thể chuyển Tiếng Việt, lựa chọn giữ khi đổi màn hình."
6. (3:40–4:30, nếu có MockAPI) "Khi tắt mạng, món vẫn lưu trong Room. Khi bật mạng, WorkManager đồng bộ lên MockAPI không cần bấm Sync; đây là JSON trả về trên Chrome."

# Video 2 — giải thích code (3–5 phút)

1. Model: tên project `BIT240200_FoodManager`; Room dùng bảng `BIT240200_DishCategory`/`BIT240200_Dish`, mọi cột bắt đầu `BIT240200_`.
2. Mối quan hệ: DishCategoryId liên kết loại-món, DishImage là JSON list của ImageUrl/IsThumbnail, duy nhất một ảnh đại diện do form kiểm soát.
3. `DishValidator`: Name, Price > 0, PreparationTime > 0, Id hợp lệ, không trùng tên cùng loại.
4. `DishListActivity`: RecyclerView, lọc đồng thời, sắp xếp, SharedPreferences giữ điều kiện.
5. `FoodRepository`: UI thao tác với Room offline; các mục có pending CREATE/UPDATE/DELETE.
6. `FoodApplication` + WorkManager: quan sát mạng, job cần mạng; gọi `sync()`; `Remote.kt` dùng Retrofit với `@SerializedName("BIT240200_...")` rồi lấy dữ liệu JSON về.
7. `values/strings.xml` và `values-vi/strings.xml` chuyển English ↔ Tiếng Việt.
8. Kết luận và giới hạn: MockAPI là backend giả lập, đồng bộ idempotent theo UUID ở mức demo, không phải hệ thống đa người dùng production.
