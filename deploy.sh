#!/bin/bash

# Dừng script nếu có lỗi xảy ra
set -e

echo "=================================================="
echo "Bắt đầu tiến trình cập nhật và deploy Stirling-PDF"
echo "Thời gian: $(date)"
echo "=================================================="

# 1. Di chuyển vào thư mục project
cd /home/haison/Stirling-PDF

# 2. Cập nhật code (Bỏ qua vì bạn đang code trực tiếp trên server)
# Nếu bạn code trên máy khác và push lên GitHub, hãy bỏ comment dòng dưới đây:
# git pull origin feature/custom-development

# 3. Build project (bao gồm backend + frontend)
echo "=> Đang build ứng dụng (bỏ qua tests)..."
# Đảm bảo đường dẫn tới Java 21 hoặc mới hơn (tùy thuộc vào Gradle)
export PATH=$PATH:~/.local/bin
./gradlew clean build -PbuildWithFrontend=true -x test

# 4. Khởi động lại service bằng systemd
echo "=> Đang khởi động lại Stirling-PDF service..."
sudo systemctl restart stirling-pdf

echo "=================================================="
echo "Deploy thành công!"
echo "Kiểm tra trạng thái service: sudo systemctl status stirling-pdf"
echo "=================================================="
