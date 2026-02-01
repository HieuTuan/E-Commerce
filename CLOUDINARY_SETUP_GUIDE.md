# Hướng dẫn Setup Cloudinary cho Video Storage

## 🎯 Tổng quan

Đã chuyển từ **local file storage** sang **Cloudinary** để lưu trữ video evidence của return requests. Cloudinary cung cấp:

- ✅ **Video optimization** tự động
- ✅ **Multiple format support** (MP4, WebM, OGG)
- ✅ **CDN delivery** toàn cầu
- ✅ **Automatic transcoding**
- ✅ **Secure URLs**
- ✅ **Free tier** 25GB storage + 25GB bandwidth/tháng

## 📋 Bước 1: Tạo Cloudinary Account

1. **Truy cập**: https://console.cloudinary.com/
2. **Sign up** với email hoặc Google account
3. **Verify email** và complete profile
4. **Chọn plan**: Free tier (đủ cho development và small production)

## 🔑 Bước 2: Lấy API Credentials

Sau khi đăng nhập vào Cloudinary Console:

1. **Dashboard** → **Account Details**
2. Copy các thông tin sau:
   ```
   Cloud Name: your-cloud-name
   API Key: 123456789012345
   API Secret: your-api-secret-here
   ```

## ⚙️ Bước 3: Cấu hình Environment Variables

### Development (Local)
Tạo file `.env` hoặc set environment variables:

```bash
# Cloudinary Configuration
export CLOUDINARY_CLOUD_NAME=your-cloud-name
export CLOUDINARY_API_KEY=123456789012345
export CLOUDINARY_API_SECRET=your-api-secret-here
```

### Production
Set environment variables trên server:

```bash
# Production Environment Variables
CLOUDINARY_CLOUD_NAME=your-cloud-name
CLOUDINARY_API_KEY=123456789012345
CLOUDINARY_API_SECRET=your-api-secret-here
```

## 🚀 Bước 4: Khởi động Application

### Development Mode (Cloudinary)
```bash
# Set environment variables
export CLOUDINARY_CLOUD_NAME=your-cloud-name
export CLOUDINARY_API_KEY=your-api-key
export CLOUDINARY_API_SECRET=your-api-secret

# Start application
mvnw.cmd spring-boot:run
```

### Production Mode (Cloudinary)
```bash
# Start with production profile
java -jar app.jar --spring.profiles.active=prod
```

### Development Mode (Local - fallback)
```bash
# Để sử dụng local storage thay vì Cloudinary
# Chỉnh app.file-storage.type=local trong application.properties
mvnw.cmd spring-boot:run
```

## 🎬 Bước 5: Test Video Upload

1. **Tạo return request** với video evidence
2. **Check logs** để xem upload process:
   ```
   INFO  - Uploading video to Cloudinary: video.mp4 (size: 5242880 bytes)
   INFO  - Video uploaded successfully to Cloudinary: https://res.cloudinary.com/...
   ```
3. **Verify video** hiển thị trong staff dashboard
4. **Check Cloudinary Console** → Media Library để xem uploaded videos

## 📁 Cấu trúc Folder trong Cloudinary

Videos sẽ được organize theo structure:

```
cloudinary-root/
├── return-evidence/
│   ├── order-123/
│   │   └── uuid-video.mp4
│   ├── order-456/
│   │   └── uuid-video.mp4
│   └── ...
└── other-uploads/
    └── ...
```

## 🔧 Tính năng Cloudinary Integration

### 1. Automatic Video Optimization
- **Quality**: Auto-optimized cho web playback
- **Format**: Tự động convert sang MP4 nếu cần
- **Compression**: Giảm file size mà không mất chất lượng

### 2. Multiple Format Support
- **Upload**: Hỗ trợ MP4, AVI, MOV, WebM, OGG
- **Delivery**: Tự động serve format tốt nhất cho browser
- **Fallback**: Multiple sources trong video player

### 3. CDN Delivery
- **Global CDN**: Fast delivery worldwide
- **Caching**: Automatic edge caching
- **Bandwidth**: Optimized bandwidth usage

### 4. Security Features
- **Secure URLs**: HTTPS by default
- **Access Control**: Private/public resource control
- **Signed URLs**: Time-limited access (if needed)

## 📊 Monitoring & Analytics

### Cloudinary Console
- **Usage Statistics**: Storage, bandwidth, transformations
- **Media Library**: Browse uploaded videos
- **Analytics**: Performance metrics

### Application Logs
```bash
# Success logs
INFO  - Video uploaded successfully to Cloudinary: https://res.cloudinary.com/...

# Error logs  
ERROR - Failed to upload video to Cloudinary: Connection timeout
```

## 🛠️ Troubleshooting

### Common Issues

1. **Invalid Credentials**
   ```
   ERROR - Cloudinary authentication failed
   ```
   **Solution**: Check CLOUDINARY_* environment variables

2. **Upload Timeout**
   ```
   ERROR - Failed to upload video to Cloudinary: Connection timeout
   ```
   **Solution**: Check network connection, file size limits

3. **File Size Too Large**
   ```
   ERROR - Video file size cannot exceed 50MB
   ```
   **Solution**: Compress video or increase limit

4. **Invalid File Format**
   ```
   ERROR - File must be a video format
   ```
   **Solution**: Ensure file has video/* MIME type

### Debug Mode
Enable debug logging:
```properties
logging.level.com.mypkga.commerceplatformfull.service.impl.CloudinaryFileServiceImpl=DEBUG
logging.level.com.cloudinary=DEBUG
```

## 💰 Cost Optimization

### Free Tier Limits
- **Storage**: 25GB
- **Bandwidth**: 25GB/month
- **Transformations**: 25,000/month

### Best Practices
1. **Compress videos** before upload
2. **Delete old videos** when not needed
3. **Monitor usage** in Cloudinary Console
4. **Use transformations** wisely

## 🔄 Migration từ Local Storage

Nếu đã có videos trong local storage:

1. **Backup existing videos**
2. **Update configuration** to Cloudinary
3. **Re-upload videos** (hoặc migrate programmatically)
4. **Update database URLs** to Cloudinary URLs

## 🎉 Kết luận

Cloudinary integration đã được setup hoàn chỉnh:

- ✅ **Configuration**: CloudinaryConfig, CloudinaryFileServiceImpl
- ✅ **Environment Variables**: CLOUDINARY_* variables
- ✅ **Video Upload**: Automatic optimization và CDN delivery
- ✅ **Error Handling**: Comprehensive error handling
- ✅ **Monitoring**: Detailed logging và analytics

**Hệ thống giờ đây sử dụng Cloudinary để lưu trữ và deliver video evidence một cách hiệu quả!** 🚀