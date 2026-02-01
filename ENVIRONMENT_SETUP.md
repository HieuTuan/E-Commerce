# Environment Variables Setup Guide

## 🔑 Required Environment Variables

Để sử dụng Cloudinary integration, bạn cần set các environment variables sau:

### Windows (Command Prompt)
```cmd
set CLOUDINARY_CLOUD_NAME=your-cloud-name
set CLOUDINARY_API_KEY=your-api-key
set CLOUDINARY_API_SECRET=your-api-secret
```

### Windows (PowerShell)
```powershell
$env:CLOUDINARY_CLOUD_NAME="your-cloud-name"
$env:CLOUDINARY_API_KEY="your-api-key"
$env:CLOUDINARY_API_SECRET="your-api-secret"
```

### Linux/Mac (Bash)
```bash
export CLOUDINARY_CLOUD_NAME=your-cloud-name
export CLOUDINARY_API_KEY=your-api-key
export CLOUDINARY_API_SECRET=your-api-secret
```

## 📋 Cách lấy Cloudinary Credentials

1. **Truy cập**: https://console.cloudinary.com/
2. **Đăng nhập** hoặc tạo account mới
3. **Dashboard** → Copy thông tin:
   ```
   Cloud Name: your-cloud-name
   API Key: 123456789012345
   API Secret: AbCdEfGhIjKlMnOpQrStUvWxYz
   ```

## 🚀 Test Environment Variables

### Kiểm tra variables đã set chưa:

**Windows:**
```cmd
echo %CLOUDINARY_CLOUD_NAME%
echo %CLOUDINARY_API_KEY%
echo %CLOUDINARY_API_SECRET%
```

**Linux/Mac:**
```bash
echo $CLOUDINARY_CLOUD_NAME
echo $CLOUDINARY_API_KEY
echo $CLOUDINARY_API_SECRET
```

## 🔧 Alternative: .env File (Development)

Tạo file `.env` trong root directory:
```
CLOUDINARY_CLOUD_NAME=your-cloud-name
CLOUDINARY_API_KEY=your-api-key
CLOUDINARY_API_SECRET=your-api-secret
```

## ⚠️ Default Values (Fallback)

Nếu không set environment variables, application sẽ sử dụng default values:
```properties
cloudinary.cloud-name=demo
cloudinary.api-key=123456789012345
cloudinary.api-secret=your-api-secret-here
```

**Lưu ý**: Default values chỉ để application khởi động được, không thể upload thực tế.

## 🎯 Production Setup

### Server Environment Variables
```bash
# Add to /etc/environment or ~/.bashrc
export CLOUDINARY_CLOUD_NAME=production-cloud-name
export CLOUDINARY_API_KEY=production-api-key
export CLOUDINARY_API_SECRET=production-api-secret
```

### Docker Environment
```dockerfile
ENV CLOUDINARY_CLOUD_NAME=your-cloud-name
ENV CLOUDINARY_API_KEY=your-api-key
ENV CLOUDINARY_API_SECRET=your-api-secret
```

### Kubernetes ConfigMap/Secret
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: cloudinary-credentials
data:
  CLOUDINARY_CLOUD_NAME: <base64-encoded-value>
  CLOUDINARY_API_KEY: <base64-encoded-value>
  CLOUDINARY_API_SECRET: <base64-encoded-value>
```

## 🔍 Troubleshooting

### Lỗi: "Could not resolve placeholder"
```
PlaceholderResolutionException: Could not resolve placeholder 'CLOUDINARY_API_KEY'
```

**Giải pháp:**
1. Kiểm tra environment variables đã set chưa
2. Restart terminal/IDE sau khi set variables
3. Verify variables với echo command

### Lỗi: "Cloudinary authentication failed"
```
ERROR - Cloudinary authentication failed
```

**Giải pháp:**
1. Kiểm tra credentials có đúng không
2. Verify trên Cloudinary Console
3. Check network connection

## ✅ Verification Steps

1. **Set environment variables**
2. **Restart terminal/IDE**
3. **Run application**: `mvnw.cmd spring-boot:run`
4. **Check logs**: Should see "Initializing Cloudinary with cloud name: your-cloud-name"
5. **Test upload**: Create return request với video

## 🎉 Success Indicators

Khi setup đúng, bạn sẽ thấy logs:
```
INFO - Initializing Cloudinary with cloud name: your-cloud-name
INFO - Configuring Cloudinary file storage service
INFO - Video uploaded successfully to Cloudinary: https://res.cloudinary.com/...
```