# Design Document

## Overview

This design addresses the video playback issue by improving the HTML5 video element implementation with multiple format support, better error handling, and fallback options. The solution focuses on making evidence videos accessible to staff members while handling various edge cases that prevent video playback.

## Architecture

The fix involves enhancing the video display component within the existing template structure:

```
Return Request Data -> Template Processing -> Enhanced Video Component
                                                    |
                                                    v
                                            HTML5 Video Element
                                                    |
                                                    v
                                        Multiple Format Sources + Error Handling
```

No backend changes are required - only template improvements for better video handling.

## Components and Interfaces

### Affected Components

1. **Template File**: `src/main/resources/templates/staff/returns/detail.html`
   - **Current Video Implementation**:
     ```html
     <video controls class="w-100" style="max-height: 300px;">
         <source th:src="${returnRequest.evidenceVideoUrl}" type="video/mp4">
         Trình duyệt không hỗ trợ video.
     </video>
     ```
   
   - **Enhanced Video Implementation**:
     ```html
     <video controls class="w-100" style="max-height: 300px;" preload="metadata">
         <source th:src="${returnRequest.evidenceVideoUrl}" type="video/mp4">
         <source th:src="${returnRequest.evidenceVideoUrl}" type="video/webm">
         <source th:src="${returnRequest.evidenceVideoUrl}" type="video/ogg">
         <p>Trình duyệt của bạn không hỗ trợ phát video. 
            <a th:href="${returnRequest.evidenceVideoUrl}" target="_blank">Tải video về</a>
         </p>
     </video>
     ```

2. **Error Handling Enhancement**: Add JavaScript for video error detection and user feedback

### Interface Contracts

- **Video Display Contract**: Video element must handle multiple formats and provide fallbacks
- **Error Handling Contract**: Failed video loads must show meaningful error messages
- **Accessibility Contract**: Provide alternative access methods when video fails
- **Performance Contract**: Videos should load efficiently with appropriate preload settings

## Data Models

### Video URL Structure
```
returnRequest.evidenceVideoUrl: String (URL to video file)
- Expected formats: MP4, WebM, OGV
- Expected location: File storage service or CDN
- Expected accessibility: Public or authenticated access
```

### Enhanced Video Template Structure
```html
<!-- Enhanced video with multiple format support -->
<video controls class="w-100" style="max-height: 300px;" 
       preload="metadata" 
       onloadstart="handleVideoLoadStart(this)"
       onerror="handleVideoError(this)"
       oncanplay="handleVideoCanPlay(this)">
    
    <!-- Multiple source formats for better compatibility -->
    <source th:src="${returnRequest.evidenceVideoUrl}" type="video/mp4">
    <source th:src="${returnRequest.evidenceVideoUrl}" type="video/webm">
    <source th:src="${returnRequest.evidenceVideoUrl}" type="video/ogg">
    
    <!-- Fallback content for unsupported browsers -->
    <div class="alert alert-warning">
        <i class="fas fa-exclamation-triangle me-2"></i>
        Trình duyệt của bạn không hỗ trợ phát video HTML5.
        <br>
        <a th:href="${returnRequest.evidenceVideoUrl}" target="_blank" class="btn btn-sm btn-outline-primary mt-2">
            <i class="fas fa-download me-2"></i>Tải video về để xem
        </a>
    </div>
</video>

<!-- Error display area -->
<div id="video-error" class="alert alert-danger mt-2" style="display: none;">
    <i class="fas fa-exclamation-circle me-2"></i>
    <span id="video-error-message">Không thể tải video</span>
    <br>
    <a th:href="${returnRequest.evidenceVideoUrl}" target="_blank" class="btn btn-sm btn-outline-light mt-2">
        <i class="fas fa-external-link-alt me-2"></i>Mở video trong tab mới
    </a>
</div>

<!-- Loading indicator -->
<div id="video-loading" class="text-center mt-2" style="display: none;">
    <div class="spinner-border spinner-border-sm me-2" role="status"></div>
    Đang tải video...
</div>
```
## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system-essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

Based on the prework analysis, after eliminating redundancy, the following properties can be tested:

**Property 1: Video element rendering consistency**
*For any* return request with evidence video URL, when the template is rendered, the video element should be present with proper controls and multiple source formats
**Validates: Requirements 1.1, 1.2, 3.1, 3.2**

**Property 2: Error handling and fallback behavior**
*For any* invalid or inaccessible video URL, when the video fails to load, appropriate error messages and fallback options should be displayed
**Validates: Requirements 2.1, 2.2, 2.4, 4.2, 4.3**

**Property 3: Page stability during video errors**
*For any* video loading error or playback failure, when the error occurs, the overall page functionality should remain intact without JavaScript errors
**Validates: Requirements 4.5**

## Error Handling

### Current Error Scenarios
1. **Video Not Loading**: Video element shows but content doesn't load
   - Root Cause: Invalid URL, unsupported format, or network issues
   - Impact: Staff cannot view evidence videos

2. **Format Compatibility**: Browser doesn't support the video format
   - Root Cause: Only MP4 source provided, browser may not support MP4
   - Impact: Video appears broken in some browsers

3. **Network Issues**: Video fails to load due to connectivity problems
   - Root Cause: Network timeouts, server unavailability
   - Impact: No feedback to user about the issue

### Improved Error Handling
- **Multiple Format Support**: Provide MP4, WebM, and OGV sources for better compatibility
- **Error Detection**: JavaScript event handlers for video loading errors
- **User Feedback**: Clear error messages and alternative access options
- **Graceful Degradation**: Download links when video playback fails
- **Loading Indicators**: Visual feedback during video loading

## Testing Strategy

### Unit Testing Approach
- Test template rendering with various video URL scenarios (valid, invalid, null)
- Verify multiple source elements are generated correctly
- Test error message display for different failure scenarios
- Validate fallback content and download links are present

### Property-Based Testing Approach
- Use **Thymeleaf testing framework** for template testing
- Generate random ReturnRequest entities with various evidenceVideoUrl values
- Property tests should run a minimum of 100 iterations
- Each property-based test will be tagged with comments referencing the design document properties

**Property-based testing requirements**:
- Library: Spring Boot Test with Thymeleaf testing support
- Minimum iterations: 100 per property test
- Test tagging format: `**Feature: video-playback-fix, Property {number}: {property_text}**`

### Integration Testing
- Test complete video display with real video files
- Verify error handling with intentionally broken video URLs
- Test cross-browser compatibility for video formats
- Confirm download links work correctly

### Manual Testing
- Test video playback with actual evidence videos
- Verify error messages appear for broken videos
- Test fallback download functionality
- Confirm video controls work properly (play, pause, volume, fullscreen)