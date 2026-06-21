# ============================================================
# GIAI ĐOẠN 1: BUILD - dùng JDK 17 để biên dịch ra file .jar
# ============================================================
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Copy toàn bộ mã nguồn backend vào container
COPY . .

# Sửa ký tự xuống dòng kiểu Windows (CRLF) trong gradlew để chạy được trên Linux,
# cấp quyền thực thi, rồi build ra file jar (bỏ qua test cho nhanh)
RUN sed -i 's/\r$//' gradlew \
    && chmod +x gradlew \
    && ./gradlew bootJar -x test --no-daemon \
    && cp build/libs/*-SNAPSHOT.jar app.jar

# ============================================================
# GIAI ĐOẠN 2: RUN - chỉ dùng JRE 17 (nhẹ hơn) để chạy app
# ============================================================
FROM eclipse-temurin:17-jre
WORKDIR /app

# Tạo thư mục chứa file upload (CV, ảnh) - sẽ được gắn ổ đĩa bền ở docker-compose
RUN mkdir -p /app/uploads

# Lấy file jar đã build từ giai đoạn 1
COPY --from=build /app/app.jar app.jar

EXPOSE 8080

# Lệnh khởi động ứng dụng
ENTRYPOINT ["java", "-jar", "app.jar"]
